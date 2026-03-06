package com.avnixm.avdibook.data.metadata

import android.content.Context
import android.net.Uri
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object M4bChapterParser {
    data class ParsedChapter(
        val title: String,
        val startMs: Long,
        val endMs: Long?
    )

    fun parse(context: Context, uri: Uri, totalDurationMs: Long?): List<ParsedChapter> {
        val chapters = runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).channel.use { channel ->
                    val fileSize = channel.size()
                    val payload = findChplPayload(channel, 0L, fileSize) ?: return@use emptyList()
                    parseChplPayload(payload, totalDurationMs)
                }
            } ?: emptyList()
        }.getOrDefault(emptyList())

        return chapters.sortedBy { it.startMs }
    }

    private fun findChplPayload(
        channel: java.nio.channels.FileChannel,
        start: Long,
        end: Long
    ): ByteArray? {
        var position = start
        while (position + 8 <= end) {
            channel.position(position)
            val header = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
            if (channel.read(header) < 8) return null
            header.flip()

            var atomSize = header.int.toLong() and 0xFFFFFFFFL
            val atomType = ByteArray(4).also { header.get(it) }.toString(Charsets.ISO_8859_1)
            var headerSize = 8L
            if (atomSize == 1L) {
                val ext = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
                if (channel.read(ext) < 8) return null
                ext.flip()
                atomSize = ext.long
                headerSize = 16L
            }
            if (atomSize <= 0L) return null

            val next = position + atomSize
            if (next > end) return null

            if (atomType == "chpl") {
                val payloadSize = (atomSize - headerSize).toInt().coerceAtLeast(0)
                val payload = ByteArray(payloadSize)
                channel.read(ByteBuffer.wrap(payload))
                return payload
            }

            if (atomType in CONTAINER_BOX_TYPES) {
                val childStart = position + headerSize + if (atomType == "meta") 4L else 0L
                val childEnd = position + atomSize
                val child = findChplPayload(channel, childStart, childEnd)
                if (child != null) return child
            }

            position = next
        }
        return null
    }

    private fun parseChplPayload(payload: ByteArray, totalDurationMs: Long?): List<ParsedChapter> {
        if (payload.size < 5) return emptyList()
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN)

        if (buffer.remaining() < 5) return emptyList()
        buffer.int // fullbox flags/version
        val count = buffer.get().toInt() and 0xFF
        if (count <= 0) return emptyList()

        val starts = mutableListOf<Pair<Long, String>>()
        for (index in 0 until count) {
            if (buffer.remaining() < 9) break
            val start100ns = buffer.long
            val titleLen = buffer.get().toInt() and 0xFF
            if (titleLen > buffer.remaining()) break
            val titleBytes = ByteArray(titleLen)
            buffer.get(titleBytes)
            val title = titleBytes.toString(Charsets.UTF_8).ifBlank { "Chapter ${index + 1}" }
            val startMs = (start100ns / 10_000L).coerceAtLeast(0L)
            starts += startMs to title
        }

        return starts.mapIndexed { idx, (start, title) ->
            val end = starts.getOrNull(idx + 1)?.first ?: totalDurationMs
            ParsedChapter(
                title = title,
                startMs = start,
                endMs = end
            )
        }
    }

    private val CONTAINER_BOX_TYPES = setOf(
        "moov",
        "trak",
        "mdia",
        "minf",
        "stbl",
        "udta",
        "meta",
        "ilst"
    )
}
