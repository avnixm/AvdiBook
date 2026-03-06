package com.avnixm.avdibook.data

import com.avnixm.avdibook.data.db.entity.PlaybackStateEntity
import com.avnixm.avdibook.data.db.entity.TrackEntity
import com.avnixm.avdibook.data.model.BookProgressCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookProgressCalculatorTest {
    @Test
    fun calculatesProgressAcrossMultipleTracks() {
        val tracks = listOf(
            TrackEntity(id = 1, bookId = 1, uri = "a", title = "a", trackIndex = 0, durationMs = 1_000),
            TrackEntity(id = 2, bookId = 1, uri = "b", title = "b", trackIndex = 1, durationMs = 2_000)
        )
        val playback = PlaybackStateEntity(
            bookId = 1,
            trackId = 2,
            positionMs = 500,
            speed = 1f,
            updatedAt = 0
        )

        val result = BookProgressCalculator.calculate(tracks, playback)

        assertEquals(3_000, result.totalMs)
        assertEquals(1_500, result.progressMs)
        assertEquals(1_500, result.remainingMs)
        assertFalse(result.isEstimated)
    }

    @Test
    fun marksEstimatedWhenDurationMissing() {
        val tracks = listOf(
            TrackEntity(id = 1, bookId = 1, uri = "a", title = "a", trackIndex = 0, durationMs = null)
        )

        val result = BookProgressCalculator.calculate(tracks, null)

        assertTrue(result.isEstimated)
        assertEquals(0f, result.percent)
    }
}
