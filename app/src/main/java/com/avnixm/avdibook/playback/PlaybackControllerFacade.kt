package com.avnixm.avdibook.playback

import android.content.Context
import android.util.Log
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.session.MediaController
import com.avnixm.avdibook.data.db.entity.TrackEntity
import com.avnixm.avdibook.data.repository.BookDetailsRepository
import com.avnixm.avdibook.data.repository.LibraryRepository
import com.avnixm.avdibook.data.repository.PlaybackRepository

class PlaybackControllerFacade(
    private val appContext: Context,
    private val libraryRepository: LibraryRepository,
    private val playbackRepository: PlaybackRepository,
    private val bookDetailsRepository: BookDetailsRepository
) {
    suspend fun connectController(): MediaController = PlaybackClient.connect(appContext)

    suspend fun playBook(bookId: Long): Result<Unit> {
        return playBookFromTrack(bookId = bookId, trackId = null, positionMs = 0L)
    }

    suspend fun playBookFromTrack(
        bookId: Long,
        trackId: Long?,
        positionMs: Long = 0L
    ): Result<Unit> = runCatching {
        Log.d("PlaybackFacade", "playBookFromTrack: bookId=$bookId trackId=$trackId positionMs=$positionMs")
        val controller = connectController()
        val book = libraryRepository.getBook(bookId) ?: error("Book not found.")
        val tracks = libraryRepository.getTracks(bookId)
        Log.d("PlaybackFacade", "book=${book.title}, tracks=${tracks.size}, uris=${tracks.map { it.uri }}")
        require(tracks.isNotEmpty()) { "This book has no playable audio tracks." }

        val mediaItems = tracks.map { track -> track.toMediaItem(bookId = book.id, bookTitle = book.title) }

        val playbackState = playbackRepository.getPlaybackState(bookId)
        val targetTrackId = trackId ?: playbackState?.trackId
        val startIndex = ResumeResolver.resolveStartIndex(
            trackIds = tracks.map { it.id },
            trackId = targetTrackId
        )

        val startPosition = when {
            trackId != null -> positionMs.coerceAtLeast(0L)
            playbackState != null -> playbackState.positionMs
            else -> 0L
        }

        controller.setMediaItems(mediaItems, startIndex, startPosition)
        controller.prepare()

        val settings = bookDetailsRepository.getOrCreateBookSettings(bookId)
        controller.setPlaybackParameters(PlaybackParameters(settings.playbackSpeed))
        controller.play()

        playbackRepository.updateLastPlayed(bookId, System.currentTimeMillis())
    }

    suspend fun play() {
        connectController().play()
    }

    suspend fun pause() {
        connectController().pause()
    }

    suspend fun seekTo(positionMs: Long) {
        connectController().seekTo(positionMs.coerceAtLeast(0L))
    }

    suspend fun seekBy(deltaMs: Long) {
        val controller = connectController()
        val target = (controller.currentPosition + deltaMs).coerceAtLeast(0L)
        controller.seekTo(target)
    }

    suspend fun setSpeed(speed: Float) {
        connectController().setPlaybackParameters(PlaybackParameters(speed))
    }

    suspend fun addBookmark(note: String?): Result<Long> = runCatching {
        val controller = connectController()
        val currentItem = controller.currentMediaItem ?: error("No active media item.")
        val bookId = currentItem.mediaMetadata.extras?.getLong(PlaybackContract.EXTRA_BOOK_ID, -1L)
            ?.takeIf { it >= 0L }
            ?: error("Cannot resolve current book.")
        val trackId = currentItem.mediaId.toLongOrNull() ?: error("Cannot resolve current track.")

        bookDetailsRepository.addBookmark(
            bookId = bookId,
            trackId = trackId,
            positionMs = controller.currentPosition,
            note = note
        )
    }

    suspend fun setSleepTimerDuration(minutes: Int) {
        PlaybackServiceBridge.controller?.setSleepTimerDuration(minutes)
    }

    suspend fun setSleepTimerEndOfTrack() {
        PlaybackServiceBridge.controller?.setSleepTimerEndOfTrack()
    }

    suspend fun clearSleepTimer() {
        PlaybackServiceBridge.controller?.clearSleepTimer()
    }

    suspend fun getSleepTimerState(): SleepTimerState {
        return PlaybackServiceBridge.controller?.getSleepTimerState() ?: SleepTimerState()
    }

    suspend fun applyBookSettingsIfCurrent(bookId: Long) {
        val controller = connectController()
        val currentBookId = controller.currentMediaItem
            ?.mediaMetadata
            ?.extras
            ?.getLong(PlaybackContract.EXTRA_BOOK_ID, -1L)
            ?.takeIf { it >= 0L }
            ?: return

        if (currentBookId != bookId) return

        val settings = bookDetailsRepository.getOrCreateBookSettings(bookId)
        controller.setPlaybackParameters(PlaybackParameters(settings.playbackSpeed))
    }

    private fun TrackEntity.toMediaItem(bookId: Long, bookTitle: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setAlbumTitle(bookTitle)
                    .setExtras(bundleOf(PlaybackContract.EXTRA_BOOK_ID to bookId))
                    .build()
            )
            .build()
    }
}
