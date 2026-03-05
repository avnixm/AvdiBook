package com.avnixm.avdibook.playback

import androidx.media3.common.Player
import com.avnixm.avdibook.playback.policy.SleepTimerEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerEngineTest {
    @Test
    fun duration_shouldTrigger_whenNowPastEnd() {
        assertTrue(SleepTimerEngine.shouldTriggerDuration(nowMs = 2000L, endTimestampMs = 1000L))
    }

    @Test
    fun duration_shouldNotTrigger_whenNowBeforeEnd() {
        assertFalse(SleepTimerEngine.shouldTriggerDuration(nowMs = 1000L, endTimestampMs = 2000L))
    }

    @Test
    fun endOfTrack_shouldPauseOnlyOnAutoTransition() {
        assertTrue(
            SleepTimerEngine.shouldPauseOnTrackBoundary(
                mode = SleepTimerMode.END_OF_TRACK,
                transitionReason = Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
            )
        )

        assertFalse(
            SleepTimerEngine.shouldPauseOnTrackBoundary(
                mode = SleepTimerMode.END_OF_TRACK,
                transitionReason = Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
            )
        )
    }
}
