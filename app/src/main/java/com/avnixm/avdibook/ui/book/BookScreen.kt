package com.avnixm.avdibook.ui.book

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.avnixm.avdibook.ui.common.TimeFormatters
import kotlinx.coroutines.flow.collectLatest

private val BOOK_TABS = listOf("Tracks", "Bookmarks", "Settings")

@Composable
fun BookRoute(
    bookId: Long,
    onBack: () -> Unit,
    onNavigateToNowPlaying: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as AvdiBookApplication
    val viewModel: BookViewModel = viewModel(
        key = "book_$bookId",
        factory = BookViewModel.factory(
            application = application,
            appContainer = application.appContainer,
            bookId = bookId
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onNotificationPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is BookEvent.NavigateToNowPlaying -> onNavigateToNowPlaying(event.bookId)
                is BookEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                BookEvent.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onNotificationPermissionResult(isGranted = true)
                    }
                }
            }
        }
    }

    BookScreen(
        modifier = modifier,
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onPlayResume = viewModel::onPlayResumeClick,
        onTrackClick = viewModel::onTrackClick,
        onBookmarkClick = viewModel::onBookmarkClick,
        onAddBookmark = viewModel::onAddBookmark,
        onDeleteBookmark = viewModel::onDeleteBookmark,
        onSpeedSelected = viewModel::onSpeedSelected,
        onSkipBackSelected = viewModel::onSkipBackSelected,
        onSkipForwardSelected = viewModel::onSkipForwardSelected,
        onAutoRewindSelected = viewModel::onAutoRewindSelected,
        onAutoRewindThresholdSelected = viewModel::onAutoRewindThresholdSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(
    uiState: BookUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onPlayResume: () -> Unit,
    onTrackClick: (Long) -> Unit,
    onBookmarkClick: (Long, Long) -> Unit,
    onAddBookmark: (String?) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onSkipBackSelected: (Int) -> Unit,
    onSkipForwardSelected: (Int) -> Unit,
    onAutoRewindSelected: (Int) -> Unit,
    onAutoRewindThresholdSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isAddNoteDialogVisible by remember { mutableStateOf(false) }
    var bookmarkNote by remember { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = uiState.title.ifBlank { "Book" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                BOOK_TABS.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> TracksTab(
                    uiState = uiState,
                    onPlayResume = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPlayResume()
                    },
                    onTrackClick = onTrackClick
                )

                1 -> BookmarksTab(
                    bookmarks = uiState.bookmarks,
                    onBookmarkClick = onBookmarkClick,
                    onDeleteBookmark = onDeleteBookmark,
                    onAddBookmark = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAddBookmark(null)
                    },
                    onAddBookmarkWithNote = {
                        bookmarkNote = ""
                        isAddNoteDialogVisible = true
                    }
                )

                2 -> SettingsTab(
                    settings = uiState.settings,
                    onSpeedSelected = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpeedSelected(it)
                    },
                    onSkipBackSelected = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSkipBackSelected(it)
                    },
                    onSkipForwardSelected = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSkipForwardSelected(it)
                    },
                    onAutoRewindSelected = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAutoRewindSelected(it)
                    },
                    onAutoRewindThresholdSelected = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAutoRewindThresholdSelected(it)
                    }
                )
            }
        }
    }

    if (isAddNoteDialogVisible) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { isAddNoteDialogVisible = false },
            title = { Text("Add Bookmark Note") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = bookmarkNote,
                    onValueChange = { bookmarkNote = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isAddNoteDialogVisible = false
                        onAddBookmark(bookmarkNote)
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { isAddNoteDialogVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TracksTab(
    uiState: BookUiState,
    onPlayResume: () -> Unit,
    onTrackClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = onPlayResume,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.hasPlaybackState) "Play / Resume" else "Play")
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.tracks, key = { it.trackId }) { track ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackClick(track.trackId) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (track.isPlaying) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            track.durationMs?.let { durationMs ->
                                Text(
                                    text = TimeFormatters.formatClock(durationMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (track.isPlaying) {
                            Text(
                                text = "Playing",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksTab(
    bookmarks: List<BookmarkUi>,
    onBookmarkClick: (Long, Long) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onAddBookmark: () -> Unit,
    onAddBookmarkWithNote: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddBookmark) {
                Text("Add Bookmark")
            }
            OutlinedButton(onClick = onAddBookmarkWithNote) {
                Text("Add with note")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (bookmarks.isEmpty()) {
            Text(
                text = "No bookmarks yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(bookmarks, key = { it.id }) { bookmark ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (
                            value == SwipeToDismissBoxValue.EndToStart ||
                            value == SwipeToDismissBoxValue.StartToEnd
                        ) {
                            onDeleteBookmark(bookmark.id)
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {},
                    content = {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBookmarkClick(bookmark.trackId, bookmark.positionMs) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = TimeFormatters.formatClock(bookmark.positionMs),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = bookmark.trackTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                bookmark.note?.takeIf { it.isNotBlank() }?.let { note ->
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsTab(
    settings: BookSettingsUi?,
    onSpeedSelected: (Float) -> Unit,
    onSkipBackSelected: (Int) -> Unit,
    onSkipForwardSelected: (Int) -> Unit,
    onAutoRewindSelected: (Int) -> Unit,
    onAutoRewindThresholdSelected: (Int) -> Unit
) {
    val current = settings ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Speed", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                    FilterChip(
                        selected = current.playbackSpeed == speed,
                        onClick = { onSpeedSelected(speed) },
                        label = { Text("${speed}x") }
                    )
                }
            }
        }

        item {
            Text("Skip Seconds", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Back", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 30).forEach { sec ->
                    FilterChip(
                        selected = current.skipBackSec == sec,
                        onClick = { onSkipBackSelected(sec) },
                        label = { Text("${sec}s") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Forward", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60).forEach { sec ->
                    FilterChip(
                        selected = current.skipForwardSec == sec,
                        onClick = { onSkipForwardSelected(sec) },
                        label = { Text("${sec}s") }
                    )
                }
            }
        }

        item {
            Text("Auto-rewind", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Rewind by", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 30).forEach { sec ->
                    FilterChip(
                        selected = current.autoRewindSec == sec,
                        onClick = { onAutoRewindSelected(sec) },
                        label = { Text("${sec}s") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Apply if paused longer than", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 180, 300, 600).forEach { sec ->
                    val label = "${sec / 60} min"
                    FilterChip(
                        selected = current.autoRewindAfterPauseSec == sec,
                        onClick = { onAutoRewindThresholdSelected(sec) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}
