package com.avnixm.avdibook.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avnixm.avdibook.AppContainer
import com.avnixm.avdibook.AvdiBookApplication
import com.avnixm.avdibook.data.prefs.AppPreferences
import com.avnixm.avdibook.ui.book.BookRoute
import com.avnixm.avdibook.ui.common.MiniPlayerBar
import com.avnixm.avdibook.ui.imports.ImportManagementRoute
import com.avnixm.avdibook.ui.library.LibraryRoute
import com.avnixm.avdibook.ui.library.SearchRoute
import com.avnixm.avdibook.ui.nowplaying.NowPlayingEmptyScreen
import com.avnixm.avdibook.ui.nowplaying.NowPlayingRoute
import com.avnixm.avdibook.ui.onboarding.ImportMethodRoute
import com.avnixm.avdibook.ui.onboarding.WelcomeRoute
import com.avnixm.avdibook.ui.settings.AboutRoute
import com.avnixm.avdibook.ui.settings.BackupRestoreRoute
import com.avnixm.avdibook.ui.settings.SettingsRoute

private const val ROUTE_WELCOME = "welcome"
private const val ROUTE_IMPORT_METHOD = "import_method"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_BOOK = "book/{bookId}"
private const val ROUTE_NOW_PLAYING_EMPTY = "now_playing"
private const val ROUTE_NOW_PLAYING = "now_playing/{bookId}"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_IMPORT_MANAGEMENT = "import_management"
private const val ROUTE_BACKUP_RESTORE = "backup_restore"
private const val ROUTE_ABOUT = "about"

private enum class BottomNavTab(
    val label: String,
    val icon: ImageVector,
    val route: String
) {
    LIBRARY("Library", Icons.AutoMirrored.Filled.MenuBook, ROUTE_LIBRARY),
    NOW_PLAYING("Now Playing", Icons.Default.Headphones, ROUTE_NOW_PLAYING_EMPTY),
    SETTINGS("Settings", Icons.Default.Settings, ROUTE_SETTINGS),
}

@Composable
fun AvdiBookApp(
    appContainer: AppContainer,
    chromeUiState: AppChromeUiState,
    onOnboardingCompleted: () -> Unit,
    onThemeModeChanged: (AppPreferences.ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onPureBlackChanged: (Boolean) -> Unit,
    onMiniPlayerTogglePlayPause: () -> Unit,
    onMiniPlayerSkipBack: () -> Unit,
    onMiniPlayerSkipForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!chromeUiState.onboardingCompleted) {
        OnboardingGraph(
            onOnboardingCompleted = onOnboardingCompleted,
            modifier = modifier
        )
    } else {
        MainAppGraph(
            appContainer = appContainer,
            chromeUiState = chromeUiState,
            onThemeModeChanged = onThemeModeChanged,
            onDynamicColorChanged = onDynamicColorChanged,
            onPureBlackChanged = onPureBlackChanged,
            onMiniPlayerTogglePlayPause = onMiniPlayerTogglePlayPause,
            onMiniPlayerSkipBack = onMiniPlayerSkipBack,
            onMiniPlayerSkipForward = onMiniPlayerSkipForward,
            modifier = modifier
        )
    }
}

