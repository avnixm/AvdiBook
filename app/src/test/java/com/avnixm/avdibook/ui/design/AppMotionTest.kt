package com.avnixm.avdibook.ui.design

import org.junit.Assert.assertEquals
import org.junit.Test

class AppMotionTest {
    @Test
    fun resolvedDuration_returnsZero_whenReducedMotionEnabled() {
        assertEquals(0, AppMotion.resolvedDuration(enabled = false, durationMillis = 280))
    }

    @Test
    fun resolvedDuration_returnsGivenDuration_whenMotionEnabled() {
        assertEquals(280, AppMotion.resolvedDuration(enabled = true, durationMillis = 280))
    }
}
