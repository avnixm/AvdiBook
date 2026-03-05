package com.avnixm.avdibook.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.avnixm.avdibook.AvdiBookApplication
import com.avnixm.avdibook.data.db.entity.PlaybackStateEntity
import com.avnixm.avdibook.data.repository.PlaybackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private var periodicSaveJob: Job? = null

    private val playbackRepository: PlaybackRepository by lazy {
        val app = application as AvdiBookApplication
        app.appContainer.playbackRepository
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) {
                persistCurrentProgress(resetToStart = false)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> persistCurrentProgress(resetToStart = true)
                Player.STATE_IDLE -> persistCurrentProgress(resetToStart = false)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build().apply {
            addListener(playerListener)
        }

        mediaSession = MediaSession.Builder(this, player)
            .build()

        periodicSaveJob = serviceScope.launch {
            while (isActive) {
                delay(10_000)
                if (player.isPlaying) {
                    persistCurrentProgress(resetToStart = false)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private fun persistCurrentProgress(resetToStart: Boolean) {
        serviceScope.launch {
            val targetItem = when {
                player.mediaItemCount <= 0 -> null
                resetToStart -> player.getMediaItemAt(0)
                else -> player.currentMediaItem
            } ?: return@launch

            val payload = targetItem.toPlaybackPayload(
                resetToStart = resetToStart,
                currentPosition = player.currentPosition,
                currentSpeed = player.playbackParameters.speed
            ) ?: return@launch

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

    override fun onDestroy() {
        periodicSaveJob?.cancel()
        player.removeListener(playerListener)
        mediaSession?.release()
        mediaSession = null
        player.release()
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
