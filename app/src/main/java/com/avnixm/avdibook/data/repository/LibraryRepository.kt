package com.avnixm.avdibook.data.repository

import android.net.Uri
import com.avnixm.avdibook.data.db.entity.BookEntity
import com.avnixm.avdibook.data.db.entity.TrackEntity
import com.avnixm.avdibook.data.model.BookWithPlayback
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeLibrary(): Flow<List<BookWithPlayback>>

    suspend fun importFolder(treeUri: Uri): Result<Long>

    suspend fun importFiles(fileUris: List<Uri>): Result<Long>

    suspend fun getBook(bookId: Long): BookEntity?

    suspend fun getTracks(bookId: Long): List<TrackEntity>
}
