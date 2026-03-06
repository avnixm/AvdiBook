package com.avnixm.avdibook.ui.nowplaying

import com.avnixm.avdibook.ui.book.BookmarkUi
import com.avnixm.avdibook.ui.book.ChapterUi
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
    val chapters: List<ChapterUi> = emptyList(),
    val bookmarks: List<BookmarkUi> = emptyList(),
    val bookProgressMs: Long = 0L,
    val bookTotalMs: Long = 0L,
    val timeLeftMs: Long = 0L,
    val bookProgressPercent: Float = 0f,
    val isBookProgressEstimated: Boolean = true,
    val currentChapterId: Long? = null,
    val coverArtPath: String? = null
)
