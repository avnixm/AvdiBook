package com.avnixm.avdibook.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avnixm.avdibook.data.prefs.AppPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    themeMode: AppPreferences.ThemeMode,
    dynamicColorEnabled: Boolean,
    pureBlackDarkEnabled: Boolean,
    onThemeModeChanged: (AppPreferences.ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onPureBlackChanged: (Boolean) -> Unit,
    onOpenImportManagement: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenAboutHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSectionHeader(icon = Icons.Default.Palette, title = "Appearance")

            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(
                            AppPreferences.ThemeMode.SYSTEM to "System",
                            AppPreferences.ThemeMode.LIGHT to "Light",
                            AppPreferences.ThemeMode.DARK to "Dark"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChanged(mode) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent = { Text("Dynamic Color") },
                supportingContent = {
                    Text(
                        "Follows your wallpaper (Android 12+)",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                trailingContent = {
                    Switch(checked = dynamicColorEnabled, onCheckedChange = onDynamicColorChanged)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent = { Text("Pure Black Dark Mode") },
                supportingContent = {
                    Text("OLED-optimized background", style = MaterialTheme.typography.bodySmall)
                },
                trailingContent = {
                    Switch(checked = pureBlackDarkEnabled, onCheckedChange = onPureBlackChanged)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionHeader(icon = Icons.Default.FolderOpen, title = "Library")

            ListItem(
                headlineContent = { Text("Import Management") },
                supportingContent = { Text("Add sources, rescan, fix missing files") },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable(onClick = onOpenImportManagement)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent = { Text("Backup & Restore") },
                supportingContent = { Text("Export or import your library data") },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable(onClick = onOpenBackupRestore)
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionHeader(icon = Icons.Default.Info, title = "About")

            ListItem(
                headlineContent = { Text("About & Help") },
                supportingContent = { Text("FAQ, privacy, version info") },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable(onClick = onOpenAboutHelp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent = { Text("Version") },
                trailingContent = {
                    Text(
                        "1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
