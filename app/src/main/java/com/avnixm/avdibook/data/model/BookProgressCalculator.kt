package com.avnixm.avdibook.data.model

import com.avnixm.avdibook.data.db.entity.PlaybackStateEntity
import com.avnixm.avdibook.data.db.entity.TrackEntity

object BookProgressCalculator {
    fun calculate(
        tracks: List<TrackEntity>,
        playbackState: PlaybackStateEntity?
    ): BookProgressData {
        if (tracks.isEmpty()) {
            return BookProgressData(
                totalMs = 0L,
                progressMs = 0L,
                remainingMs = 0L,
                percent = 0f,
                isEstimated = true
            )
        }

        val sortedTracks = tracks.sortedBy { it.trackIndex }
        val knownDurations = sortedTracks.map { it.durationMs }
        val allKnown = knownDurations.all { it != null }
        val totalMs = knownDurations.sumOf { it ?: 0L }.coerceAtLeast(0L)

        val progressMs = if (playbackState != null) {
            val currentIndex = sortedTracks.indexOfFirst { it.id == playbackState.trackId }.coerceAtLeast(0)
            val previous = sortedTracks.take(currentIndex).sumOf { it.durationMs ?: 0L }
            (previous + playbackState.positionMs.coerceAtLeast(0L)).coerceAtLeast(0L)
        } else {
            0L
        }

        val remainingMs = (totalMs - progressMs).coerceAtLeast(0L)
        val percent = if (totalMs > 0) {
            (progressMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        return BookProgressData(
            totalMs = totalMs,
            progressMs = progressMs.coerceAtMost(totalMs.takeIf { it > 0 } ?: Long.MAX_VALUE),
            remainingMs = remainingMs,
            percent = percent,
            isEstimated = !allKnown || totalMs <= 0L
        )
    }
}
