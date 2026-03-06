package com.avnixm.avdibook.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.avnixm.avdibook.AppContainer
import com.avnixm.avdibook.data.repository.BackupRepository
import com.avnixm.avdibook.data.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
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
    private val backupRepository: BackupRepository
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
                    lastPlayedAt = item.book.lastPlayedAt,
                    progressPercent = item.progress.percent,
                    timeLeftMs = item.progress.remainingMs,
                    isProgressEstimated = item.progress.isEstimated,
                    isMissingSource = item.book.isMissingSource,
                    coverArtPath = item.book.coverArtPath
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                libraryRepository.refreshMissingSourceFlags()
            }
        }
    }

    fun importFolder(treeUriString: String) {
        viewModelScope.launch {
            isImporting.update { true }
            val result = libraryRepository.importFolder(android.net.Uri.parse(treeUriString))
            isImporting.update { false }

            result.exceptionOrNull()?.let { throwable ->
                eventsFlow.emit(LibraryEvent.ShowMessage(throwable.message ?: "Folder import failed."))
                return@launch
            }
            libraryRepository.refreshMissingSourceFlags()
            eventsFlow.emit(LibraryEvent.ShowMessage("Folder imported."))
        }
    }

    fun exportBackup(destinationUriString: String) {
        viewModelScope.launch {
            val result = backupRepository.exportToUri(android.net.Uri.parse(destinationUriString))
            result.exceptionOrNull()?.let { throwable ->
                eventsFlow.emit(LibraryEvent.ShowMessage(throwable.message ?: "Backup export failed."))
                return@launch
            }
            eventsFlow.emit(LibraryEvent.ShowMessage("Backup exported successfully."))
        }
    }

    fun restoreBackup(sourceUriString: String) {
        viewModelScope.launch {
            val uri = android.net.Uri.parse(sourceUriString)
            val validateResult = backupRepository.validateBackup(uri)
            validateResult.exceptionOrNull()?.let { throwable ->
                eventsFlow.emit(LibraryEvent.ShowMessage(throwable.message ?: "Invalid backup file."))
                return@launch
            }
            val result = backupRepository.restoreFromUri(uri)
            result.exceptionOrNull()?.let { throwable ->
                eventsFlow.emit(LibraryEvent.ShowMessage(throwable.message ?: "Backup restore failed."))
                return@launch
            }
            eventsFlow.emit(LibraryEvent.ShowMessage("Backup restored."))
        }
    }

    fun importFiles(fileUriStrings: List<String>) {
        viewModelScope.launch {
            isImporting.update { true }
            val result = libraryRepository.importFiles(fileUriStrings.map(android.net.Uri::parse))
            isImporting.update { false }

            result.exceptionOrNull()?.let { throwable ->
                eventsFlow.emit(LibraryEvent.ShowMessage(throwable.message ?: "File import failed."))
                return@launch
            }
            libraryRepository.refreshMissingSourceFlags()
            eventsFlow.emit(LibraryEvent.ShowMessage("Files imported."))
        }
    }

    fun onBookSelected(bookId: Long) {
        viewModelScope.launch {
            val selected = uiState.value.books.firstOrNull { it.bookId == bookId }
            if (selected?.isMissingSource == true) {
                eventsFlow.emit(
                    LibraryEvent.ShowMessage("Source access missing. Re-import/relink required.")
                )
                return@launch
            }
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
                        libraryRepository = appContainer.libraryRepository,
                        backupRepository = appContainer.backupRepository
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
