package com.avnixm.avdibook.data.repository

import com.avnixm.avdibook.data.db.dao.BookDao
import com.avnixm.avdibook.data.db.dao.BookSettingsDao
import com.avnixm.avdibook.data.db.dao.BookmarkDao
import com.avnixm.avdibook.data.db.dao.ChapterDao
import com.avnixm.avdibook.data.db.dao.PlaybackDao
import com.avnixm.avdibook.data.db.dao.TrackDao
import com.avnixm.avdibook.data.db.entity.BookSettingsEntity
import com.avnixm.avdibook.data.db.entity.BookmarkEntity
import com.avnixm.avdibook.data.db.entity.ChapterEntity
import com.avnixm.avdibook.data.db.entity.TrackEntity
import com.avnixm.avdibook.data.model.BookDetailsData
import com.avnixm.avdibook.data.model.BookProgressCalculator
import com.avnixm.avdibook.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class DefaultBookDetailsRepository(
    private val bookDao: BookDao,
    private val trackDao: TrackDao,
    private val playbackDao: PlaybackDao,
    private val bookSettingsDao: BookSettingsDao,
    private val bookmarkDao: BookmarkDao,
    private val chapterDao: ChapterDao,
    private val appPreferences: AppPreferences
) : BookDetailsRepository {
    override fun observeBookDetails(bookId: Long): Flow<BookDetailsData> {
        return combine(
            bookDao.observeBookById(bookId),
            trackDao.observeTracksByBook(bookId),
            chapterDao.observeByBook(bookId),
            bookmarkDao.observeByBook(bookId),
            playbackDao.observePlaybackState(bookId),
            bookSettingsDao.observeByBook(bookId)
        ) { book, tracks, chapters, bookmarks, playbackState, settings ->
            BookDetailsData(
                book = book,
                tracks = tracks,
                chapters = chapters,
                bookmarks = bookmarks,
                playbackState = playbackState,
                settings = settings,
                progress = BookProgressCalculator.calculate(
                    tracks = tracks,
                    playbackState = playbackState
                )
            )
        }
    }

    override suspend fun getOrCreateBookSettings(bookId: Long): BookSettingsEntity = withContext(Dispatchers.IO) {
        val existing = bookSettingsDao.getByBook(bookId)
        if (existing != null) return@withContext existing

        val created = BookSettingsEntity(
            bookId = bookId,
            playbackSpeed = appPreferences.getDefaultSpeed(),
            skipForwardSec = appPreferences.getDefaultSkipForwardSec(),
            skipBackSec = appPreferences.getDefaultSkipBackSec(),
            autoRewindSec = appPreferences.getDefaultAutoRewindSec(),
            autoRewindAfterPauseSec = appPreferences.getDefaultAutoRewindAfterPauseSec(),
            useLoudnessBoost = appPreferences.getDefaultUseLoudnessBoost(),
            updatedAt = System.currentTimeMillis()
        )
        bookSettingsDao.upsert(created)
        created
    }

    override suspend fun upsertBookSettings(settings: BookSettingsEntity) {
        withContext(Dispatchers.IO) {
            bookSettingsDao.upsert(
                settings.copy(updatedAt = System.currentTimeMillis())
            )
        }
    }

    override suspend fun addBookmark(
        bookId: Long,
        trackId: Long,
        positionMs: Long,
        note: String?
    ): Long {
        return withContext(Dispatchers.IO) {
            bookmarkDao.insert(
                BookmarkEntity(
                    bookId = bookId,
                    trackId = trackId,
                    positionMs = positionMs.coerceAtLeast(0L),
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun deleteBookmark(bookmarkId: Long) {
        withContext(Dispatchers.IO) {
            bookmarkDao.deleteById(bookmarkId)
        }
    }

    override suspend fun getTrackById(trackId: Long): TrackEntity? {
        return withContext(Dispatchers.IO) {
            trackDao.getTrackById(trackId)
        }
    }

    override suspend fun getBookmarksByBook(bookId: Long): List<BookmarkEntity> {
        return withContext(Dispatchers.IO) {
            bookmarkDao.listByBook(bookId)
        }
    }

    override suspend fun getChaptersByBook(bookId: Long): List<ChapterEntity> {
        return withContext(Dispatchers.IO) {
            chapterDao.getByBook(bookId)
        }
    }

    override suspend fun replaceChapters(bookId: Long, chapters: List<ChapterEntity>) {
        withContext(Dispatchers.IO) {
            chapterDao.replaceByBook(bookId, chapters)
        }
    }

    override suspend fun updateTrackDuration(trackId: Long, durationMs: Long) {
        withContext(Dispatchers.IO) {
            trackDao.updateDuration(trackId, durationMs.coerceAtLeast(0L))
        }
    }
}
