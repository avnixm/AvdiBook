package com.avnixm.avdibook.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.avnixm.avdibook.AppContainer
import com.avnixm.avdibook.data.repository.LibraryRepository
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
    private val libraryRepository: LibraryRepository
) : AndroidViewModel(application) {
    private val isImporting = MutableStateFlow(false)
    private val hasSkippedImport = MutableStateFlow(false)
    private val eventsFlow = MutableSharedFlow<LibraryEvent>()

    val events: SharedFlow<LibraryEvent> = eventsFlow.asSharedFlow()

    val uiState: StateFlow<LibraryUiState> = combine(
        libraryRepository.observeLibrary(),
        isImporting,
        hasSkippedImport
    ) { books, importing, skipped ->
        LibraryUiState(
            isImporting = importing,
            showImportOnboarding = books.isEmpty() && !skipped,
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
            eventsFlow.emit(LibraryEvent.NavigateToBook(bookId))
        }
    }

    fun onSkipImportForNow() {
        hasSkippedImport.update { true }
    }

    companion object {
        fun factory(application: Application, appContainer: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    LibraryViewModel(
                        application = application,
                        libraryRepository = appContainer.libraryRepository
                    )
                }
            }
    }
}

data class LibraryUiState(
    val isImporting: Boolean = false,
    val showImportOnboarding: Boolean = true,
    val books: List<BookWithProgressUi> = emptyList()
)

sealed interface LibraryEvent {
    data class NavigateToBook(val bookId: Long) : LibraryEvent
    data class ShowMessage(val message: String) : LibraryEvent
}
