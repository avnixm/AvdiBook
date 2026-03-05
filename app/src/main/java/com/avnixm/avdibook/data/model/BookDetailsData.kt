package com.avnixm.avdibook.data.model

import com.avnixm.avdibook.data.db.entity.BookEntity
import com.avnixm.avdibook.data.db.entity.BookSettingsEntity
import com.avnixm.avdibook.data.db.entity.BookmarkEntity
import com.avnixm.avdibook.data.db.entity.ChapterEntity
import com.avnixm.avdibook.data.db.entity.PlaybackStateEntity
import com.avnixm.avdibook.data.db.entity.TrackEntity

data class BookDetailsData(
    val book: BookEntity?,
    val tracks: List<TrackEntity>,
    val chapters: List<ChapterEntity>,
    val bookmarks: List<BookmarkEntity>,
    val playbackState: PlaybackStateEntity?,
    val settings: BookSettingsEntity?,
    val progress: BookProgressData
)
