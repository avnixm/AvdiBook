package com.avnixm.avdibook.ui.onboarding

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avnixm.avdibook.AvdiBookApplication
import com.avnixm.avdibook.ui.library.LibraryEvent
import com.avnixm.avdibook.ui.library.LibraryViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportMethodRoute(
    onCompleted: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as AvdiBookApplication
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.factory(application, application.appContainer)
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPrivacySheet by remember { mutableStateOf(false) }
    val privacySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val folderImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val treeUri = data.data ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        viewModel.importFolder(treeUri.toString())
    }

    val filesImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val uris = mutableListOf<Uri>()
        data.clipData?.let { clip ->
            for (index in 0 until clip.itemCount) {
                clip.getItemAt(index).uri?.let(uris::add)
            }
        }
        data.data?.let(uris::add)
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uris.distinct().forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        viewModel.importFiles(uris.map(Uri::toString))
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LibraryEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                    if (event.message.contains("imported", ignoreCase = true)) {
                        onCompleted()
                    }
                }
                is LibraryEvent.NavigateToBook -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Your Books") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "How would you like to import?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "You can always add more sources later from Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Privacy info pill
            Surface(
                onClick = { showPrivacySheet = true },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "How file access works",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Import Folder card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Column {
                            Text("Import a Folder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Best for organized libraries. Persistent access, automatic scanning.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (uiState.isImporting) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    Button(
                        onClick = {
                            folderImportLauncher.launch(
                                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isImporting
                    ) {
                        Text("Choose Folder")
                    }
                }
            }

            // Import Files card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Column {
                            Text("Import Individual Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Pick specific audiobook files one at a time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
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
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isImporting
                    ) {
                        Text("Choose Files")
                    }
                }
            }

            TextButton(
                onClick = onCompleted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now →")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showPrivacySheet) {
        ModalBottomSheet(
            onDismissRequest = { showPrivacySheet = false },
            sheetState = privacySheetState
        ) {
            PermissionEducationSheetContent(
                onDismiss = { showPrivacySheet = false }
            )
        }
    }
}

@Composable
fun PermissionEducationSheetContent(
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "File Access & Your Privacy",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        listOf(
            Pair(Icons.Default.Folder, "Your files never leave your device — no cloud uploads, ever."),
            Pair(Icons.Default.Info, "We only read files you explicitly choose through the Android file picker."),
            Pair(Icons.Default.Folder, "Android handles the permission dialog — AvdiBook never sees your other files.")
        ).forEach { (icon, text) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Got it")
        }
    }
}
