package com.avnixm.avdibook.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.avnixm.avdibook.data.db.entity.BookSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookSettingsDao {
    @Upsert
    suspend fun upsert(settings: BookSettingsEntity)

    @Query("SELECT * FROM book_settings WHERE bookId = :bookId LIMIT 1")
    suspend fun getByBook(bookId: Long): BookSettingsEntity?

    @Query("SELECT * FROM book_settings")
    suspend fun getAll(): List<BookSettingsEntity>

    @Query("SELECT * FROM book_settings WHERE bookId = :bookId LIMIT 1")
    fun observeByBook(bookId: Long): Flow<BookSettingsEntity?>
}
