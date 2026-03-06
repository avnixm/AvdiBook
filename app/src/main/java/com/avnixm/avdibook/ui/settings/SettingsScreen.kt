package com.avnixm.avdibook.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avnixm.avdibook.BuildConfig
import com.avnixm.avdibook.data.model.ListeningSettings
import com.avnixm.avdibook.data.prefs.AppPreferences
import com.avnixm.avdibook.ui.design.AppHeroCard
import com.avnixm.avdibook.ui.design.AppSectionCard
import com.avnixm.avdibook.ui.design.AppSpacing
import com.avnixm.avdibook.ui.design.AppWindowSize
import com.avnixm.avdibook.ui.design.rememberAppWindowSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    themeMode: AppPreferences.ThemeMode,
    dynamicColorEnabled: Boolean,
    pureBlackDarkEnabled: Boolean,
    listeningDefaults: ListeningSettings,
    textScalePreset: AppPreferences.TextScalePreset,
    reducedMotionEnabled: Boolean,
    onThemeModeChanged: (AppPreferences.ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onPureBlackChanged: (Boolean) -> Unit,
    onDefaultSpeedChanged: (Float) -> Unit,
    onDefaultSkipBackChanged: (Int) -> Unit,
    onDefaultSkipForwardChanged: (Int) -> Unit,
    onDefaultAutoRewindChanged: (Int) -> Unit,
    onDefaultAutoRewindThresholdChanged: (Int) -> Unit,
    onDefaultUseLoudnessBoostChanged: (Boolean) -> Unit,
    onTextScalePresetChanged: (AppPreferences.TextScalePreset) -> Unit,
    onReducedMotionChanged: (Boolean) -> Unit,
    onOpenImportManagement: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenAboutHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val windowSize = rememberAppWindowSize()
    val horizontalPadding = when (windowSize) {
        AppWindowSize.COMPACT -> 16.dp
        AppWindowSize.MEDIUM -> 24.dp
        AppWindowSize.EXPANDED -> 40.dp
    }

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
                .padding(horizontal = horizontalPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            SettingsHeroCard()

            SettingsCard(title = "Appearance", icon = Icons.Default.Palette) {
                LabelValueRow(
                    label = "Theme",
                    value = when (themeMode) {
                        AppPreferences.ThemeMode.SYSTEM -> "System"
                        AppPreferences.ThemeMode.LIGHT -> "Light"
                        AppPreferences.ThemeMode.DARK -> "Dark"
                    }
                )
                ChipRow {
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                ToggleRow(
                    title = "Dynamic color",
                    subtitle = "Use wallpaper-inspired accents on Android 12+.",
                    checked = dynamicColorEnabled,
                    onCheckedChange = onDynamicColorChanged
                )
                ToggleRow(
                    title = "Pure black dark mode",
                    subtitle = "Deepen dark surfaces for OLED displays.",
                    checked = pureBlackDarkEnabled,
                    onCheckedChange = onPureBlackChanged
                )
            }

            SettingsCard(title = "Listening Defaults", icon = Icons.Default.Tune) {
                Text(
                    "These apply to every book unless you customize that book separately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsChoiceBlock(
                    title = "Playback speed",
                    selectedLabel = "${listeningDefaults.playbackSpeed}x"
                ) {
                    listOf(0.8f, 1.0f, 1.2f, 1.5f, 1.8f, 2.0f).forEach { value ->
                        FilterChip(
                            selected = listeningDefaults.playbackSpeed == value,
                            onClick = { onDefaultSpeedChanged(value) },
                            label = { Text("${value}x") }
                        )
                    }
                }
                SettingsChoiceBlock(
                    title = "Skip back",
                    selectedLabel = "${listeningDefaults.skipBackSec}s"
                ) {
                    listOf(5, 10, 15, 30).forEach { value ->
                        FilterChip(
                            selected = listeningDefaults.skipBackSec == value,
                            onClick = { onDefaultSkipBackChanged(value) },
                            label = { Text("${value}s") }
                        )
                    }
                }
                SettingsChoiceBlock(
                    title = "Skip forward",
                    selectedLabel = "${listeningDefaults.skipForwardSec}s"
                ) {
                    listOf(15, 30, 45, 60).forEach { value ->
                        FilterChip(
                            selected = listeningDefaults.skipForwardSec == value,
                            onClick = { onDefaultSkipForwardChanged(value) },
                            label = { Text("${value}s") }
                        )
                    }
                }
                SettingsChoiceBlock(
                    title = "Auto-rewind",
                    selectedLabel = if (listeningDefaults.autoRewindSec == 0) "Off" else "${listeningDefaults.autoRewindSec}s"
                ) {
                    listOf(0, 5, 10, 15, 30).forEach { value ->
                        FilterChip(
                            selected = listeningDefaults.autoRewindSec == value,
                            onClick = { onDefaultAutoRewindChanged(value) },
                            label = { Text(if (value == 0) "Off" else "${value}s") }
                        )
                    }
                }
                SettingsChoiceBlock(
                    title = "Pause threshold",
                    selectedLabel = "${listeningDefaults.autoRewindAfterPauseSec / 60} min"
                ) {
                    listOf(60, 180, 300, 600).forEach { value ->
                        FilterChip(
                            selected = listeningDefaults.autoRewindAfterPauseSec == value,
                            onClick = { onDefaultAutoRewindThresholdChanged(value) },
                            label = { Text("${value / 60} min") }
                        )
                    }
                }
                ToggleRow(
                    title = "Loudness boost",
                    subtitle = "Keep quieter recordings easier to hear.",
                    checked = listeningDefaults.useLoudnessBoost,
                    onCheckedChange = onDefaultUseLoudnessBoostChanged
                )
            }

            SettingsCard(title = "Accessibility", icon = Icons.Default.AccessibilityNew) {
                LabelValueRow(
                    label = "Reading size",
                    value = when (textScalePreset) {
                        AppPreferences.TextScalePreset.STANDARD -> "Standard"
                        AppPreferences.TextScalePreset.LARGE -> "Large"
                        AppPreferences.TextScalePreset.LARGEST -> "Largest"
                    }
                )
                ChipRow {
                    listOf(
                        AppPreferences.TextScalePreset.STANDARD to "Standard",
                        AppPreferences.TextScalePreset.LARGE to "Large",
                        AppPreferences.TextScalePreset.LARGEST to "Largest"
                    ).forEach { (preset, label) ->
                        FilterChip(
                            selected = textScalePreset == preset,
                            onClick = { onTextScalePresetChanged(preset) },
                            label = { Text(label) }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                ToggleRow(
                    title = "Reduce motion",
                    subtitle = "Replace larger transitions with calmer screen changes.",
                    checked = reducedMotionEnabled,
                    onCheckedChange = onReducedMotionChanged
                )
            }

            SettingsCard(title = "Library & Data", icon = Icons.Default.FolderOpen) {
                NavigationRow(
                    title = "Import management",
                    subtitle = "Add sources, rescan, and resolve missing files.",
                    onClick = onOpenImportManagement
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                NavigationRow(
                    title = "Backup & restore",
                    subtitle = "Move your progress, bookmarks, and settings safely.",
                    onClick = onOpenBackupRestore
                )
            }

            SettingsCard(title = "About", icon = Icons.Default.Info) {
                NavigationRow(
                    title = "About & help",
                    subtitle = "FAQ, privacy notes, and guidance.",
                    onClick = onOpenAboutHelp
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                ListItem(
                    headlineContent = { Text("Version") },
                    supportingContent = { Text("Current installed build") },
                    trailingContent = {
                        Text(
                            BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsHeroCard() {
    AppHeroCard(
        title = "Global listening, tailored accessibility, and polished visuals",
        subtitle = "These settings are app-wide defaults. Books can optionally use advanced per-book overrides."
    )
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    AppSectionCard(
        title = title,
        icon = icon,
        content = content
    )
}

@Composable
private fun SettingsChoiceBlock(
    title: String,
    selectedLabel: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabelValueRow(label = title, value = selectedLabel)
        ChipRow(content = content)
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
private fun NavigationRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
