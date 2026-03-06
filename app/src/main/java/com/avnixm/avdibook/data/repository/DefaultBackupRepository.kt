package com.avnixm.avdibook.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.avnixm.avdibook.data.db.AvdiBookDatabase
import com.avnixm.avdibook.data.db.entity.BookEntity
import com.avnixm.avdibook.data.db.entity.BookSettingsEntity
import com.avnixm.avdibook.data.db.entity.BookmarkEntity
import com.avnixm.avdibook.data.db.entity.ChapterEntity
import com.avnixm.avdibook.data.db.entity.PlaybackStateEntity
import com.avnixm.avdibook.data.db.entity.TrackEntity
import com.avnixm.avdibook.data.model.BackupPayloadV1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DefaultBackupRepository(
    private val appContext: Context,
    private val database: AvdiBookDatabase,
    private val libraryRepository: LibraryRepository
) : BackupRepository {
    private val bookDao = database.bookDao()
    private val trackDao = database.trackDao()
    private val playbackDao = database.playbackDao()
    private val bookSettingsDao = database.bookSettingsDao()
    private val bookmarkDao = database.bookmarkDao()
    private val chapterDao = database.chapterDao()

    override suspend fun exportToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildPayload()
            val json = payload.toJson().toString(2)
            appContext.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
                writer.write(json)
            } ?: error("Unable to open backup destination.")
        }
    }

    override suspend fun restoreFromUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = readPayload(uri)
            require(payload.schemaVersion == SCHEMA_VERSION) {
                "Unsupported backup schema version: ${payload.schemaVersion}"
            }
            database.withTransaction {
                mergePayload(payload)
            }
            libraryRepository.refreshMissingSourceFlags()
        }
    }

    override suspend fun validateBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = readPayload(uri)
            require(payload.schemaVersion == SCHEMA_VERSION) {
                "Unsupported backup schema version: ${payload.schemaVersion}"
            }
        }
    }

    private suspend fun buildPayload(): BackupPayloadV1 {
        val books = bookDao.getAllBooks().sortedBy { it.createdAt }
        val tracks = trackDao.getAllTracks().sortedWith(compareBy<TrackEntity> { it.bookId }.thenBy { it.trackIndex })
        val playbackStates = playbackDao.getAllPlaybackStates().sortedBy { it.bookId }
        val settings = bookSettingsDao.getAll().sortedBy { it.bookId }
        val bookmarks = bookmarkDao.getAll().sortedWith(compareBy<BookmarkEntity> { it.bookId }.thenBy { it.createdAt })
        val chapters = chapterDao.getAll().sortedWith(compareBy<ChapterEntity> { it.bookId }.thenBy { it.index })

        val sourceByBookId = books.associate { it.id to it.sourceUri }
        val trackById = tracks.associateBy { it.id }
        val trackByBookAndUri = tracks.groupBy { it.bookId }

        return BackupPayloadV1(
            schemaVersion = SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            books = books.map { book ->
                BackupPayloadV1.BackupBook(
                    sourceUri = book.sourceUri,
                    title = book.title,
                    sourceType = book.sourceType,
                    createdAt = book.createdAt,
                    lastPlayedAt = book.lastPlayedAt,
                    isMissingSource = book.isMissingSource
                )
            },
            tracks = tracks.mapNotNull { track ->
                val sourceUri = sourceByBookId[track.bookId] ?: return@mapNotNull null
                BackupPayloadV1.BackupTrack(
                    bookSourceUri = sourceUri,
                    uri = track.uri,
                    title = track.title,
                    trackIndex = track.trackIndex,
                    durationMs = track.durationMs
                )
            },
            playbackStates = playbackStates.mapNotNull { state ->
                val sourceUri = sourceByBookId[state.bookId] ?: return@mapNotNull null
                val trackUri = trackById[state.trackId]?.uri ?: return@mapNotNull null
                BackupPayloadV1.BackupPlaybackState(
                    bookSourceUri = sourceUri,
                    trackUri = trackUri,
                    positionMs = state.positionMs,
                    speed = state.speed,
                    updatedAt = state.updatedAt
                )
            },
            settings = settings.mapNotNull { value ->
                val sourceUri = sourceByBookId[value.bookId] ?: return@mapNotNull null
                BackupPayloadV1.BackupBookSettings(
                    bookSourceUri = sourceUri,
                    playbackSpeed = value.playbackSpeed,
                    skipForwardSec = value.skipForwardSec,
                    skipBackSec = value.skipBackSec,
                    autoRewindSec = value.autoRewindSec,
                    autoRewindAfterPauseSec = value.autoRewindAfterPauseSec,
                    useLoudnessBoost = value.useLoudnessBoost,
                    updatedAt = value.updatedAt
                )
            },
            bookmarks = bookmarks.mapNotNull { bookmark ->
                val sourceUri = sourceByBookId[bookmark.bookId] ?: return@mapNotNull null
                val trackUri = trackById[bookmark.trackId]?.uri ?: return@mapNotNull null
                BackupPayloadV1.BackupBookmark(
                    bookSourceUri = sourceUri,
                    trackUri = trackUri,
                    positionMs = bookmark.positionMs,
                    note = bookmark.note,
                    createdAt = bookmark.createdAt
                )
            },
            chapters = chapters.mapNotNull { chapter ->
                val sourceUri = sourceByBookId[chapter.bookId] ?: return@mapNotNull null
                val trackUri = chapter.trackId?.let { trackId ->
                    trackByBookAndUri[chapter.bookId]?.firstOrNull { it.id == trackId }?.uri
                }
                BackupPayloadV1.BackupChapter(
                    bookSourceUri = sourceUri,
                    trackUri = trackUri,
                    title = chapter.title,
                    startMs = chapter.startMs,
                    endMs = chapter.endMs,
                    index = chapter.index
                )
            }
        )
    }

    private suspend fun mergePayload(payload: BackupPayloadV1) {
        val existingBooks = bookDao.getAllBooks().associateBy { it.sourceUri }.toMutableMap()
        val bookIdBySource = mutableMapOf<String, Long>()

        payload.books.forEach { item ->
            val existing = existingBooks[item.sourceUri]
            if (existing == null) {
                val bookId = bookDao.insertBook(
                    BookEntity(
                        title = item.title,
                        sourceType = item.sourceType,
                        sourceUri = item.sourceUri,
                        createdAt = item.createdAt,
                        lastPlayedAt = item.lastPlayedAt,
                        isMissingSource = item.isMissingSource
                    )
                )
                bookIdBySource[item.sourceUri] = bookId
            } else {
                val merged = existing.copy(
                    title = item.title,
                    sourceType = item.sourceType,
                    createdAt = minOf(existing.createdAt, item.createdAt),
                    lastPlayedAt = listOfNotNull(existing.lastPlayedAt, item.lastPlayedAt).maxOrNull(),
                    isMissingSource = existing.isMissingSource || item.isMissingSource
                )
                bookDao.upsertBook(merged)
                bookIdBySource[item.sourceUri] = existing.id
            }
        }

        payload.tracks.forEach { track ->
            val bookId = bookIdBySource[track.bookSourceUri] ?: return@forEach
            val existingTrack = trackDao.getByBookAndUri(bookId, track.uri)
            if (existingTrack == null) {
                trackDao.insertTrack(
                    TrackEntity(
                        bookId = bookId,
                        uri = track.uri,
                        title = track.title,
                        trackIndex = track.trackIndex,
                        durationMs = track.durationMs
                    )
                )
            } else {
                trackDao.upsertTracks(
                    listOf(
                        existingTrack.copy(
                            title = track.title,
                            trackIndex = track.trackIndex,
                            durationMs = track.durationMs ?: existingTrack.durationMs
                        )
                    )
                )
            }
        }

        val tracksBySourceUri = bookIdBySource.mapValues { (_, bookId) ->
            trackDao.getTracksByBook(bookId).associateBy { it.uri }
        }

        payload.playbackStates.forEach { state ->
            val bookId = bookIdBySource[state.bookSourceUri] ?: return@forEach
            val trackId = tracksBySourceUri[state.bookSourceUri]?.get(state.trackUri)?.id ?: return@forEach
            val existing = playbackDao.getPlaybackState(bookId)
            if (existing == null || state.updatedAt >= existing.updatedAt) {
                playbackDao.upsertPlaybackState(
                    PlaybackStateEntity(
                        bookId = bookId,
                        trackId = trackId,
                        positionMs = state.positionMs,
                        speed = state.speed,
                        updatedAt = state.updatedAt
                    )
                )
            }
        }

        payload.settings.forEach { setting ->
            val bookId = bookIdBySource[setting.bookSourceUri] ?: return@forEach
            val existing = bookSettingsDao.getByBook(bookId)
            if (existing == null || setting.updatedAt >= existing.updatedAt) {
                bookSettingsDao.upsert(
                    BookSettingsEntity(
                        bookId = bookId,
                        playbackSpeed = setting.playbackSpeed,
                        skipForwardSec = setting.skipForwardSec,
                        skipBackSec = setting.skipBackSec,
                        autoRewindSec = setting.autoRewindSec,
                        autoRewindAfterPauseSec = setting.autoRewindAfterPauseSec,
                        useLoudnessBoost = setting.useLoudnessBoost,
                        updatedAt = setting.updatedAt
                    )
                )
            }
        }

        payload.bookmarks.groupBy { it.bookSourceUri }.forEach { (sourceUri, bookmarks) ->
            val bookId = bookIdBySource[sourceUri] ?: return@forEach
            val tracksByUri = tracksBySourceUri[sourceUri].orEmpty()
            val existing = bookmarkDao.listByBook(bookId)
            val existingKeys = existing.map { bookmark ->
                val uri = tracksByUri.entries.firstOrNull { it.value.id == bookmark.trackId }?.key
                BookmarkKey(uri, bookmark.positionMs, bookmark.createdAt, bookmark.note)
            }.toMutableSet()

            bookmarks.forEach { bookmark ->
                val trackId = tracksByUri[bookmark.trackUri]?.id ?: return@forEach
                val key = BookmarkKey(bookmark.trackUri, bookmark.positionMs, bookmark.createdAt, bookmark.note)
                if (key !in existingKeys) {
                    bookmarkDao.insert(
                        BookmarkEntity(
                            bookId = bookId,
                            trackId = trackId,
                            positionMs = bookmark.positionMs,
                            note = bookmark.note,
                            createdAt = bookmark.createdAt
                        )
                    )
                    existingKeys.add(key)
                }
            }
        }

        payload.chapters.groupBy { it.bookSourceUri }.forEach { (sourceUri, chapters) ->
            val bookId = bookIdBySource[sourceUri] ?: return@forEach
            val tracksByUri = tracksBySourceUri[sourceUri].orEmpty()
            chapterDao.replaceByBook(
                bookId = bookId,
                chapters = chapters
                    .sortedBy { it.index }
                    .map { chapter ->
                        ChapterEntity(
                            bookId = bookId,
                            trackId = chapter.trackUri?.let { tracksByUri[it]?.id },
                            title = chapter.title,
                            startMs = chapter.startMs,
                            endMs = chapter.endMs,
                            index = chapter.index
                        )
                    }
            )
        }
    }

    private fun readPayload(uri: Uri): BackupPayloadV1 {
        val json = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            reader.readText()
        } ?: error("Unable to open backup file.")

        return JSONObject(json).toPayload()
    }

    private fun BackupPayloadV1.toJson(): JSONObject {
        return JSONObject().apply {
            put("schemaVersion", schemaVersion)
            put("exportedAt", exportedAt)
            put("books", JSONArray().apply {
                books.forEach { book ->
                    put(JSONObject().apply {
                        put("sourceUri", book.sourceUri)
                        put("title", book.title)
                        put("sourceType", book.sourceType)
                        put("createdAt", book.createdAt)
                        put("lastPlayedAt", book.lastPlayedAt)
                        put("isMissingSource", book.isMissingSource)
                    })
                }
            })
            put("tracks", JSONArray().apply {
                tracks.forEach { track ->
                    put(JSONObject().apply {
                        put("bookSourceUri", track.bookSourceUri)
                        put("uri", track.uri)
                        put("title", track.title)
                        put("trackIndex", track.trackIndex)
                        put("durationMs", track.durationMs)
                    })
                }
            })
            put("playbackStates", JSONArray().apply {
                playbackStates.forEach { state ->
                    put(JSONObject().apply {
                        put("bookSourceUri", state.bookSourceUri)
                        put("trackUri", state.trackUri)
                        put("positionMs", state.positionMs)
                        put("speed", state.speed)
                        put("updatedAt", state.updatedAt)
                    })
                }
            })
            put("settings", JSONArray().apply {
                settings.forEach { setting ->
                    put(JSONObject().apply {
                        put("bookSourceUri", setting.bookSourceUri)
                        put("playbackSpeed", setting.playbackSpeed)
                        put("skipForwardSec", setting.skipForwardSec)
                        put("skipBackSec", setting.skipBackSec)
                        put("autoRewindSec", setting.autoRewindSec)
                        put("autoRewindAfterPauseSec", setting.autoRewindAfterPauseSec)
                        put("useLoudnessBoost", setting.useLoudnessBoost)
                        put("updatedAt", setting.updatedAt)
                    })
                }
            })
            put("bookmarks", JSONArray().apply {
                bookmarks.forEach { bookmark ->
                    put(JSONObject().apply {
                        put("bookSourceUri", bookmark.bookSourceUri)
                        put("trackUri", bookmark.trackUri)
                        put("positionMs", bookmark.positionMs)
                        put("note", bookmark.note)
                        put("createdAt", bookmark.createdAt)
                    })
                }
            })
            put("chapters", JSONArray().apply {
                chapters.forEach { chapter ->
                    put(JSONObject().apply {
                        put("bookSourceUri", chapter.bookSourceUri)
                        put("trackUri", chapter.trackUri)
                        put("title", chapter.title)
                        put("startMs", chapter.startMs)
                        put("endMs", chapter.endMs)
                        put("index", chapter.index)
                    })
                }
            })
        }
    }

    private fun JSONObject.toPayload(): BackupPayloadV1 {
        return BackupPayloadV1(
            schemaVersion = optInt("schemaVersion"),
            exportedAt = optLong("exportedAt"),
            books = optJSONArray("books").toList().map { value ->
                value as JSONObject
                BackupPayloadV1.BackupBook(
                    sourceUri = value.getString("sourceUri"),
                    title = value.getString("title"),
                    sourceType = value.getInt("sourceType"),
                    createdAt = value.getLong("createdAt"),
                    lastPlayedAt = if (value.isNull("lastPlayedAt")) null else value.getLong("lastPlayedAt"),
                    isMissingSource = value.optBoolean("isMissingSource", false)
                )
            },
            tracks = optJSONArray("tracks").toList().map { value ->
                value as JSONObject
                BackupPayloadV1.BackupTrack(
                    bookSourceUri = value.getString("bookSourceUri"),
                    uri = value.getString("uri"),
                    title = value.getString("title"),
                    trackIndex = value.getInt("trackIndex"),
                    durationMs = if (value.isNull("durationMs")) null else value.getLong("durationMs")
                )
            },
            playbackStates = optJSONArray("playbackStates").toList().map { value ->
                value as JSONObject
                BackupPayloadV1.BackupPlaybackState(
                    bookSourceUri = value.getString("bookSourceUri"),
                    trackUri = value.getString("trackUri"),
                    positionMs = value.getLong("positionMs"),
                    speed = value.getDouble("speed").toFloat(),
                    updatedAt = value.getLong("updatedAt")
                )
            },
            settings = optJSONArray("settings").toList().map { value ->
                value as JSONObject
                BackupPayloadV1.BackupBookSettings(
                    bookSourceUri = value.getString("bookSourceUri"),
                    playbackSpeed = value.getDouble("playbackSpeed").toFloat(),
                    skipForwardSec = value.getInt("skipForwardSec"),
                    skipBackSec = value.getInt("skipBackSec"),
                    autoRewindSec = value.getInt("autoRewindSec"),
                    autoRewindAfterPauseSec = value.getInt("autoRewindAfterPauseSec"),
                    useLoudnessBoost = value.optBoolean("useLoudnessBoost", false),
                    updatedAt = value.getLong("updatedAt")
                )
            },
            bookmarks = optJSONArray("bookmarks").toList().map { value ->
                value as JSONObject
                BackupPayloadV1.BackupBookmark(
                    bookSourceUri = value.getString("bookSourceUri"),
                    trackUri = value.getString("trackUri"),
                    positionMs = value.getLong("positionMs"),
                    note = if (value.isNull("note")) null else value.getString("note"),
                    createdAt = value.getLong("createdAt")
                )
            },
            chapters = optJSONArray("chapters").toList().map { value ->
                value as JSONObject
                BackupPayloadV1.BackupChapter(
                    bookSourceUri = value.getString("bookSourceUri"),
                    trackUri = if (value.isNull("trackUri")) null else value.getString("trackUri"),
                    title = value.getString("title"),
                    startMs = value.getLong("startMs"),
                    endMs = if (value.isNull("endMs")) null else value.getLong("endMs"),
                    index = value.getInt("index")
                )
            }
        )
    }

    private fun JSONArray?.toList(): List<Any> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (i in 0 until length()) {
                add(get(i))
            }
        }
    }

    private data class BookmarkKey(
        val trackUri: String?,
        val positionMs: Long,
        val createdAt: Long,
        val note: String?
    )

    companion object {
        private const val SCHEMA_VERSION = 1
    }
}
