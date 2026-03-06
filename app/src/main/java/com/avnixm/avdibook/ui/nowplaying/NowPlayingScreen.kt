package com.avnixm.avdibook.ui.nowplaying

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avnixm.avdibook.AvdiBookApplication
import com.avnixm.avdibook.ui.book.BookmarkUi
import com.avnixm.avdibook.ui.book.ChapterUi
import com.avnixm.avdibook.ui.common.EmptyState
import com.avnixm.avdibook.ui.common.TimeFormatters
import kotlinx.coroutines.flow.collectLatest

private val SHEET_TABS = listOf("Chapters", "Bookmarks")
private val SPEED_OPTIONS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkChip(
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.BookmarkBorder,
                contentDescription = "Add bookmark (hold for note)",
                modifier = Modifier.size(16.dp)
            )
            Text("Bookmark", style = MaterialTheme.typography.labelMedium)
        }
    }
}

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
        onChapterSelected = viewModel::onChapterSelected,
        onBookmarkSelected = viewModel::onBookmarkSelected
    )
}

@Composable
fun NowPlayingEmptyScreen(
    onGoToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        EmptyState(
            icon = Icons.Default.Headphones,
            title = "Nothing playing yet",
            description = "Open a book from your library and press Play to start listening.",
            primaryActionLabel = "Go to Library",
            onPrimaryAction = onGoToLibrary
        )
    }
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
    onChapterSelected: (Long) -> Unit,
    onBookmarkSelected: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showBookmarkSheet by remember { mutableStateOf(false) }
    var showBrowseSheet by remember { mutableStateOf(false) }
    var bookmarkNoteText by remember { mutableStateOf("") }
    var selectedSheetTab by remember { mutableIntStateOf(0) }

    val maxDuration = uiState.durationMs.coerceAtLeast(1L)
    var sliderValue by remember(uiState.positionMs, uiState.durationMs) {
        mutableFloatStateOf(uiState.positionMs.coerceIn(0L, maxDuration).toFloat())
    }
    var isDragging by remember { mutableStateOf(false) }

    val speedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sleepSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bookmarkSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val browseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize player"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showBrowseSheet = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Browse chapters and bookmarks")
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
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cover art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
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

            // Book + track info
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = uiState.bookTitle.ifBlank { "Loading…" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = uiState.trackTitle.ifBlank { " " },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Seek bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (isDragging) sliderValue else uiState.positionMs
                        .coerceIn(0L, maxDuration).toFloat(),
                    onValueChange = {
                        isDragging = true
                        sliderValue = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        onSeekTo(sliderValue.toLong())
                    },
                    valueRange = 0f..maxDuration.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                "Seek bar: ${TimeFormatters.formatClock(uiState.positionMs)} of ${TimeFormatters.formatClock(uiState.durationMs)}"
                        }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        TimeFormatters.formatClock(if (isDragging) sliderValue.toLong() else uiState.positionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        TimeFormatters.formatClock(uiState.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Book progress row
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { uiState.bookProgressPercent.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (uiState.isBookProgressEstimated) "Calculating progress…"
                    else "Book: ${(uiState.bookProgressPercent * 100).toInt()}% · ${TimeFormatters.formatHoursMinutes(uiState.timeLeftMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { showSpeedSheet = true },
                    label = { Text("${uiState.speed}×") },
                    leadingIcon = {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
                AssistChip(
                    onClick = { showSleepSheet = true },
                    label = { Text(uiState.sleepLabel) },
                    leadingIcon = {
                        Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
                BookmarkChip(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAddBookmark(null)
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        bookmarkNoteText = ""
                        showBookmarkSheet = true
                    }
                )
                if (uiState.chapters.isNotEmpty()) {
                    AssistChip(
                        onClick = { showBrowseSheet = true },
                        label = { Text("Chapters") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Playback controls
            PlayerControlsCluster(
                isPlaying = uiState.isPlaying,
                skipBackSec = uiState.skipBackSec,
                skipForwardSec = uiState.skipForwardSec,
                onPlayPause = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPlayPause()
                },
                onSeekBack = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSeekBack()
                },
                onSeekForward = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSeekForward()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Speed picker sheet
    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            sheetState = speedSheetState
        ) {
            SpeedPickerSheetContent(
                currentSpeed = uiState.speed,
                onSpeedSelected = { speed ->
                    onSetSpeed(speed)
                    showSpeedSheet = false
                }
            )
        }
    }

    // Sleep timer sheet
    if (showSleepSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSleepSheet = false },
            sheetState = sleepSheetState
        ) {
            SleepTimerSheetContent(
                currentLabel = uiState.sleepLabel,
                onSetDuration = { minutes ->
                    onSetSleepDuration(minutes)
                    showSleepSheet = false
                },
                onSetEndOfTrack = {
                    onSetSleepEndOfTrack()
                    showSleepSheet = false
                },
                onClear = {
                    onClearSleepTimer()
                    showSleepSheet = false
                }
            )
        }
    }

    // Bookmark with note sheet
    if (showBookmarkSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookmarkSheet = false },
            sheetState = bookmarkSheetState
        ) {
            BookmarkNoteSheetContent(
                positionLabel = TimeFormatters.formatClock(uiState.positionMs),
                trackTitle = uiState.trackTitle,
                noteText = bookmarkNoteText,
                onNoteChange = { bookmarkNoteText = it },
                onSave = {
                    onAddBookmark(bookmarkNoteText.takeIf { it.isNotBlank() })
                    showBookmarkSheet = false
                },
                onCancel = { showBookmarkSheet = false }
            )
        }
    }

    // Chapters/Bookmarks browse sheet
    if (showBrowseSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBrowseSheet = false },
            sheetState = browseSheetState
        ) {
            BrowseSheetContent(
                selectedTab = selectedSheetTab,
                onTabSelected = { selectedSheetTab = it },
                chapters = uiState.chapters,
                bookmarks = uiState.bookmarks,
                onChapterSelected = { chapterId ->
                    onChapterSelected(chapterId)
                    showBrowseSheet = false
                },
                onBookmarkSelected = { trackId, positionMs ->
                    onBookmarkSelected(trackId, positionMs)
                    showBrowseSheet = false
                }
            )
        }
    }
}

