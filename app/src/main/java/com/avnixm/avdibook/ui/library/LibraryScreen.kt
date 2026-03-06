package com.avnixm.avdibook.ui.library

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.avnixm.avdibook.AvdiBookApplication
import com.avnixm.avdibook.ui.common.EmptyState
import com.avnixm.avdibook.ui.common.TimeFormatters
import com.avnixm.avdibook.ui.design.AppWindowSize
import com.avnixm.avdibook.ui.design.ArtworkTile
import com.avnixm.avdibook.ui.design.rememberAppWindowSize
import kotlinx.coroutines.flow.collectLatest

private enum class SortMode(val label: String) {
    RECENT("Recent"),
    TITLE("Title A–Z"),
    DURATION("Duration")
}

private enum class LibraryFilter(val label: String) {
    ALL("All"),
    IN_PROGRESS("In Progress"),
    FINISHED("Finished"),
    NEEDS_RELINK("Needs Relink")
}

@Composable
fun LibraryRoute(
    onNavigateToBook: (Long) -> Unit,
    onContinueBook: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as AvdiBookApplication
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.factory(application, application.appContainer)
    )

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val folderImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val treeUri = data.data ?: return@rememberLauncherForActivityResult
        persistReadPermission(context.contentResolver, treeUri, data.flags)
        viewModel.importFolder(treeUri.toString())
    }

    val filesImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val uris = extractUrisFromResult(data)
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uris.forEach { uri -> persistReadPermission(context.contentResolver, uri, data.flags) }
        viewModel.importFiles(uris.map(Uri::toString))
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LibraryEvent.NavigateToBook -> onNavigateToBook(event.bookId)
                is LibraryEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LibraryScreen(
        modifier = modifier,
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onOpenSearch = onOpenSearch,
        onImportFolder = {
            folderImportLauncher.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
            )
        },
        onImportFiles = {
            filesImportLauncher.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/mp4"))
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
            )
        },
        onSkipImport = viewModel::onSkipImportForNow,
        onBookClick = viewModel::onBookSelected,
        onContinueClick = onContinueBook
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    snackbarHostState: SnackbarHostState,
    onOpenSearch: () -> Unit,
    onImportFolder: () -> Unit,
    onImportFiles: () -> Unit,
    onSkipImport: () -> Unit,
    onBookClick: (Long) -> Unit,
    onContinueClick: (Long) -> Unit = onBookClick,
    modifier: Modifier = Modifier
) {
    val windowSize = rememberAppWindowSize()
    var isGridMode by rememberSaveable(windowSize) {
        mutableStateOf(windowSize != AppWindowSize.COMPACT)
    }
    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var showImportSheet by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.RECENT) }
    var activeFilter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }

    val filteredAndSortedBooks = remember(uiState.books, sortMode, activeFilter) {
        val filtered = when (activeFilter) {
            LibraryFilter.ALL -> uiState.books
            LibraryFilter.IN_PROGRESS -> uiState.books.filter { it.progressPercent > 0f && it.progressPercent < 1f }
            LibraryFilter.FINISHED -> uiState.books.filter { it.progressPercent >= 1f }
            LibraryFilter.NEEDS_RELINK -> uiState.books.filter { it.isMissingSource }
        }
        when (sortMode) {
            SortMode.RECENT -> filtered.sortedWith(
                compareByDescending<BookWithProgressUi> { it.lastPlayedAt ?: it.bookId }
                    .thenBy { it.title.lowercase() }
            )
            SortMode.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortMode.DURATION -> filtered.sortedByDescending { it.timeLeftMs }
        }
    }

    val sortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val importSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Library", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { isGridMode = !isGridMode }) {
                        Icon(
                            imageVector = if (isGridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridMode) "Switch to list view" else "Switch to grid view"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search library")
                    }
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Sort and filter")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.showImportOnboarding) {
                ExtendedFloatingActionButton(
                    onClick = { showImportSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Books") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.showImportOnboarding) {
            OnboardingEmptyContent(
                isImporting = uiState.isImporting,
                onImportFolder = onImportFolder,
                onImportFiles = onImportFiles,
                onSkipImport = onSkipImport,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            )
        } else {
            LibraryContent(
                books = filteredAndSortedBooks,
                allBooksEmpty = uiState.books.isEmpty(),
                isGridMode = isGridMode,
                windowSize = windowSize,
                activeFilter = activeFilter,
                onFilterSelected = { activeFilter = it },
                onBookClick = onBookClick,
                onContinueClick = onContinueClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = sortSheetState
        ) {
            SortFilterSheetContent(
                sortMode = sortMode,
                activeFilter = activeFilter,
                onSortSelected = { sortMode = it },
                onFilterSelected = { activeFilter = it },
                onDismiss = { showSortSheet = false }
            )
        }
    }

    if (showImportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImportSheet = false },
            sheetState = importSheetState
        ) {
            ImportMethodSheetContent(
                onImportFolder = {
                    showImportSheet = false
                    onImportFolder()
                },
                onImportFiles = {
                    showImportSheet = false
                    onImportFiles()
                }
            )
        }
    }
}

