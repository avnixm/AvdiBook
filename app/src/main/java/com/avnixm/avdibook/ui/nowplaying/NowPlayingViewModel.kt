package com.avnixm.avdibook.ui.nowplaying

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.avnixm.avdibook.AppContainer
import com.avnixm.avdibook.data.db.entity.BookSettingsEntity
import com.avnixm.avdibook.data.model.BookDetailsData
import com.avnixm.avdibook.data.repository.BookDetailsRepository
import com.avnixm.avdibook.playback.PlaybackContract
import com.avnixm.avdibook.playback.PlaybackControllerFacade
import com.avnixm.avdibook.playback.SleepTimerMode
import com.avnixm.avdibook.ui.book.BookmarkUi
import com.avnixm.avdibook.ui.book.BookTrackUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NowPlayingViewModel(
    application: Application,
    private val bookId: Long,
    private val bookDetailsRepository: BookDetailsRepository,
    private val playbackControllerFacade: PlaybackControllerFacade
) : AndroidViewModel(application) {
    private val mutableUiState = MutableStateFlow(NowPlayingUiState(bookId = bookId))
    val uiState: StateFlow<NowPlayingUiState> = mutableUiState.asStateFlow()

    private val eventsFlow = MutableSharedFlow<NowPlayingEvent>()
    val events: SharedFlow<NowPlayingEvent> = eventsFlow.asSharedFlow()

    private var positionJob: Job? = null
    private var detailsJob: Job? = null
    private var controller: MediaController? = null
    private var latestDetails: BookDetailsData? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refreshPlayerState()
        }
    }

    init {
        viewModelScope.launch {
            controller = playbackControllerFacade.connectController().also {
                it.addListener(playerListener)
            }
            bookDetailsRepository.getOrCreateBookSettings(bookId)
            refreshPlayerState()
            startPositionTicker()
        }

        detailsJob = viewModelScope.launch {
            bookDetailsRepository.observeBookDetails(bookId).collect { details ->
                latestDetails = details
                updateUiFromDetails(details)
            }
        }
    }

    fun onPlayPauseTapped() {
        viewModelScope.launch {
            if (uiState.value.isPlaying) {
                playbackControllerFacade.pause()
            } else {
                playbackControllerFacade.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            playbackControllerFacade.seekTo(positionMs)
        }
    }

    fun seekBack() {
        viewModelScope.launch {
            playbackControllerFacade.seekBy(-(uiState.value.skipBackSec * 1_000L))
        }
    }

    fun seekForward() {
        viewModelScope.launch {
            playbackControllerFacade.seekBy(uiState.value.skipForwardSec * 1_000L)
        }
    }

    fun setSpeed(speed: Float) {
        viewModelScope.launch {
            playbackControllerFacade.setSpeed(speed)
            mutateSettings { current -> current.copy(playbackSpeed = speed) }
            refreshPlayerState()
        }
    }

    fun addBookmark(note: String?) {
        viewModelScope.launch {
            val result = playbackControllerFacade.addBookmark(note)
            if (result.isSuccess) {
                eventsFlow.emit(NowPlayingEvent.ShowMessage("Bookmark added"))
            } else {
                eventsFlow.emit(
                    NowPlayingEvent.ShowMessage(
                        result.exceptionOrNull()?.message ?: "Unable to add bookmark"
                    )
                )
            }
        }
    }

    fun setSleepDuration(minutes: Int) {
        viewModelScope.launch {
            playbackControllerFacade.setSleepTimerDuration(minutes)
            refreshPlayerState()
        }
    }

    fun setSleepEndOfTrack() {
        viewModelScope.launch {
            playbackControllerFacade.setSleepTimerEndOfTrack()
            refreshPlayerState()
        }
    }

    fun clearSleepTimer() {
        viewModelScope.launch {
            playbackControllerFacade.clearSleepTimer()
            refreshPlayerState()
        }
    }

    fun onTrackSelected(trackId: Long) {
        viewModelScope.launch {
            val result = playbackControllerFacade.playBookFromTrack(
                bookId = bookId,
                trackId = trackId,
                positionMs = 0L
            )
            if (result.isFailure) {
                eventsFlow.emit(
                    NowPlayingEvent.ShowMessage(
                        result.exceptionOrNull()?.message ?: "Unable to play selected track"
                    )
                )
            }
        }
    }

    fun onBookmarkSelected(trackId: Long, positionMs: Long) {
        viewModelScope.launch {
            val result = playbackControllerFacade.playBookFromTrack(
                bookId = bookId,
                trackId = trackId,
                positionMs = positionMs
            )
            if (result.isFailure) {
                eventsFlow.emit(
                    NowPlayingEvent.ShowMessage(
                        result.exceptionOrNull()?.message ?: "Unable to play bookmark"
                    )
                )
            }
        }
    }

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (isActive) {
                refreshPlayerState()
                delay(1_000)
            }
        }
    }

    private fun refreshPlayerState() {
        viewModelScope.launch {
            val mediaController = controller ?: playbackControllerFacade.connectController().also {
                controller = it
            }
            val durationMs = mediaController.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            val metadata = mediaController.currentMediaItem?.mediaMetadata
            val sleepState = playbackControllerFacade.getSleepTimerState()

            mutableUiState.update { current ->
                current.copy(
                    bookTitle = metadata?.albumTitle?.toString().takeUnless { it.isNullOrBlank() }
                        ?: current.bookTitle,
                    trackTitle = metadata?.title?.toString().orEmpty(),
                    isPlaying = mediaController.isPlaying,
                    positionMs = mediaController.currentPosition.coerceAtLeast(0L),
                    durationMs = durationMs.coerceAtLeast(0L),
                    speed = mediaController.playbackParameters.speed,
                    sleepLabel = sleepState.toLabel()
                )
            }

            latestDetails?.let { details ->
                updateUiFromDetails(details)
            }
        }
    }

    private fun updateUiFromDetails(details: BookDetailsData) {
        val mediaItem = controller?.currentMediaItem
        val currentBookId = mediaItem?.mediaMetadata?.extras
            ?.getLong(PlaybackContract.EXTRA_BOOK_ID, -1L)
            ?.takeIf { it >= 0L }
        val currentTrackId = if (currentBookId == bookId) {
            mediaItem?.mediaId?.toLongOrNull()
        } else {
            null
        }
        val settings = details.settings
        val mappedTracks = details.tracks.map { track ->
            BookTrackUi(
                trackId = track.id,
                title = track.title,
                trackIndex = track.trackIndex,
                durationMs = track.durationMs,
                isPlaying = track.id == currentTrackId
            )
        }
        val mappedBookmarks = details.bookmarks.map { bookmark ->
            BookmarkUi(
                id = bookmark.id,
                trackId = bookmark.trackId,
                positionMs = bookmark.positionMs,
                note = bookmark.note,
                createdAt = bookmark.createdAt,
                trackTitle = details.tracks.firstOrNull { it.id == bookmark.trackId }?.title ?: "Track"
            )
        }

        mutableUiState.update { current ->
            current.copy(
                bookTitle = details.book?.title ?: current.bookTitle,
                tracks = mappedTracks,
                bookmarks = mappedBookmarks,
                skipBackSec = settings?.skipBackSec ?: current.skipBackSec,
                skipForwardSec = settings?.skipForwardSec ?: current.skipForwardSec,
                speed = settings?.playbackSpeed ?: current.speed
            )
        }
    }

    private suspend fun mutateSettings(change: (BookSettingsEntity) -> BookSettingsEntity) {
        val current = bookDetailsRepository.getOrCreateBookSettings(bookId)
        bookDetailsRepository.upsertBookSettings(change(current))
    }

    override fun onCleared() {
        positionJob?.cancel()
        detailsJob?.cancel()
        controller?.removeListener(playerListener)
        super.onCleared()
    }

    companion object {
        fun factory(
            application: Application,
            appContainer: AppContainer,
            bookId: Long
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NowPlayingViewModel(
                    application = application,
                    bookId = bookId,
                    bookDetailsRepository = appContainer.bookDetailsRepository,
                    playbackControllerFacade = appContainer.playbackControllerFacade
                )
            }
        }
    }
}

sealed interface NowPlayingEvent {
    data class ShowMessage(val message: String) : NowPlayingEvent
}

private fun com.avnixm.avdibook.playback.SleepTimerState.toLabel(): String {
    return when (mode) {
        SleepTimerMode.OFF -> "Off"
        SleepTimerMode.END_OF_TRACK -> "End of track"
        SleepTimerMode.DURATION -> {
            val minutes = ((remainingMs ?: 0L) / 60_000L).coerceAtLeast(0L)
            if (minutes <= 0) "<1 min" else "${minutes} min"
        }
    }
}
