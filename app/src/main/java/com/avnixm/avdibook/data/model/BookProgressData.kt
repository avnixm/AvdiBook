package com.avnixm.avdibook.data.model

data class BookProgressData(
    val totalMs: Long,
    val progressMs: Long,
    val remainingMs: Long,
    val percent: Float,
    val isEstimated: Boolean
)
