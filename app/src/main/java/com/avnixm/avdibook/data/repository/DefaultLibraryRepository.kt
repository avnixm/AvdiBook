package com.avnixm.avdibook.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.avnixm.avdibook.data.db.AvdiBookDatabase
import com.avnixm.avdibook.data.db.entity.BookEntity
import com.avnixm.avdibook.data.db.entity.TrackEntity
import com.avnixm.avdibook.data.metadata.ChapterExtractionScheduler
import com.avnixm.avdibook.data.model.BookProgressCalculator
import com.avnixm.avdibook.data.model.BookWithPlayback
import com.avnixm.avdibook.data.model.SourceType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class DefaultLibraryRepository(
    private val context: Context,
    private val database: AvdiBookDatabase,
    private val extractionScheduler: ChapterExtractionScheduler
) : LibraryRepository {
    private val bookDao = database.bookDao()
    private val trackDao = database.trackDao()
    private val playbackDao = database.playbackDao()

    override fun observeLibrary(): Flow<List<BookWithPlayback>> {
        return combine(
            bookDao.observeBooks(),
            playbackDao.observeAllPlaybackStates(),
            trackDao.observeAllTracks()
        ) { books, playbackStates, allTracks ->
            val playbackByBook = playbackStates.associateBy { it.bookId }
            val tracksByBook = allTracks.groupBy { it.bookId }
            books.map { book ->
                val bookTracks = tracksByBook[book.id].orEmpty().sortedBy { it.trackIndex }
                val playbackState = playbackByBook[book.id]
                BookWithPlayback(
                    book = book,
                    playbackState = playbackState,
                    trackCount = bookTracks.size,
                    progress = BookProgressCalculator.calculate(
                        tracks = bookTracks,
                        playbackState = playbackState
                    )
                )
            }
        }
    }

    override suspend fun importFolder(treeUri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: error("Unable to read the selected folder.")
            val audioFiles = collectAudioFiles(root)
            require(audioFiles.isNotEmpty()) { "No supported audio files were found in this folder." }

            val now = System.currentTimeMillis()
            val bookTitle = root.name?.takeIf { it.isNotBlank() } ?: "Imported Folder"

            val insertedBookId = database.withTransaction {
                val bookId = bookDao.insertBook(
                    BookEntity(
                        title = bookTitle,
                        sourceType = SourceType.FOLDER.value,
                        sourceUri = treeUri.toString(),
                        createdAt = now,
                        lastPlayedAt = null,
                        isMissingSource = false
                    )
                )

                trackDao.insertTracks(
                    audioFiles.mapIndexed { index, file ->
                        TrackEntity(
                            bookId = bookId,
                            uri = file.uri.toString(),
                            title = normalizedTrackTitle(file.name),
                            trackIndex = index,
                            durationMs = null
                        )
                    }
                )

                bookId
            }
            extractionScheduler.schedule(insertedBookId)
            insertedBookId
        }
    }

    override suspend fun importFiles(fileUris: List<Uri>): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            require(fileUris.isNotEmpty()) { "No files selected." }

            val now = System.currentTimeMillis()
            val bookTitle = "Imported (${timestamp(now)})"
            val sourceUri = "files:$now:${UUID.randomUUID()}"

            val insertedBookId = database.withTransaction {
                val bookId = bookDao.insertBook(
                    BookEntity(
                        title = bookTitle,
                        sourceType = SourceType.FILES.value,
                        sourceUri = sourceUri,
                        createdAt = now,
                        lastPlayedAt = null,
                        isMissingSource = false
                    )
                )

                trackDao.insertTracks(
                    fileUris.mapIndexed { index, uri ->
                        val singleDoc = DocumentFile.fromSingleUri(context, uri)
                        TrackEntity(
                            bookId = bookId,
                            uri = uri.toString(),
                            title = normalizedTrackTitle(singleDoc?.name ?: uri.lastPathSegment),
                            trackIndex = index,
                            durationMs = null
                        )
                    }
                )

                bookId
            }
            extractionScheduler.schedule(insertedBookId)
            insertedBookId
        }
    }

    override suspend fun getBook(bookId: Long): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.getBookById(bookId)
    }

    override suspend fun getTracks(bookId: Long): List<TrackEntity> = withContext(Dispatchers.IO) {
        trackDao.getTracksByBook(bookId)
    }

    override suspend fun refreshMissingSourceFlags() {
        withContext(Dispatchers.IO) {
            val books = bookDao.getAllBooks()
            books.forEach { book ->
                val missing = isSourceMissing(book.sourceType, book.sourceUri, book.id)
                if (missing != book.isMissingSource) {
                    bookDao.updateMissingSource(book.id, missing)
                }
            }
        }
    }

    override suspend fun backfillMissingCoverArt() {
        withContext(Dispatchers.IO) {
            bookDao.getAllBooks()
                .filter { it.coverArtPath == null }
                .forEach { book -> extractionScheduler.schedule(book.id) }
        }
    }

    private fun collectAudioFiles(root: DocumentFile): List<DocumentFile> {
        val result = mutableListOf<DocumentFile>()

        fun walk(node: DocumentFile) {
            if (node.isDirectory) {
                node.listFiles()
                    .sortedBy { it.name?.lowercase(Locale.ROOT) ?: "" }
                    .forEach(::walk)
                return
            }

            if (node.isFile && isAudioDocument(node)) {
                result += node
            }
        }

        walk(root)
        return result
    }

    private fun isAudioDocument(file: DocumentFile): Boolean {
        val mimeType = file.type?.lowercase(Locale.ROOT)
        if (mimeType?.startsWith("audio/") == true) return true
        if (mimeType == "video/mp4") return true  // M4b files are often misreported as video/mp4

        val extension = file.name
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.ROOT)
            .orEmpty()

        return extension in AUDIO_EXTENSIONS
    }

    private fun normalizedTrackTitle(rawTitle: String?): String {
        val title = rawTitle?.substringBeforeLast('.', rawTitle) ?: "Unknown Track"
        return title.ifBlank { "Unknown Track" }
    }

    private fun timestamp(epochMs: Long): String {
        val format = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
        return format.format(Date(epochMs))
    }

    companion object {
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "m4b", "aac", "ogg", "wav", "flac", "opus")
    }

    private suspend fun isSourceMissing(sourceType: Int, sourceUri: String, bookId: Long): Boolean {
        return when (sourceType) {
            SourceType.FOLDER.value -> {
                val uri = Uri.parse(sourceUri)
                !hasPersistedReadPermission(uri)
            }

            SourceType.FILES.value -> {
                val tracks = trackDao.getTracksByBook(bookId)
                if (tracks.isEmpty()) {
                    true
                } else {
                    tracks.none { track ->
                        val uri = Uri.parse(track.uri)
                        canOpenUri(uri)
                    }
                }
            }

            else -> false
        }
    }

    private fun hasPersistedReadPermission(uri: Uri): Boolean {
        return context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission
        }
    }

    private fun canOpenUri(uri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.read()
            }
        }.isSuccess
    }
}
