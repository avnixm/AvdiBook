package com.avnixm.avdibook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.avnixm.avdibook.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBook(book: BookEntity): Long

    @Query("SELECT * FROM books ORDER BY createdAt DESC")
    fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getBookById(bookId: Long): BookEntity?

    @Query("UPDATE books SET lastPlayedAt = :lastPlayedAt WHERE id = :bookId")
    suspend fun updateLastPlayed(bookId: Long, lastPlayedAt: Long)
}