@Composable
private fun PlayerControlsCluster(
    isPlaying: Boolean,
    skipBackSec: Int,
    skipForwardSec: Int,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onSeekBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Skip back $skipBackSec seconds",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "${skipBackSec}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onSeekForward,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward30,
                        contentDescription = "Skip forward $skipForwardSec seconds",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "${skipForwardSec}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpeedPickerSheetContent(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Playback Speed",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Saved for this book",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        SPEED_OPTIONS.forEach { speed ->
            ListItem(
                headlineContent = { Text("${speed}×") },
                trailingContent = {
                    if (speed == currentSpeed) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Selected")
                    }
                },
                modifier = Modifier.clickable { onSpeedSelected(speed) }
            )
        }
    }
}

@Composable
private fun SleepTimerSheetContent(
    currentLabel: String,
    onSetDuration: (Int) -> Unit,
    onSetEndOfTrack: () -> Unit,
    onClear: () -> Unit
) {
    val isOff = currentLabel == "Off"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Sleep Timer",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val options = listOf(
            Pair("Off", null as Int?),
            Pair("5 minutes", 5),
            Pair("10 minutes", 10),
            Pair("15 minutes", 15),
            Pair("30 minutes", 30),
            Pair("45 minutes", 45),
            Pair("60 minutes", 60),
        )

        options.forEach { (label, minutes) ->
            val isSelected = if (minutes == null) isOff else !isOff && currentLabel == "${minutes}:00" || currentLabel == label
            ListItem(
                headlineContent = { Text(label) },
                trailingContent = {
                    if (minutes == null && isOff || !isOff && minutes != null && currentLabel.startsWith("$minutes:") || currentLabel == label) {
                        RadioButton(selected = true, onClick = null)
                    }
                },
                modifier = Modifier.clickable {
                    if (minutes == null) onClear() else onSetDuration(minutes)
                }
            )
        }

        ListItem(
            headlineContent = { Text("End of this track") },
            modifier = Modifier.clickable { onSetEndOfTrack() }
        )
    }
}

@Composable
private fun BookmarkNoteSheetContent(
    positionLabel: String,
    trackTitle: String,
    noteText: String,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Add Bookmark",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "$trackTitle · $positionLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = noteText,
            onValueChange = onNoteChange,
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Save Bookmark")
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

@Composable
private fun BrowseSheetContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    chapters: List<ChapterUi>,
    bookmarks: List<BookmarkUi>,
    onChapterSelected: (Long) -> Unit,
    onBookmarkSelected: (Long, Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = selectedTab) {
            SHEET_TABS.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title) }
                )
            }
        }

        if (selectedTab == 0) {
            if (chapters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No chapter data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(chapters, key = { it.id }) { chapter ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChapterSelected(chapter.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (chapter.isCurrent) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (chapter.isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${TimeFormatters.formatClock(chapter.startMs)}${chapter.endMs?.let { " – ${TimeFormatters.formatClock(it)}" } ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (chapter.isCurrent) {
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
        } else {
            if (bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No bookmarks yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(bookmarks, key = { it.id }) { bookmark ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBookmarkSelected(bookmark.trackId, bookmark.positionMs) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    TimeFormatters.formatClock(bookmark.positionMs),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    bookmark.trackTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                bookmark.note?.takeIf { it.isNotBlank() }?.let { note ->
                                    Text(
                                        note,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
