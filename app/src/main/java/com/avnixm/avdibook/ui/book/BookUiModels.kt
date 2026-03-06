package com.avnixm.avdibook.ui.book

data class BookTrackUi(
    val trackId: Long,
    val title: String,
    val trackIndex: Int,
    val durationMs: Long?,
    val isPlaying: Boolean
)

data class ChapterUi(
    val id: Long,
    val title: String,
    val startMs: Long,
    val endMs: Long?,
    val trackId: Long?,
    val isCurrent: Boolean
)

data class BookmarkUi(
    val id: Long,
    val trackId: Long,
    val positionMs: Long,
    val note: String?,
    val createdAt: Long,
    val trackTitle: String
)

data class BookSettingsUi(
    val playbackSpeed: Float,
    val skipForwardSec: Int,
    val skipBackSec: Int,
    val autoRewindSec: Int,
    val autoRewindAfterPauseSec: Int,
    val useLoudnessBoost: Boolean,
    val isUsingGlobalDefaults: Boolean
)

data class BookUiState(
    val isLoading: Boolean = true,
    val bookId: Long,
    val title: String = "",
    val tracks: List<BookTrackUi> = emptyList(),
    val chapters: List<ChapterUi> = emptyList(),
    val bookmarks: List<BookmarkUi> = emptyList(),
    val settings: BookSettingsUi? = null,
    val hasPlaybackState: Boolean = false,
    val bookProgressPercent: Float = 0f,
    val timeLeftMs: Long = 0L,
    val isProgressEstimated: Boolean = true,
    val isMissingSource: Boolean = false,
    val coverArtPath: String? = null
)
