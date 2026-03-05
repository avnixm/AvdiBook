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
import com.avnixm.avdibook.data.repository.LibraryRepository
import com.avnixm.avdibook.playback.PlaybackControllerFacade
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NowPlayingViewModel(
    application: Application,
    private val bookId: Long,
    private val libraryRepository: LibraryRepository,
    private val playbackControllerFacade: PlaybackControllerFacade
) : AndroidViewModel(application) {
    private val mutableUiState = MutableStateFlow(NowPlayingUiState(bookId = bookId))
    val uiState: StateFlow<NowPlayingUiState> = mutableUiState.asStateFlow()

    private var positionJob: Job? = null
    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refreshPlayerState()
        }
    }

    init {
        viewModelScope.launch {
            val book = libraryRepository.getBook(bookId)
            mutableUiState.update { it.copy(bookTitle = book?.title ?: "Now Playing") }

            controller = playbackControllerFacade.connectController().also {
                it.addListener(playerListener)
            }
            refreshPlayerState()
            startPositionTicker()
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

    fun seekBy(deltaMs: Long) {
        viewModelScope.launch {
            playbackControllerFacade.seekBy(deltaMs)
        }
    }

    fun setSpeed(speed: Float) {
        viewModelScope.launch {
            playbackControllerFacade.setSpeed(speed)
            refreshPlayerState()
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

            mutableUiState.update { current ->
                current.copy(
                    bookTitle = metadata?.albumTitle?.toString().takeUnless { it.isNullOrBlank() }
                        ?: current.bookTitle,
                    trackTitle = metadata?.title?.toString().orEmpty(),
                    isPlaying = mediaController.isPlaying,
                    positionMs = mediaController.currentPosition.coerceAtLeast(0L),
                    durationMs = durationMs.coerceAtLeast(0L),
                    speed = mediaController.playbackParameters.speed
                )
            }
        }
    }

    override fun onCleared() {
        positionJob?.cancel()
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
                    libraryRepository = appContainer.libraryRepository,
                    playbackControllerFacade = appContainer.playbackControllerFacade
                )
            }
        }
    }
}
