package com.avnixm.avdibook.playback.policy

import androidx.media3.common.Player
import com.avnixm.avdibook.playback.SleepTimerMode

object SleepTimerEngine {
    fun shouldTriggerDuration(nowMs: Long, endTimestampMs: Long?): Boolean {
        val end = endTimestampMs ?: return false
        return nowMs >= end
    }

    fun remainingMs(nowMs: Long, endTimestampMs: Long?): Long? {
        val end = endTimestampMs ?: return null
        return (end - nowMs).coerceAtLeast(0L)
    }

    fun shouldPauseOnTrackBoundary(mode: SleepTimerMode, transitionReason: Int): Boolean {
        return mode == SleepTimerMode.END_OF_TRACK &&
            transitionReason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
    }
}
