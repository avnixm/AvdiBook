package com.avnixm.avdibook.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class ResumeResolverTest {
    @Test
    fun resolveStartIndex_returnsSavedIndex_whenTrackExists() {
        val index = ResumeResolver.resolveStartIndex(trackIds = listOf(10L, 20L, 30L), trackId = 20L)
        assertEquals(1, index)
    }

    @Test
    fun resolveStartIndex_returnsZero_whenTrackMissing() {
        val index = ResumeResolver.resolveStartIndex(trackIds = listOf(10L, 20L), trackId = 99L)
        assertEquals(0, index)
    }

    @Test
    fun resolveStartIndex_returnsZero_whenNoSavedTrack() {
        val index = ResumeResolver.resolveStartIndex(trackIds = listOf(10L, 20L), trackId = null)
        assertEquals(0, index)
    }
}
