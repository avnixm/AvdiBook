package com.avnixm.avdibook.data.repository

import com.avnixm.avdibook.data.db.entity.PlaybackStateEntity
import kotlinx.coroutines.flow.Flow

interface PlaybackRepository {
    suspend fun upsertPlaybackState(state: PlaybackStateEntity)

    suspend fun getPlaybackState(bookId: Long): PlaybackStateEntity?

    fun observePlaybackState(bookId: Long): Flow<PlaybackStateEntity?>

    fun observeAllPlaybackStates(): Flow<List<PlaybackStateEntity>>

    suspend fun clearPlaybackState(bookId: Long)

    suspend fun updateLastPlayed(bookId: Long, lastPlayedAt: Long)
}
