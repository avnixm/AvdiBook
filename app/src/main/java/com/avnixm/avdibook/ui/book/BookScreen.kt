package com.avnixm.avdibook.ui.book

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avnixm.avdibook.AvdiBookApplication
import com.avnixm.avdibook.ui.common.EmptyState
import com.avnixm.avdibook.ui.common.TimeFormatters
import kotlinx.coroutines.flow.collectLatest

private val BOOK_TABS = listOf("Tracks", "Bookmarks", "Settings")

@Composable
fun BookRoute(
    bookId: Long,
    autoPlay: Boolean = false,
    onBack: () -> Unit,
    onNavigateToNowPlaying: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as AvdiBookApplication
    val viewModel: BookViewModel = viewModel(
        key = "book_${bookId}_${autoPlay}",
        factory = BookViewModel.factory(
            application = application,
            appContainer = application.appContainer,
            bookId = bookId,
            autoPlay = autoPlay
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
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
        ) {
            // Hero header section
            BookHeroHeader(
                uiState = uiState,
                onPlayResume = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPlayResume()
                }
            )

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
                0 -> TracksTab(uiState = uiState, onTrackClick = onTrackClick)

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
        AlertDialog(
            onDismissRequest = { isAddNoteDialogVisible = false },
            title = { Text("Add Bookmark") },
            text = {
                OutlinedTextField(
                    value = bookmarkNote,
                    onValueChange = { bookmarkNote = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isAddNoteDialogVisible = false
                        onAddBookmark(bookmarkNote.takeIf { it.isNotBlank() })
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { isAddNoteDialogVisible = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BookHeroHeader(
    uiState: BookUiState,
    onPlayResume: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover art
            Box(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
                AsyncImage(
                    model = uiState.coverArtPath?.let { java.io.File(it) },
                    contentDescription = "Cover art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.title.ifBlank { "Loading…" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${uiState.tracks.size} track${if (uiState.tracks.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { uiState.bookProgressPercent.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription =
                        "${(uiState.bookProgressPercent * 100).toInt()}% complete"
                }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${(uiState.bookProgressPercent * 100).toInt()}% complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!uiState.isProgressEstimated) {
                Text(
                    text = TimeFormatters.formatHoursMinutes(uiState.timeLeftMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isMissingSource) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Source missing. Tap to relink from book settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = onPlayResume,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isMissingSource
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(if (uiState.hasPlaybackState) "Continue" else "Play from Beginning")
        }
    }

    HorizontalDivider()
}

@Composable
private fun TracksTab(
    uiState: BookUiState,
    onTrackClick: (Long) -> Unit
) {
    if (uiState.tracks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Default.Book,
                title = "No tracks",
                description = "No playable audio tracks were found for this book."
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp, vertical = 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (uiState.chapters.isNotEmpty()) {
            item {
                Text(
                    "Chapters",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(uiState.chapters.take(4), key = { "ch_${it.id}" }) { chapter ->
                Text(
                    text = "${if (chapter.isCurrent) "▶ " else ""}${chapter.title} · ${TimeFormatters.formatClock(chapter.startMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (chapter.isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        }

        item {
            Text(
                "Tracks",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(uiState.tracks, key = { it.trackId }) { track ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTrackClick(track.trackId) },
                colors = CardDefaults.cardColors(
                    containerColor = if (track.isPlaying) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (track.isPlaying) FontWeight.SemiBold else FontWeight.Normal,
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
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text("Playing", style = MaterialTheme.typography.labelSmall) }
                        )
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
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(onClick = onAddBookmark) {
                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Add Bookmark")
            }
            OutlinedButton(onClick = onAddBookmarkWithNote) {
                Text("Add with Note")
            }
        }

        HorizontalDivider()

        if (bookmarks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.BookmarkBorder,
                    title = "No bookmarks yet",
                    description = "Tap \"Add Bookmark\" while listening to save your place.",
                    primaryActionLabel = "Add Bookmark",
                    onPrimaryAction = onAddBookmark
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp, vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bookmarks, key = { it.id }) { bookmark ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart ||
                            value == SwipeToDismissBoxValue.StartToEnd
                        ) {
                            onDeleteBookmark(bookmark.id)
                            true
                        } else false
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
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = TimeFormatters.formatClock(bookmark.positionMs),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = bookmark.trackTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
    ) {
        item {
            SettingSection(title = "Playback Speed", note = "Saved for this book") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                        FilterChip(
                            selected = current.playbackSpeed == speed,
                            onClick = { onSpeedSelected(speed) },
                            label = { Text("${speed}×") }
                        )
                    }
                }
            }
        }

        item {
            SettingSection(title = "Skip Back") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30).forEach { sec ->
                        FilterChip(
                            selected = current.skipBackSec == sec,
                            onClick = { onSkipBackSelected(sec) },
                            label = { Text("${sec}s") }
                        )
                    }
                }
            }
        }

        item {
            SettingSection(title = "Skip Forward") {
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
        }

        item {
            SettingSection(
                title = "Auto-rewind after pause",
                note = "Rewinds automatically if you paused for longer than the threshold below"
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 5, 10, 15, 30).forEach { sec ->
                        FilterChip(
                            selected = current.autoRewindSec == sec,
                            onClick = { onAutoRewindSelected(sec) },
                            label = { Text(if (sec == 0) "Off" else "${sec}s") }
                        )
                    }
                }
            }
        }

        item {
            SettingSection(title = "Pause threshold") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60, 180, 300, 600).forEach { sec ->
                        FilterChip(
                            selected = current.autoRewindAfterPauseSec == sec,
                            onClick = { onAutoRewindThresholdSelected(sec) },
                            label = { Text("${sec / 60} min") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    note: String? = null,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        note?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
    }
}
