package com.avnixm.avdibook.data.repository

import com.avnixm.avdibook.data.db.entity.BookSettingsEntity
import com.avnixm.avdibook.data.db.entity.BookmarkEntity
import com.avnixm.avdibook.data.db.entity.ChapterEntity
import com.avnixm.avdibook.data.db.entity.TrackEntity
import com.avnixm.avdibook.data.model.BookDetailsData
import kotlinx.coroutines.flow.Flow

interface BookDetailsRepository {
    fun observeBookDetails(bookId: Long): Flow<BookDetailsData>

    suspend fun getOrCreateBookSettings(bookId: Long): BookSettingsEntity

    suspend fun upsertBookSettings(settings: BookSettingsEntity)

    suspend fun addBookmark(
        bookId: Long,
        trackId: Long,
        positionMs: Long,
        note: String?
    ): Long

    suspend fun deleteBookmark(bookmarkId: Long)

    suspend fun getTrackById(trackId: Long): TrackEntity?

    suspend fun getBookmarksByBook(bookId: Long): List<BookmarkEntity>

    suspend fun getChaptersByBook(bookId: Long): List<ChapterEntity>

    suspend fun replaceChapters(bookId: Long, chapters: List<ChapterEntity>)

    suspend fun updateTrackDuration(trackId: Long, durationMs: Long)
}
