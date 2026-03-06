package com.avnixm.avdibook.data.metadata

import android.content.Context
import android.net.Uri
import com.avnixm.avdibook.data.db.dao.BookDao
import com.avnixm.avdibook.data.db.dao.ChapterDao
import com.avnixm.avdibook.data.db.dao.TrackDao
import com.avnixm.avdibook.data.db.entity.ChapterEntity
import com.avnixm.avdibook.data.db.entity.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChapterExtractionScheduler(
    private val appContext: Context,
    private val bookDao: BookDao,
    private val trackDao: TrackDao,
    private val chapterDao: ChapterDao
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val extractionDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + extractionDispatcher)
    private val inFlight = mutableSetOf<Long>()
    private val mutex = Mutex()

    fun schedule(bookId: Long) {
        scope.launch {
            val shouldRun = mutex.withLock {
                inFlight.add(bookId)
            }
            if (!shouldRun) return@launch

            try {
                runExtraction(bookId)
            } finally {
                mutex.withLock {
                    inFlight.remove(bookId)
                }
            }
        }
    }

    private suspend fun runExtraction(bookId: Long) {
        val book = bookDao.getBookById(bookId) ?: return
        val tracks = trackDao.getTracksByBook(bookId).sortedBy { it.trackIndex }
        if (tracks.isEmpty()) return

        // Populate duration metadata progressively; this is idempotent.
        tracks.forEach { track ->
            val parsedDuration = AudioMetadataExtractor.extractDurationMs(
                context = appContext,
                uri = Uri.parse(track.uri)
            )
            if (parsedDuration != null && parsedDuration > 0L) {
                trackDao.updateDuration(track.id, parsedDuration)
            }
        }

        val refreshedTracks = trackDao.getTracksByBook(bookId).sortedBy { it.trackIndex }
        val chapters = buildChapters(book.id, refreshedTracks)
        chapterDao.replaceByBook(book.id, chapters)

        if (book.coverArtPath == null) {
            val coverBytes = refreshedTracks.firstNotNullOfOrNull { track ->
                AudioMetadataExtractor.extractCoverArt(appContext, Uri.parse(track.uri))
            }
            if (coverBytes != null) {
                val dir = java.io.File(appContext.filesDir, "covers").also { it.mkdirs() }
                java.io.File(dir, "$bookId.png").writeBytes(coverBytes)
                bookDao.updateCoverArtPath(bookId, "${appContext.filesDir}/covers/$bookId.png")
            }
        }
    }

    private fun buildChapters(
        bookId: Long,
        tracks: List<TrackEntity>
    ): List<ChapterEntity> {
        if (tracks.isEmpty()) return emptyList()

        if (tracks.size == 1) {
            val only = tracks.first()
            // Always attempt M4b chapter parsing: the parser reads the raw MP4 atom structure
            // and safely returns empty for non-M4b files. We cannot rely on the URI string
            // ending in ".m4b" because SAF content URIs are often opaque IDs with no filename.
            val parsed = M4bChapterParser.parse(
                context = appContext,
                uri = Uri.parse(only.uri),
                totalDurationMs = only.durationMs
            )
            if (parsed.isNotEmpty()) {
                return parsed.mapIndexed { index, chapter ->
                    ChapterEntity(
                        bookId = bookId,
                        trackId = only.id,
                        title = chapter.title.ifBlank { "Chapter ${index + 1}" },
                        startMs = chapter.startMs,
                        endMs = chapter.endMs,
                        index = index
                    )
                }
            }
            return listOf(
                ChapterEntity(
                    bookId = bookId,
                    trackId = only.id,
                    title = "Whole book",
                    startMs = 0L,
                    endMs = only.durationMs,
                    index = 0
                )
            )
        }

        var cursor = 0L
        return tracks.mapIndexed { idx, track ->
            val start = cursor
            val duration = track.durationMs ?: 0L
            val end = if (duration > 0L) start + duration else null
            if (duration > 0L) cursor = end ?: cursor
            ChapterEntity(
                bookId = bookId,
                trackId = track.id,
                title = track.title.ifBlank { "Track ${idx + 1}" },
                startMs = start,
                endMs = end,
                index = idx
            )
        }
    }
}
