package com.avnixm.avdibook.data.repository

import com.avnixm.avdibook.data.db.dao.BookDao
import com.avnixm.avdibook.data.db.dao.PlaybackDao
import com.avnixm.avdibook.data.db.entity.PlaybackStateEntity
import kotlinx.coroutines.flow.Flow

class DefaultPlaybackRepository(
    private val bookDao: BookDao,
    private val playbackDao: PlaybackDao
) : PlaybackRepository {
    override suspend fun upsertPlaybackState(state: PlaybackStateEntity) {
        playbackDao.upsertPlaybackState(state)
    }

    override suspend fun getPlaybackState(bookId: Long): PlaybackStateEntity? {
        return playbackDao.getPlaybackState(bookId)
    }

    override fun observePlaybackState(bookId: Long): Flow<PlaybackStateEntity?> {
        return playbackDao.observePlaybackState(bookId)
    }

    override fun observeAllPlaybackStates(): Flow<List<PlaybackStateEntity>> {
        return playbackDao.observeAllPlaybackStates()
    }

    override suspend fun clearPlaybackState(bookId: Long) {
        playbackDao.deletePlaybackState(bookId)
    }

    override suspend fun updateLastPlayed(bookId: Long, lastPlayedAt: Long) {
        bookDao.updateLastPlayed(bookId = bookId, lastPlayedAt = lastPlayedAt)
    }
}
