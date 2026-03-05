package com.avnixm.avdibook.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.avnixm.avdibook.data.db.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Upsert
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE bookId = :bookId ORDER BY trackIndex ASC")
    suspend fun getTracksByBook(bookId: Long): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE bookId = :bookId ORDER BY trackIndex ASC")
    fun observeTracksByBook(bookId: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun getTrackById(trackId: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE bookId = :bookId AND uri = :uri LIMIT 1")
    suspend fun getByBookAndUri(bookId: Long, uri: String): TrackEntity?

    @Query("UPDATE tracks SET durationMs = :durationMs WHERE id = :trackId")
    suspend fun updateDuration(trackId: Long, durationMs: Long)

    @Query("SELECT * FROM tracks")
    suspend fun getAllTracks(): List<TrackEntity>

    @Query("SELECT * FROM tracks")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT bookId, COUNT(*) AS trackCount FROM tracks GROUP BY bookId")
    fun observeTrackCounts(): Flow<List<BookTrackCount>>
}

data class BookTrackCount(
    @ColumnInfo(name = "bookId")
    val bookId: Long,
    @ColumnInfo(name = "trackCount")
    val trackCount: Int
)
