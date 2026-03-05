package com.avnixm.avdibook.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.avnixm.avdibook.data.db.entity.PlaybackStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackDao {
    @Upsert
    suspend fun upsertPlaybackState(state: PlaybackStateEntity)

    @Query("SELECT * FROM playback_state WHERE bookId = :bookId LIMIT 1")
    suspend fun getPlaybackState(bookId: Long): PlaybackStateEntity?

    @Query("SELECT * FROM playback_state WHERE bookId = :bookId LIMIT 1")
    fun observePlaybackState(bookId: Long): Flow<PlaybackStateEntity?>

    @Query("SELECT * FROM playback_state")
    fun observeAllPlaybackStates(): Flow<List<PlaybackStateEntity>>

    @Query("DELETE FROM playback_state WHERE bookId = :bookId")
    suspend fun deletePlaybackState(bookId: Long)
}