@Composable
private fun OnboardingGraph(
    onOnboardingCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = ROUTE_WELCOME,
        modifier = modifier.fillMaxSize()
    ) {
        composable(ROUTE_WELCOME) {
            WelcomeRoute(
                onGetStarted = { navController.navigate(ROUTE_IMPORT_METHOD) },
                onRestoreBackup = { navController.navigate(ROUTE_IMPORT_METHOD) }
            )
        }
        composable(ROUTE_IMPORT_METHOD) {
            ImportMethodRoute(
                onCompleted = onOnboardingCompleted,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun MainAppGraph(
    appContainer: AppContainer,
    chromeUiState: AppChromeUiState,
    onThemeModeChanged: (AppPreferences.ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onPureBlackChanged: (Boolean) -> Unit,
    onMiniPlayerTogglePlayPause: () -> Unit,
    onMiniPlayerSkipBack: () -> Unit,
    onMiniPlayerSkipForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val selectedTab = when (currentRoute) {
        ROUTE_NOW_PLAYING_EMPTY, ROUTE_NOW_PLAYING -> BottomNavTab.NOW_PLAYING
        ROUTE_SETTINGS, ROUTE_IMPORT_MANAGEMENT, ROUTE_BACKUP_RESTORE, ROUTE_ABOUT -> BottomNavTab.SETTINGS
        else -> BottomNavTab.LIBRARY
    }

    val hideBottomBar = currentRoute == ROUTE_SEARCH

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (!hideBottomBar) {
                Column {
                    AnimatedVisibility(
                        visible = chromeUiState.miniPlayer.isVisible && chromeUiState.miniPlayer.bookId != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        MiniPlayerBar(
                            state = chromeUiState.miniPlayer,
                            onOpenPlayer = {
                                val bookId = chromeUiState.miniPlayer.bookId
                                if (bookId != null) {
                                    navController.navigate("now_playing/$bookId") {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onTogglePlayPause = onMiniPlayerTogglePlayPause,
                            onSkipBack = onMiniPlayerSkipBack,
                            onSkipForward = onMiniPlayerSkipForward
                        )
                    }

                    NavigationBar {
                        BottomNavTab.entries.forEach { tab ->
                            val isSelected = tab == selectedTab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    when (tab) {
                                        BottomNavTab.LIBRARY -> navController.navigate(ROUTE_LIBRARY) {
                                            popUpTo(ROUTE_LIBRARY) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                        BottomNavTab.NOW_PLAYING -> {
                                            val bookId = chromeUiState.miniPlayer.bookId
                                            val route = if (bookId != null) "now_playing/$bookId"
                                            else ROUTE_NOW_PLAYING_EMPTY
                                            navController.navigate(route) { launchSingleTop = true }
                                        }
                                        BottomNavTab.SETTINGS -> navController.navigate(ROUTE_SETTINGS) {
                                            popUpTo(ROUTE_SETTINGS) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                icon = {
                                    if (tab == BottomNavTab.NOW_PLAYING && chromeUiState.miniPlayer.isPlaying) {
                                        BadgedBox(badge = { Badge() }) {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_LIBRARY,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ROUTE_LIBRARY) {
                LibraryRoute(
                    onNavigateToBook = { bookId -> navController.navigate("book/$bookId") },
                    onOpenSearch = { navController.navigate(ROUTE_SEARCH) }
                )
            }

            composable(ROUTE_SEARCH) {
                val application = LocalContext.current.applicationContext as AvdiBookApplication
                SearchRoute(
                    application = application,
                    appContainer = appContainer,
                    onBack = { navController.popBackStack() },
                    onBookClick = { bookId -> navController.navigate("book/$bookId") }
                )
            }

            composable(
                route = ROUTE_BOOK,
                arguments = listOf(navArgument("bookId") { type = NavType.LongType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
                BookRoute(
                    bookId = bookId,
                    onBack = { navController.popBackStack() },
                    onNavigateToNowPlaying = { nowPlayingBookId ->
                        navController.navigate("now_playing/$nowPlayingBookId") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(ROUTE_NOW_PLAYING_EMPTY) {
                NowPlayingEmptyScreen(
                    onGoToLibrary = {
                        navController.navigate(ROUTE_LIBRARY) { launchSingleTop = true }
                    }
                )
            }

            composable(
                route = ROUTE_NOW_PLAYING,
                arguments = listOf(navArgument("bookId") { type = NavType.LongType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
                NowPlayingRoute(
                    bookId = bookId,
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(ROUTE_LIBRARY) { launchSingleTop = true }
                        }
                    }
                )
            }

            composable(ROUTE_SETTINGS) {
                SettingsRoute(
                    themeMode = chromeUiState.themeMode,
                    dynamicColorEnabled = chromeUiState.dynamicColorEnabled,
                    pureBlackDarkEnabled = chromeUiState.pureBlackDarkEnabled,
                    onThemeModeChanged = onThemeModeChanged,
                    onDynamicColorChanged = onDynamicColorChanged,
                    onPureBlackChanged = onPureBlackChanged,
                    onOpenImportManagement = { navController.navigate(ROUTE_IMPORT_MANAGEMENT) },
                    onOpenBackupRestore = { navController.navigate(ROUTE_BACKUP_RESTORE) },
                    onOpenAboutHelp = { navController.navigate(ROUTE_ABOUT) }
                )
            }

            composable(ROUTE_IMPORT_MANAGEMENT) {
                ImportManagementRoute(onBack = { navController.popBackStack() })
            }

            composable(ROUTE_BACKUP_RESTORE) {
                BackupRestoreRoute(onBack = { navController.popBackStack() })
            }

            composable(ROUTE_ABOUT) {
                AboutRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}
