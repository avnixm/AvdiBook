package com.avnixm.avdibook.data.model

data class ChapterData(
    val id: Long,
    val bookId: Long,
    val trackId: Long?,
    val title: String,
    val startMs: Long,
    val endMs: Long?,
    val index: Int
)
