package com.avnixm.avdibook.ui.nowplaying

import com.avnixm.avdibook.ui.book.BookmarkUi
import com.avnixm.avdibook.ui.book.BookTrackUi

data class NowPlayingUiState(
    val bookId: Long,
    val bookTitle: String = "",
    val trackTitle: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val sleepLabel: String = "Off",
    val skipBackSec: Int = 10,
    val skipForwardSec: Int = 30,
    val tracks: List<BookTrackUi> = emptyList(),
    val bookmarks: List<BookmarkUi> = emptyList()
)
