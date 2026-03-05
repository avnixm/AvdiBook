package com.avnixm.avdibook.data.repository

import android.net.Uri

interface BackupRepository {
    suspend fun exportToUri(uri: Uri): Result<Unit>
    suspend fun restoreFromUri(uri: Uri): Result<Unit>
    suspend fun validateBackup(uri: Uri): Result<Unit>
}
