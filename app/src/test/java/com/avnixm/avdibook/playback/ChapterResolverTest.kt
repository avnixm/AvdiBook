package com.avnixm.avdibook.playback

import com.avnixm.avdibook.data.db.entity.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ChapterResolverTest {
    @Test
    fun resolvesTrackAndOffsetFromBookPosition() {
        val tracks = listOf(
            TrackEntity(id = 10, bookId = 1, uri = "a", title = "A", trackIndex = 0, durationMs = 1_000),
            TrackEntity(id = 11, bookId = 1, uri = "b", title = "B", trackIndex = 1, durationMs = 2_000)
        )

        val target = ChapterResolver.resolveSeekTarget(
            tracks = tracks,
            bookPositionMs = 1_500
        )

        assertNotNull(target)
        assertEquals(11, target?.trackId)
        assertEquals(500, target?.positionMs)
    }
}
