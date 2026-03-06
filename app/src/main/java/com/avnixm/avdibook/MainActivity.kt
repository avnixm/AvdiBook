package com.avnixm.avdibook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avnixm.avdibook.debug.DebugCrashReporter
import com.avnixm.avdibook.ui.navigation.AvdiBookApp
import com.avnixm.avdibook.ui.navigation.AppChromeViewModel
import com.avnixm.avdibook.ui.theme.AvdiBookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val pendingCrash = DebugCrashReporter.consumeLastCrash(this)

        setContent {
            val application = applicationContext as AvdiBookApplication
            val chromeViewModel: AppChromeViewModel = viewModel(
                factory = AppChromeViewModel.factory(
                    application = application,
                    appContainer = application.appContainer
                )
            )
            val chromeUiState = chromeViewModel.uiState.collectAsStateWithLifecycle().value
            val darkTheme = when (chromeUiState.themeMode) {
                com.avnixm.avdibook.data.prefs.AppPreferences.ThemeMode.SYSTEM -> isSystemInDarkTheme()
                com.avnixm.avdibook.data.prefs.AppPreferences.ThemeMode.LIGHT -> false
                com.avnixm.avdibook.data.prefs.AppPreferences.ThemeMode.DARK -> true
            }

            AvdiBookTheme(
                darkTheme = darkTheme,
                dynamicColor = chromeUiState.dynamicColorEnabled,
                pureBlackDark = chromeUiState.pureBlackDarkEnabled
            ) {
                AvdiBookApp(
                    appContainer = application.appContainer,
                    chromeUiState = chromeUiState,
                    onOnboardingCompleted = { chromeViewModel.setOnboardingCompleted(true) },
                    onThemeModeChanged = chromeViewModel::setThemeMode,
                    onDynamicColorChanged = chromeViewModel::setDynamicColorEnabled,
                    onPureBlackChanged = chromeViewModel::setPureBlackDarkEnabled,
                    onMiniPlayerTogglePlayPause = chromeViewModel::toggleMiniPlayerPlayPause,
                    onMiniPlayerSkipBack = chromeViewModel::miniPlayerSkipBack,
                    onMiniPlayerSkipForward = chromeViewModel::miniPlayerSkipForward
                )

                if (pendingCrash != null) {
                    var shown by remember { mutableStateOf(true) }
                    if (shown) {
                        AlertDialog(
                            onDismissRequest = { shown = false },
                            title = { Text("Previous Crash Report") },
                            text = {
                                Text(
                                    text = pendingCrash,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 400.dp)
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState())
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { shown = false }) {
                                    Text("Dismiss")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
