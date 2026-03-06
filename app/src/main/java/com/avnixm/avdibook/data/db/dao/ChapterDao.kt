package com.avnixm.avdibook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.avnixm.avdibook.data.db.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `index` ASC")
    suspend fun getByBook(bookId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters")
    suspend fun getAll(): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `index` ASC")
    fun observeByBook(bookId: Long): Flow<List<ChapterEntity>>

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteByBook(bookId: Long)

    @Transaction
    suspend fun replaceByBook(bookId: Long, chapters: List<ChapterEntity>) {
        deleteByBook(bookId)
        if (chapters.isNotEmpty()) {
            insertChapters(chapters)
        }
    }
}
