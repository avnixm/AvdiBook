package com.avnixm.avdibook.data.model

import com.avnixm.avdibook.data.db.entity.BookEntity
import com.avnixm.avdibook.data.db.entity.PlaybackStateEntity

data class BookWithPlayback(
    val book: BookEntity,
    val playbackState: PlaybackStateEntity?,
    val trackCount: Int
)
