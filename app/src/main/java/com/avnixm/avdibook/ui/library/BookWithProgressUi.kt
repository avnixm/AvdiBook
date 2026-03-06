package com.avnixm.avdibook.ui.library

data class BookWithProgressUi(
    val bookId: Long,
    val title: String,
    val trackCount: Int,
    val hasResume: Boolean,
    val resumePositionMs: Long,
    val lastPlayedAt: Long?,
    val progressPercent: Float,
    val timeLeftMs: Long,
    val isProgressEstimated: Boolean,
    val isMissingSource: Boolean,
    val coverArtPath: String? = null
)
