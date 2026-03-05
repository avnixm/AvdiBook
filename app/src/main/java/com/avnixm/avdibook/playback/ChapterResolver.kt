package com.avnixm.avdibook.playback

import com.avnixm.avdibook.data.db.entity.TrackEntity

object ChapterResolver {
    data class SeekTarget(
        val trackId: Long,
        val positionMs: Long
    )

    fun resolveSeekTarget(
        tracks: List<TrackEntity>,
        bookPositionMs: Long
    ): SeekTarget? {
        if (tracks.isEmpty()) return null

        val targetMs = bookPositionMs.coerceAtLeast(0L)
        val sorted = tracks.sortedBy { it.trackIndex }
        var consumed = 0L

        sorted.forEach { track ->
            val duration = track.durationMs ?: return SeekTarget(track.id, 0L)
            val nextConsumed = consumed + duration
            if (targetMs < nextConsumed) {
                return SeekTarget(track.id, (targetMs - consumed).coerceAtLeast(0L))
            }
            consumed = nextConsumed
        }

        val last = sorted.last()
        return SeekTarget(last.id, last.durationMs?.coerceAtLeast(0L) ?: 0L)
    }

    fun resolveCurrentChapterId(
        chapterStarts: List<Pair<Long, Long?>>,
        bookProgressMs: Long
    ): Long? {
        if (chapterStarts.isEmpty()) return null
        val progress = bookProgressMs.coerceAtLeast(0L)
        var result: Long? = null
        chapterStarts.forEach { (chapterId, startMs) ->
            if ((startMs ?: 0L) <= progress) {
                result = chapterId
            }
        }
        return result ?: chapterStarts.first().first
    }
}
