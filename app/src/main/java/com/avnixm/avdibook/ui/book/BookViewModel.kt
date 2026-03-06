package com.avnixm.avdibook.ui.book

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.avnixm.avdibook.AppContainer
import com.avnixm.avdibook.data.db.entity.BookSettingsEntity
import com.avnixm.avdibook.data.model.BookDetailsData
import com.avnixm.avdibook.data.prefs.AppPreferences
import com.avnixm.avdibook.data.repository.BookDetailsRepository
import com.avnixm.avdibook.playback.PlaybackContract
import com.avnixm.avdibook.playback.PlaybackControllerFacade
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BookViewModel(
    application: Application,
    private val bookId: Long,
    private val autoPlay: Boolean = false,
    private val bookDetailsRepository: BookDetailsRepository,
    private val playbackControllerFacade: PlaybackControllerFacade,
    private val appPreferences: AppPreferences
) : AndroidViewModel(application) {
    private val eventsFlow = MutableSharedFlow<BookEvent>()
    val events: SharedFlow<BookEvent> = eventsFlow.asSharedFlow()

    private var pendingPlaybackRequest: PendingPlaybackRequest? = null
    private val currentTrackId = MutableStateFlow<Long?>(null)
    private var currentTrackPollingJob: Job? = null
    private var latestDetails: BookDetailsData? = null

    val uiState: StateFlow<BookUiState> = combine(
        bookDetailsRepository.observeBookDetails(bookId),
        currentTrackId
    ) { details, activeTrackId ->
        latestDetails = details

        val settings = details.settings
        val chapters = details.chapters.map { chapter ->
            ChapterUi(
                id = chapter.id,
                title = chapter.title,
                startMs = chapter.startMs,
                endMs = chapter.endMs,
                trackId = chapter.trackId,
                isCurrent = chapter.startMs <= details.progress.progressMs &&
                    (chapter.endMs == null || details.progress.progressMs < chapter.endMs)
            )
        }.ifEmpty {
            var cursor = 0L
            details.tracks.mapIndexed { index, track ->
                val start = cursor
                val end = track.durationMs?.let { start + it }
                cursor = end ?: cursor
                ChapterUi(
                    id = -track.id,
                    title = track.title.ifBlank { "Track ${index + 1}" },
                    startMs = start,
                    endMs = end,
                    trackId = track.id,
                    isCurrent = details.playbackState?.trackId == track.id
                )
            }
        }
        BookUiState(
            isLoading = false,
            bookId = bookId,
            title = details.book?.title ?: "Book",
            tracks = details.tracks.map { track ->
                BookTrackUi(
                    trackId = track.id,
                    title = track.title,
                    trackIndex = track.trackIndex,
                    durationMs = track.durationMs,
                    isPlaying = track.id == activeTrackId
                )
            },
            chapters = chapters,
            bookmarks = details.bookmarks.map { bookmark ->
                val trackTitle = details.tracks.firstOrNull { it.id == bookmark.trackId }?.title ?: "Track"
                BookmarkUi(
                    id = bookmark.id,
                    trackId = bookmark.trackId,
                    positionMs = bookmark.positionMs,
                    note = bookmark.note,
                    createdAt = bookmark.createdAt,
                    trackTitle = trackTitle
                )
            },
            settings = settings?.let {
                BookSettingsUi(
                    playbackSpeed = it.playbackSpeed,
                    skipForwardSec = it.skipForwardSec,
                    skipBackSec = it.skipBackSec,
                    autoRewindSec = it.autoRewindSec,
                    autoRewindAfterPauseSec = it.autoRewindAfterPauseSec,
                    useLoudnessBoost = it.useLoudnessBoost
                )
            },
            hasPlaybackState = details.playbackState != null,
            bookProgressPercent = details.progress.percent,
            timeLeftMs = details.progress.remainingMs,
            isProgressEstimated = details.progress.isEstimated,
            isMissingSource = details.book?.isMissingSource == true,
            coverArtPath = details.book?.coverArtPath
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BookUiState(bookId = bookId)
    )

    init {
        viewModelScope.launch {
            bookDetailsRepository.getOrCreateBookSettings(bookId)
        }

        if (autoPlay) {
            viewModelScope.launch {
                uiState.first { !it.isLoading }
                requestPlayback(PendingPlaybackRequest(trackId = null, positionMs = 0L))
            }
        }

        currentTrackPollingJob = viewModelScope.launch {
            while (isActive) {
                runCatching {
                    val controller = playbackControllerFacade.connectController()
                    val mediaItem = controller.currentMediaItem
                    val currentBookId = mediaItem?.mediaMetadata?.extras
                        ?.getLong(PlaybackContract.EXTRA_BOOK_ID, -1L)
                        ?.takeIf { it >= 0L }
                    val mediaId = if (currentBookId == bookId) {
                        mediaItem.mediaId.toLongOrNull()
                    } else {
                        null
                    }
                    currentTrackId.update { mediaId }
                }
                delay(1_000)
            }
        }
    }

    fun onPlayResumeClick() {
        requestPlayback(PendingPlaybackRequest(trackId = null, positionMs = 0L))
    }

    fun onTrackClick(trackId: Long) {
        requestPlayback(PendingPlaybackRequest(trackId = trackId, positionMs = 0L))
    }

    fun onBookmarkClick(trackId: Long, positionMs: Long) {
        requestPlayback(PendingPlaybackRequest(trackId = trackId, positionMs = positionMs))
    }

    fun onNotificationPermissionResult(isGranted: Boolean) {
        viewModelScope.launch {
            val request = pendingPlaybackRequest ?: return@launch
            pendingPlaybackRequest = null

            if (!isGranted) {
                eventsFlow.emit(
                    BookEvent.ShowMessage(
                        "Notification permission denied. Playback continues, but lockscreen controls may be limited."
                    )
                )
            }

            executePlayback(request)
        }
    }

    fun onAddBookmark(note: String?) {
        viewModelScope.launch {
            val details = latestDetails
            val fallbackTrack = details?.tracks?.firstOrNull()?.id ?: return@launch
            val playback = details.playbackState

            val trackId = playback?.trackId ?: fallbackTrack
            val positionMs = playback?.positionMs ?: 0L

            val bookmarkId = bookDetailsRepository.addBookmark(
                bookId = bookId,
                trackId = trackId,
                positionMs = positionMs,
                note = note
            )

            if (bookmarkId > 0L) {
                eventsFlow.emit(BookEvent.ShowMessage("Bookmark added"))
            }
        }
    }

    fun onDeleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            bookDetailsRepository.deleteBookmark(bookmarkId)
        }
    }

    fun onSpeedSelected(speed: Float) {
        viewModelScope.launch {
            mutateSettings { current -> current.copy(playbackSpeed = speed) }
            playbackControllerFacade.applyBookSettingsIfCurrent(bookId)
        }
    }

    fun onSkipBackSelected(seconds: Int) {
        viewModelScope.launch {
            mutateSettings { current -> current.copy(skipBackSec = seconds) }
        }
    }

    fun onSkipForwardSelected(seconds: Int) {
        viewModelScope.launch {
            mutateSettings { current -> current.copy(skipForwardSec = seconds) }
        }
    }

    fun onAutoRewindSelected(seconds: Int) {
        viewModelScope.launch {
            mutateSettings { current -> current.copy(autoRewindSec = seconds) }
        }
    }

    fun onAutoRewindThresholdSelected(seconds: Int) {
        viewModelScope.launch {
            mutateSettings { current -> current.copy(autoRewindAfterPauseSec = seconds) }
        }
    }

    private fun requestPlayback(request: PendingPlaybackRequest) {
        viewModelScope.launch {
            if (latestDetails?.book?.isMissingSource == true) {
                eventsFlow.emit(
                    BookEvent.ShowMessage("Source access missing. Re-import/relink required.")
                )
                return@launch
            }
            val shouldRequestPermission = shouldRequestNotificationPermission()
            if (shouldRequestPermission) {
                pendingPlaybackRequest = request
                appPreferences.setNotificationPermissionAskedOnce(true)
                eventsFlow.emit(BookEvent.RequestNotificationPermission)
                return@launch
            }

            executePlayback(request)
        }
    }

    private suspend fun executePlayback(request: PendingPlaybackRequest) {
        val result = playbackControllerFacade.playBookFromTrack(
            bookId = bookId,
            trackId = request.trackId,
            positionMs = request.positionMs
        )

        if (result.isSuccess) {
            eventsFlow.emit(BookEvent.NavigateToNowPlaying(bookId))
        } else {
            val e = result.exceptionOrNull()
            Log.e("BookViewModel", "executePlayback failed for bookId=$bookId", e)
            val debugMsg = buildString {
                append("[DEBUG] Playback failed\n")
                append("${e?.javaClass?.simpleName}: ${e?.message}")
                val cause = e?.cause
                if (cause != null) append("\nCaused by: ${cause.javaClass.simpleName}: ${cause.message}")
            }
            eventsFlow.emit(BookEvent.ShowMessage(debugMsg))
        }
    }

    private suspend fun mutateSettings(change: (BookSettingsEntity) -> BookSettingsEntity) {
        val current = bookDetailsRepository.getOrCreateBookSettings(bookId)
        bookDetailsRepository.upsertBookSettings(change(current))
    }

    private suspend fun shouldRequestNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false

        val notificationsEnabled = NotificationManagerCompat.from(getApplication()).areNotificationsEnabled()
        if (notificationsEnabled) return false

        return !appPreferences.isNotificationPermissionAskedOnce()
    }

    override fun onCleared() {
        currentTrackPollingJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(
            application: Application,
            appContainer: AppContainer,
            bookId: Long,
            autoPlay: Boolean = false
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                BookViewModel(
                    application = application,
                    bookId = bookId,
                    autoPlay = autoPlay,
                    bookDetailsRepository = appContainer.bookDetailsRepository,
                    playbackControllerFacade = appContainer.playbackControllerFacade,
                    appPreferences = appContainer.appPreferences
                )
            }
        }
    }
}

private data class PendingPlaybackRequest(
    val trackId: Long?,
    val positionMs: Long
)

sealed interface BookEvent {
    data object RequestNotificationPermission : BookEvent
    data class NavigateToNowPlaying(val bookId: Long) : BookEvent
    data class ShowMessage(val message: String) : BookEvent
}