@Composable
private fun LibraryContent(
    books: List<BookWithProgressUi>,
    allBooksEmpty: Boolean,
    isGridMode: Boolean,
    windowSize: AppWindowSize,
    activeFilter: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit,
    onBookClick: (Long) -> Unit,
    onContinueClick: (Long) -> Unit = onBookClick,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryFilter.entries.forEach { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) }
                )
            }
        }

        if (allBooksEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.Book,
                    title = "No books yet",
                    description = "Import a folder or individual audiobook files to get started."
                )
            }
        } else if (books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.FilterList,
                    title = "No matches",
                    description = "No books match the current filter. Try selecting a different category.",
                    primaryActionLabel = "Show all",
                    onPrimaryAction = { onFilterSelected(LibraryFilter.ALL) }
                )
            }
        } else if (isGridMode) {
            val columns = when (windowSize) {
                AppWindowSize.COMPACT -> 2
                AppWindowSize.MEDIUM -> 3
                AppWindowSize.EXPANDED -> 4
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(books, key = { it.bookId }) { book ->
                    BookCard(book = book, onBookClick = onBookClick, onContinueClick = onContinueClick)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(books, key = { it.bookId }) { book ->
                    BookRow(book = book, onBookClick = onBookClick, onContinueClick = onContinueClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCard(
    book: BookWithProgressUi,
    onBookClick: (Long) -> Unit,
    onContinueClick: (Long) -> Unit = onBookClick,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { onBookClick(book.bookId) }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentAlignment = Alignment.Center
            ) {
                ArtworkTile(
                    imagePath = book.coverArtPath,
                    modifier = Modifier.fillMaxSize()
                )
                // Progress bar overlay at bottom of cover
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    LinearProgressIndicator(
                        progress = { book.progressPercent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription =
                                    "${(book.progressPercent * 100).toInt()}% complete"
                            }
                    )
                }
            }

            // Content area below cover
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(book.progressPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!book.isProgressEstimated) {
                        Text(
                            text = TimeFormatters.formatHoursMinutes(book.timeLeftMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (book.hasResume) {
                    AssistChip(
                        onClick = { onContinueClick(book.bookId) },
                        label = { Text("Continue", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
                if (book.isMissingSource) {
                    AssistChip(
                        onClick = { onBookClick(book.bookId) },
                        label = {
                            Text(
                                "Needs Relink",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookRow(
    book: BookWithProgressUi,
    onBookClick: (Long) -> Unit,
    onContinueClick: (Long) -> Unit = onBookClick,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { onBookClick(book.bookId) }),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArtworkTile(
                    imagePath = book.coverArtPath,
                    modifier = Modifier.size(56.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append("${(book.progressPercent * 100).toInt()}%")
                            if (!book.isProgressEstimated) {
                                append(" · ")
                                append(TimeFormatters.formatHoursMinutes(book.timeLeftMs))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (book.isMissingSource) {
                        Text(
                            text = "Needs relink",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (book.hasResume) {
                    AssistChip(
                        onClick = { onContinueClick(book.bookId) },
                        label = { Text("Continue", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            LinearProgressIndicator(
                progress = { book.progressPercent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription =
                            "${book.title}: ${(book.progressPercent * 100).toInt()}% complete"
                    }
            )
        }
    }
}

@Composable
private fun OnboardingEmptyContent(
    isImporting: Boolean,
    onImportFolder: () -> Unit,
    onImportFiles: () -> Unit,
    onSkipImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyState(
            icon = Icons.Default.Book,
            title = "Welcome to your library",
            description = "Import a folder of audiobooks or select individual files to get started.",
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onImportFolder,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isImporting
        ) {
            Text(if (isImporting) "Importing…" else "Import Folder")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onImportFiles,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isImporting
        ) {
            Text("Import Files")
        }
        TextButton(
            onClick = onSkipImport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip for now")
        }
    }
}

@Composable
private fun SortFilterSheetContent(
    sortMode: SortMode,
    activeFilter: LibraryFilter,
    onSortSelected: (SortMode) -> Unit,
    onFilterSelected: (LibraryFilter) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Sort & Filter",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            "Sort by",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            SortMode.entries.forEach { mode ->
                FilterChip(
                    selected = sortMode == mode,
                    onClick = { onSortSelected(mode) },
                    label = { Text(mode.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Show",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            LibraryFilter.entries.forEach { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply")
        }
    }
}

@Composable
private fun ImportMethodSheetContent(
    onImportFolder: () -> Unit,
    onImportFiles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Add Books",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Import Folder") },
            supportingContent = { Text("Grant access to a folder and scan all audio files") },
            modifier = Modifier.clickable(onClick = onImportFolder)
        )
        ListItem(
            headlineContent = { Text("Import Files") },
            supportingContent = { Text("Pick individual audio files") },
            modifier = Modifier.clickable(onClick = onImportFiles)
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun persistReadPermission(contentResolver: ContentResolver, uri: Uri, flags: Int) {
    val grantedFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    val flagsToPersist = if (grantedFlags == 0) Intent.FLAG_GRANT_READ_URI_PERMISSION else grantedFlags
    runCatching { contentResolver.takePersistableUriPermission(uri, flagsToPersist) }
}

private fun extractUrisFromResult(data: Intent): List<Uri> {
    val uris = mutableListOf<Uri>()
    data.clipData?.let { clipData ->
        for (index in 0 until clipData.itemCount) {
            clipData.getItemAt(index).uri?.let(uris::add)
        }
    }
    data.data?.let(uris::add)
    return uris.distinct()
}
