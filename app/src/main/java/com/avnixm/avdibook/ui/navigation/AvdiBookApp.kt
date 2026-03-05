package com.avnixm.avdibook.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avnixm.avdibook.ui.library.LibraryRoute
import com.avnixm.avdibook.ui.nowplaying.NowPlayingRoute

private const val LIBRARY_ROUTE = "library"
private const val NOW_PLAYING_ROUTE = "nowPlaying/{bookId}"

@Composable
fun AvdiBookApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = LIBRARY_ROUTE,
        modifier = modifier
    ) {
        composable(route = LIBRARY_ROUTE) {
            LibraryRoute(
                onNavigateToNowPlaying = { bookId ->
                    navController.navigate("nowPlaying/$bookId")
                }
            )
        }

        composable(
            route = NOW_PLAYING_ROUTE,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            NowPlayingRoute(
                bookId = bookId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
