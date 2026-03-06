package com.avnixm.avdibook.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

@Immutable
enum class AppWindowSize {
    COMPACT,
    MEDIUM,
    EXPANDED
}

fun appWindowSizeFromWidth(widthDp: Int): AppWindowSize {
    return when {
        widthDp < 600 -> AppWindowSize.COMPACT
        widthDp < 840 -> AppWindowSize.MEDIUM
        else -> AppWindowSize.EXPANDED
    }
}

@Composable
fun rememberAppWindowSize(): AppWindowSize {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) { appWindowSizeFromWidth(widthDp) }
}
