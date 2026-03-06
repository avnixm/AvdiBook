package com.avnixm.avdibook.playback

import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.avnixm.avdibook.AvdiBookApplication
import com.avnixm.avdibook.data.db.entity.PlaybackStateEntity
import com.avnixm.avdibook.data.repository.BookDetailsRepository
import com.avnixm.avdibook.data.repository.PlaybackRepository
import com.avnixm.avdibook.playback.policy.AutoRewindPolicy
import com.avnixm.avdibook.playback.policy.SleepTimerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackService : MediaSessionService() {
    // All coroutines run on Main so ExoPlayer can be accessed safely.
    // Database calls switch to IO via withContext inside each function.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private var periodicSaveJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var currentBookId: Long? = null
    private var lastPausedAtMs: Long? = null
    private var isFadingOut = false

    @Volatile
    private var sleepTimerMode: SleepTimerMode = SleepTimerMode.OFF

    @Volatile
    private var sleepTimerEndTimestampMs: Long? = null

    private val playbackRepository: PlaybackRepository by lazy {
        val app = application as AvdiBookApplication
        app.appContainer.playbackRepository
    }

    private val bookDetailsRepository: BookDetailsRepository by lazy {
        val app = application as AvdiBookApplication
        app.appContainer.bookDetailsRepository
    }

    private val bridgeController = object : PlaybackServiceBridge.Controller {
        override fun setSleepTimerDuration(minutes: Int) {
            if (minutes <= 0) {
                this@PlaybackService.clearSleepTimer()
                return
            }
            sleepTimerMode = SleepTimerMode.DURATION
            sleepTimerEndTimestampMs = System.currentTimeMillis() + (minutes * 60_000L)
        }

        override fun setSleepTimerEndOfTrack() {
            sleepTimerMode = SleepTimerMode.END_OF_TRACK
            sleepTimerEndTimestampMs = null
        }

        override fun clearSleepTimer() {
            this@PlaybackService.clearSleepTimer()
        }

        override fun getSleepTimerState(): SleepTimerState {
            return currentSleepTimerState()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                serviceScope.launch {
                    maybeApplyAutoRewind()
                    currentBookId?.let { applyBookSettings(it) }
                }
                return
            }

            if (player.playbackState != Player.STATE_ENDED) {
                lastPausedAtMs = SystemClock.elapsedRealtime()
            }
            persistCurrentProgress(resetToStart = false)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> persistCurrentTrackDuration()

                Player.STATE_ENDED -> {
                    persistCurrentProgress(resetToStart = true)
                    if (sleepTimerMode == SleepTimerMode.END_OF_TRACK) {
                        triggerSleepPause()
                    }
                }

                Player.STATE_IDLE -> persistCurrentProgress(resetToStart = false)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentBookId = mediaItem?.mediaMetadata?.extras.bookIdOrNull()
            currentBookId?.let { bookId ->
                serviceScope.launch {
                    applyBookSettings(bookId)
                }
            }

            if (
                SleepTimerEngine.shouldPauseOnTrackBoundary(
                    mode = sleepTimerMode,
                    transitionReason = reason
                )
            ) {
                triggerSleepPause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build().apply {
            addListener(playerListener)
        }

        mediaSession = MediaSession.Builder(this, player).build()

        PlaybackServiceBridge.controller = bridgeController

        periodicSaveJob = serviceScope.launch {
            while (isActive) {
                delay(10_000)
                if (player.isPlaying) {
                    persistCurrentProgress(resetToStart = false)
                }
            }
        }

        sleepTimerJob = serviceScope.launch {
            while (isActive) {
                delay(1_000)
                if (sleepTimerMode == SleepTimerMode.DURATION) {
                    val endTs = sleepTimerEndTimestampMs ?: continue
                    if (SleepTimerEngine.shouldTriggerDuration(System.currentTimeMillis(), endTs)) {
                        triggerSleepPause()
                    }
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private fun clearSleepTimer() {
        sleepTimerMode = SleepTimerMode.OFF
        sleepTimerEndTimestampMs = null
    }

    private fun currentSleepTimerState(): SleepTimerState {
        return when (sleepTimerMode) {
            SleepTimerMode.OFF -> SleepTimerState(mode = SleepTimerMode.OFF)
            SleepTimerMode.END_OF_TRACK -> SleepTimerState(mode = SleepTimerMode.END_OF_TRACK)
            SleepTimerMode.DURATION -> {
                val endTs = sleepTimerEndTimestampMs
                val remaining = SleepTimerEngine.remainingMs(
                    nowMs = System.currentTimeMillis(),
                    endTimestampMs = endTs
                )
                SleepTimerState(
                    mode = SleepTimerMode.DURATION,
                    endTimestampMs = endTs,
                    remainingMs = remaining
                )
            }
        }
    }

    private fun triggerSleepPause() {
        serviceScope.launch {
            fadeOutAndPause()
            clearSleepTimer()
        }
    }

    private suspend fun fadeOutAndPause() {
        if (isFadingOut) return
        isFadingOut = true
        val originalVolume = player.volume

        try {
            if (player.isPlaying) {
                val steps = 10
                repeat(steps) { step ->
                    val ratio = 1f - ((step + 1) / steps.toFloat())
                    player.volume = (originalVolume * ratio).coerceIn(0f, 1f)
                    delay(1_000)
                }
            }
            player.pause()
        } finally {
            player.volume = originalVolume
            isFadingOut = false
        }
    }

    private suspend fun maybeApplyAutoRewind() {
        val pausedAt = lastPausedAtMs ?: return
        val bookId = currentBookId ?: return
        lastPausedAtMs = null

        val pausedForMs = SystemClock.elapsedRealtime() - pausedAt
        val settings = withContext(Dispatchers.IO) {
            bookDetailsRepository.getOrCreateBookSettings(bookId)
        }
        if (!AutoRewindPolicy.shouldApply(pausedForMs, settings.autoRewindAfterPauseSec)) return

        // player.currentPosition is on Main thread (serviceScope dispatcher).
        val targetPosition = AutoRewindPolicy.rewoundPosition(
            currentPositionMs = player.currentPosition,
            rewindSec = settings.autoRewindSec
        )
        player.seekTo(targetPosition)
    }

    private suspend fun applyBookSettings(bookId: Long) {
        val settings = withContext(Dispatchers.IO) {
            bookDetailsRepository.getOrCreateBookSettings(bookId)
        }
        player.setPlaybackParameters(PlaybackParameters(settings.playbackSpeed))
    }

    private fun persistCurrentProgress(resetToStart: Boolean) {
        // Snapshot player state on Main thread first, then write to DB on IO.
        val targetItem = when {
            player.mediaItemCount <= 0 -> null
            resetToStart -> player.getMediaItemAt(0)
            else -> player.currentMediaItem
        } ?: return

        val payload = targetItem.toPlaybackPayload(
            resetToStart = resetToStart,
            currentPosition = player.currentPosition,
            currentSpeed = player.playbackParameters.speed
        ) ?: return

        serviceScope.launch {
            withContext(Dispatchers.IO) {
                playbackRepository.upsertPlaybackState(
                    PlaybackStateEntity(
                        bookId = payload.bookId,
                        trackId = payload.trackId,
                        positionMs = payload.positionMs,
                        speed = payload.speed,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                playbackRepository.updateLastPlayed(payload.bookId, System.currentTimeMillis())
            }
        }
    }

    private fun persistCurrentTrackDuration() {
        // Snapshot on Main thread, then write to DB on IO.
        val duration = player.duration
        if (duration == C.TIME_UNSET || duration <= 0L) return
        val trackId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return

        serviceScope.launch {
            withContext(Dispatchers.IO) {
                bookDetailsRepository.updateTrackDuration(trackId, duration)
            }
        }
    }

    override fun onDestroy() {
        periodicSaveJob?.cancel()
        sleepTimerJob?.cancel()
        player.removeListener(playerListener)
        mediaSession?.release()
        mediaSession = null
        player.release()
        if (PlaybackServiceBridge.controller === bridgeController) {
            PlaybackServiceBridge.controller = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun MediaItem.toPlaybackPayload(
        resetToStart: Boolean,
        currentPosition: Long,
        currentSpeed: Float
    ): PlaybackPayload? {
        val bookId = mediaMetadata.extras.bookIdOrNull() ?: return null
        val trackId = mediaId.toLongOrNull() ?: return null

        return PlaybackPayload(
            bookId = bookId,
            trackId = trackId,
            positionMs = if (resetToStart) 0L else currentPosition.coerceAtLeast(0L),
            speed = if (resetToStart) 1f else currentSpeed
        )
    }

    private fun Bundle?.bookIdOrNull(): Long? {
        val value = this?.getLong(PlaybackContract.EXTRA_BOOK_ID, -1L) ?: -1L
        return value.takeIf { it >= 0L }
    }

    private data class PlaybackPayload(
        val bookId: Long,
        val trackId: Long,
        val positionMs: Long,
        val speed: Float
    )
}
