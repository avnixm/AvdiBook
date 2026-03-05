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

    @Query("SELECT * FROM books WHERE sourceUri = :sourceUri LIMIT 1")
    suspend fun getBySourceUri(sourceUri: String): BookEntity?

    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    fun observeBookById(bookId: Long): Flow<BookEntity?>

    @Query("UPDATE books SET lastPlayedAt = :lastPlayedAt WHERE id = :bookId")
    suspend fun updateLastPlayed(bookId: Long, lastPlayedAt: Long)

    @Query("UPDATE books SET isMissingSource = :isMissingSource WHERE id = :bookId")
    suspend fun updateMissingSource(bookId: Long, isMissingSource: Boolean)
}
