package com.avnixm.avdibook.ui.library

data class BookWithProgressUi(
    val bookId: Long,
    val title: String,
    val trackCount: Int,
    val hasResume: Boolean,
    val resumePositionMs: Long,
    val lastPlayedAt: Long?
)
