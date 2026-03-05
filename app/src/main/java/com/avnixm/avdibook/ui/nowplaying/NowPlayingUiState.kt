package com.avnixm.avdibook.ui.nowplaying

data class NowPlayingUiState(
    val bookId: Long,
    val bookTitle: String = "",
    val trackTitle: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f
)
