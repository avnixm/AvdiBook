package com.avnixm.avdibook.playback

import android.content.Context
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.session.MediaController
import com.avnixm.avdibook.data.repository.LibraryRepository
import com.avnixm.avdibook.data.repository.PlaybackRepository

class PlaybackControllerFacade(
    private val appContext: Context,
    private val libraryRepository: LibraryRepository,
    private val playbackRepository: PlaybackRepository
) {
    suspend fun connectController(): MediaController = PlaybackClient.connect(appContext)

    suspend fun playBook(bookId: Long): Result<Unit> = runCatching {
        val controller = connectController()
        val book = libraryRepository.getBook(bookId) ?: error("Book not found.")
        val tracks = libraryRepository.getTracks(bookId)
        require(tracks.isNotEmpty()) { "This book has no playable audio tracks." }

        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setAlbumTitle(book.title)
                        .setExtras(bundleOf(PlaybackContract.EXTRA_BOOK_ID to book.id))
                        .build()
                )
                .build()
        }

        val playbackState = playbackRepository.getPlaybackState(bookId)
        val startIndex = ResumeResolver.resolveStartIndex(
            trackIds = tracks.map { it.id },
            trackId = playbackState?.trackId
        )
        val startPosition = playbackState?.positionMs ?: 0L

        controller.setMediaItems(mediaItems, startIndex, startPosition)
        controller.prepare()

        val speed = playbackState?.speed ?: 1f
        controller.setPlaybackParameters(PlaybackParameters(speed))
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

}
