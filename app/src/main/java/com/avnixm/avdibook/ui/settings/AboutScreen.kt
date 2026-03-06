package com.avnixm.avdibook.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class FaqItem(val question: String, val answer: String)

private val FAQ_ITEMS = listOf(
    FaqItem(
        "How do I import audiobooks?",
        "Tap \"Add Books\" on the library screen or go to Settings → Import Management. You can import an entire folder (recommended for large libraries) or select individual files."
    ),
    FaqItem(
        "Why does the app need file permissions?",
        "Android requires explicit permission to read files. AvdiBook only reads files you choose — we never upload or share your audio files."
    ),
    FaqItem(
        "What are M4B chapters?",
        "M4B is an audiobook format that can embed chapter markers inside the file. AvdiBook automatically reads these so you can jump between chapters in the Browse sheet."
    ),
    FaqItem(
        "How do I move books to a new device?",
        "Use Backup & Restore in Settings to export your progress, then restore it on the new device. Transfer your audio files separately (e.g. via cloud storage), then relink any missing sources."
    ),
    FaqItem(
        "What does auto-rewind do?",
        "If you pause for longer than a set threshold, AvdiBook automatically rewinds a few seconds when you resume — so you never lose your place in a sentence."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("About & Help") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // App header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "AvdiBook",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Version 1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // FAQ section
            Text(
                "Frequently Asked Questions",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            FAQ_ITEMS.forEach { item ->
                FaqExpandableItem(item = item)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Privacy",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Text(
                    "AvdiBook is fully offline. Your audio files, progress, and bookmarks never leave your device. No accounts, no analytics, no tracking.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FaqExpandableItem(item: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        ListItem(
            headlineContent = {
                Text(item.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            },
            trailingContent = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
        )
        AnimatedVisibility(visible = expanded) {
            Text(
                text = item.answer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            )
        }
    }
}
