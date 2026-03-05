package com.avnixm.avdibook.playback

import com.avnixm.avdibook.playback.policy.AutoRewindPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoRewindPolicyTest {
    @Test
    fun shouldApply_returnsTrue_whenPauseExceedsThreshold() {
        assertTrue(AutoRewindPolicy.shouldApply(pauseDurationMs = 180_000, thresholdSec = 180))
    }

    @Test
    fun shouldApply_returnsFalse_whenPauseBelowThreshold() {
        assertFalse(AutoRewindPolicy.shouldApply(pauseDurationMs = 30_000, thresholdSec = 180))
    }

    @Test
    fun rewoundPosition_neverGoesBelowZero() {
        val rewound = AutoRewindPolicy.rewoundPosition(currentPositionMs = 4_000, rewindSec = 10)
        assertEquals(0L, rewound)
    }
}
