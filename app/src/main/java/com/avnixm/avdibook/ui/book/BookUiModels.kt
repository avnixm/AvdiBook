package com.avnixm.avdibook.ui.book

data class BookTrackUi(
    val trackId: Long,
    val title: String,
    val trackIndex: Int,
    val durationMs: Long?,
    val isPlaying: Boolean
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
    val useLoudnessBoost: Boolean
)

data class BookUiState(
    val isLoading: Boolean = true,
    val bookId: Long,
    val title: String = "",
    val tracks: List<BookTrackUi> = emptyList(),
    val bookmarks: List<BookmarkUi> = emptyList(),
    val settings: BookSettingsUi? = null,
    val hasPlaybackState: Boolean = false
)
