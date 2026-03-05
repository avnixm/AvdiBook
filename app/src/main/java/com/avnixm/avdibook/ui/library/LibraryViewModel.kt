package com.avnixm.avdibook.ui.library

import android.app.Application
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.avnixm.avdibook.AppContainer
import com.avnixm.avdibook.data.prefs.AppPreferences
import com.avnixm.avdibook.data.repository.LibraryRepository
import com.avnixm.avdibook.playback.PlaybackControllerFacade
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    application: Application,
    private val libraryRepository: LibraryRepository,
    private val playbackControllerFacade: PlaybackControllerFacade,
    private val appPreferences: AppPreferences
) : AndroidViewModel(application) {
    private val isImporting = MutableStateFlow(false)
    private val eventsFlow = MutableSharedFlow<LibraryEvent>()
    private var pendingBookIdForPermission: Long? = null

    val events: SharedFlow<LibraryEvent> = eventsFlow.asSharedFlow()

    val uiState: StateFlow<LibraryUiState> = combine(
        libraryRepository.observeLibrary(),
        isImporting
    ) { books, importing ->
        LibraryUiState(
            isImporting = importing,
            books = books.map { item ->
                BookWithProgressUi(
                    bookId = item.book.id,
                    title = item.book.title,
                    trackCount = item.trackCount,
                    hasResume = item.playbackState != null,
                    resumePositionMs = item.playbackState?.positionMs ?: 0L,
                    lastPlayedAt = item.book.lastPlayedAt
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    fun importFolder(treeUriString: String) {
        viewModelScope.launch {
            isImporting.update { true }
            val result = libraryRepository.importFolder(android.net.Uri.parse(treeUriString))
            isImporting.update { false }

            result.exceptionOrNull()?.let { throwable ->
                eventsFlow.emit(LibraryEvent.ShowMessage(throwable.message ?: "Folder import failed."))
            }
        }
    }

    fun importFiles(fileUriStrings: List<String>) {
        viewModelScope.launch {
            isImporting.update { true }
            val result = libraryRepository.importFiles(fileUriStrings.map(android.net.Uri::parse))
            isImporting.update { false }

            result.exceptionOrNull()?.let { throwable ->
                eventsFlow.emit(LibraryEvent.ShowMessage(throwable.message ?: "File import failed."))
            }
        }
    }

    fun onBookSelected(bookId: Long) {
        viewModelScope.launch {
            val shouldRequestPermission = shouldRequestNotificationPermission()
            if (shouldRequestPermission) {
                pendingBookIdForPermission = bookId
                appPreferences.setNotificationPermissionAskedOnce(true)
                eventsFlow.emit(LibraryEvent.RequestNotificationPermission)
                return@launch
            }

            startPlayback(bookId)
        }
    }

    fun onSkipImportForNow() {
        viewModelScope.launch {
            eventsFlow.emit(
                LibraryEvent.ShowMessage("No worries. You can import books any time from this screen.")
            )
        }
    }

    fun onNotificationPermissionResult(isGranted: Boolean) {
        viewModelScope.launch {
            val bookId = pendingBookIdForPermission ?: return@launch
            pendingBookIdForPermission = null

            if (!isGranted) {
                eventsFlow.emit(
                    LibraryEvent.ShowMessage(
                        "Notification permission denied. Playback continues, but lockscreen controls may be limited."
                    )
                )
            }

            startPlayback(bookId)
        }
    }

    private suspend fun startPlayback(bookId: Long) {
        val result = playbackControllerFacade.playBook(bookId)
        if (result.isSuccess) {
            eventsFlow.emit(LibraryEvent.NavigateToNowPlaying(bookId))
        } else {
            eventsFlow.emit(
                LibraryEvent.ShowMessage(
                    result.exceptionOrNull()?.message ?: "Failed to start playback."
                )
            )
        }
    }

    private suspend fun shouldRequestNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false

        val notificationsEnabled = NotificationManagerCompat.from(getApplication()).areNotificationsEnabled()
        if (notificationsEnabled) return false

        return !appPreferences.isNotificationPermissionAskedOnce()
    }

    companion object {
        fun factory(application: Application, appContainer: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    LibraryViewModel(
                        application = application,
                        libraryRepository = appContainer.libraryRepository,
                        playbackControllerFacade = appContainer.playbackControllerFacade,
                        appPreferences = appContainer.appPreferences
                    )
                }
            }
    }
}

data class LibraryUiState(
    val isImporting: Boolean = false,
    val books: List<BookWithProgressUi> = emptyList()
)

sealed interface LibraryEvent {
    data object RequestNotificationPermission : LibraryEvent
    data class NavigateToNowPlaying(val bookId: Long) : LibraryEvent
    data class ShowMessage(val message: String) : LibraryEvent
}
