package com.avnixm.avdibook.ui.nowplaying

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avnixm.avdibook.AvdiBookApplication
import com.avnixm.avdibook.ui.book.BookmarkUi
import com.avnixm.avdibook.ui.book.BookTrackUi
import com.avnixm.avdibook.ui.common.TimeFormatters
import kotlinx.coroutines.flow.collectLatest

private val SHEET_TABS = listOf("Tracks", "Bookmarks")

@Composable
fun NowPlayingRoute(
    bookId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as AvdiBookApplication
    val viewModel: NowPlayingViewModel = viewModel(
        key = "now_playing_$bookId",
        factory = NowPlayingViewModel.factory(
            application = application,
            appContainer = application.appContainer,
            bookId = bookId
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is NowPlayingEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    NowPlayingScreen(
        modifier = modifier,
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onPlayPause = viewModel::onPlayPauseTapped,
        onSeekBack = viewModel::seekBack,
        onSeekForward = viewModel::seekForward,
        onSeekTo = viewModel::seekTo,
        onSetSpeed = viewModel::setSpeed,
        onAddBookmark = viewModel::addBookmark,
        onSetSleepDuration = viewModel::setSleepDuration,
        onSetSleepEndOfTrack = viewModel::setSleepEndOfTrack,
        onClearSleepTimer = viewModel::clearSleepTimer,
        onTrackSelected = viewModel::onTrackSelected,
        onBookmarkSelected = viewModel::onBookmarkSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    uiState: NowPlayingUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onAddBookmark: (String?) -> Unit,
    onSetSleepDuration: (Int) -> Unit,
    onSetSleepEndOfTrack: () -> Unit,
    onClearSleepTimer: () -> Unit,
    onTrackSelected: (Long) -> Unit,
    onBookmarkSelected: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    var isSpeedDialogVisible by remember { mutableStateOf(false) }
    var isSleepDialogVisible by remember { mutableStateOf(false) }
    var isBookmarkNoteDialogVisible by remember { mutableStateOf(false) }
    var bookmarkNoteText by remember { mutableStateOf("") }
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    var selectedSheetTab by remember { mutableIntStateOf(0) }

    val maxDuration = uiState.durationMs.coerceAtLeast(1L)
    var sliderValue by remember(uiState.positionMs, uiState.durationMs) {
        mutableFloatStateOf(uiState.positionMs.coerceIn(0L, maxDuration).toFloat())
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                actions = {
                    TextButton(onClick = { isBottomSheetVisible = true }) {
                        Text("Browse")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = uiState.bookTitle.ifBlank { "Unknown Book" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = uiState.trackTitle.ifBlank { "No track loaded" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    ActionChip(
                        label = "Speed ${uiState.speed}x",
                        onClick = { isSpeedDialogVisible = true }
                    )
                }
                item {
                    ActionChip(
                        label = "Sleep ${uiState.sleepLabel}",
                        onClick = { isSleepDialogVisible = true }
                    )
                }
                item {
                    ActionChip(
                        label = "Bookmark",
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAddBookmark(null)
                        },
                        onLongClick = {
                            bookmarkNoteText = ""
                            isBookmarkNoteDialogVisible = true
                        }
                    )
                }
            }

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onSeekTo(sliderValue.toLong()) },
                valueRange = 0f..maxDuration.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(TimeFormatters.formatClock(uiState.positionMs), style = MaterialTheme.typography.bodyMedium)
                Text(TimeFormatters.formatClock(uiState.durationMs), style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSeekBack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("-${uiState.skipBackSec}s")
                }
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPlayPause()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (uiState.isPlaying) "Pause" else "Play")
                }
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSeekForward()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+${uiState.skipForwardSec}s")
                }
            }
        }
    }

    if (isSpeedDialogVisible) {
        AlertDialog(
            onDismissRequest = { isSpeedDialogVisible = false },
            title = { Text("Playback Speed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SPEED_OPTIONS.forEach { speed ->
                        OutlinedButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSetSpeed(speed)
                                isSpeedDialogVisible = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${speed}x")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isSpeedDialogVisible = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (isSleepDialogVisible) {
        AlertDialog(
            onDismissRequest = { isSleepDialogVisible = false },
            title = { Text("Sleep Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSetSleepDuration(15)
                            isSleepDialogVisible = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("15 min")
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSetSleepDuration(30)
                            isSleepDialogVisible = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("30 min")
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSetSleepEndOfTrack()
                            isSleepDialogVisible = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("End of track")
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClearSleepTimer()
                            isSleepDialogVisible = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Off")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isSleepDialogVisible = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (isBookmarkNoteDialogVisible) {
        AlertDialog(
            onDismissRequest = { isBookmarkNoteDialogVisible = false },
            title = { Text("Bookmark Note") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = bookmarkNoteText,
                    onValueChange = { bookmarkNoteText = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddBookmark(bookmarkNoteText)
                        isBookmarkNoteDialogVisible = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { isBookmarkNoteDialogVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { isBottomSheetVisible = false }
        ) {
            TabRow(selectedTabIndex = selectedSheetTab) {
                SHEET_TABS.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSheetTab == index,
                        onClick = { selectedSheetTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (selectedSheetTab == 0) {
                TracksSheet(
                    tracks = uiState.tracks,
                    onTrackSelected = {
                        onTrackSelected(it)
                        isBottomSheetVisible = false
                    }
                )
            } else {
                BookmarksSheet(
                    bookmarks = uiState.bookmarks,
                    onBookmarkSelected = { trackId, positionMs ->
                        onBookmarkSelected(trackId, positionMs)
                        isBottomSheetVisible = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionChip(
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .widthIn(min = 72.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TracksSheet(
    tracks: List<BookTrackUi>,
    onTrackSelected: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tracks, key = { it.trackId }) { track ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTrackSelected(track.trackId) },
                colors = CardDefaults.cardColors(
                    containerColor = if (track.isPlaying) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                )
            ) {
                Text(
                    text = track.title,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BookmarksSheet(
    bookmarks: List<BookmarkUi>,
    onBookmarkSelected: (Long, Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(bookmarks, key = { it.id }) { bookmark ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBookmarkSelected(bookmark.trackId, bookmark.positionMs) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(TimeFormatters.formatClock(bookmark.positionMs), fontWeight = FontWeight.SemiBold)
                    Text(
                        bookmark.trackTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    bookmark.note?.takeIf { it.isNotBlank() }?.let { note ->
                        Text(note, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private val SPEED_OPTIONS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
