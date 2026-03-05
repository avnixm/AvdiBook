package com.avnixm.avdibook.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.avnixm.avdibook.data.db.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE bookId = :bookId ORDER BY trackIndex ASC")
    suspend fun getTracksByBook(bookId: Long): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE bookId = :bookId ORDER BY trackIndex ASC")
    fun observeTracksByBook(bookId: Long): Flow<List<TrackEntity>>

    @Query("SELECT bookId, COUNT(*) AS trackCount FROM tracks GROUP BY bookId")
    fun observeTrackCounts(): Flow<List<BookTrackCount>>
}

data class BookTrackCount(
    @ColumnInfo(name = "bookId")
    val bookId: Long,
    @ColumnInfo(name = "trackCount")
    val trackCount: Int
)
