package com.avnixm.avdibook.playback.policy

object AutoRewindPolicy {
    fun shouldApply(pauseDurationMs: Long, thresholdSec: Int): Boolean {
        return pauseDurationMs >= thresholdSec.coerceAtLeast(0) * 1_000L
    }

    fun rewoundPosition(currentPositionMs: Long, rewindSec: Int): Long {
        val rewindMs = rewindSec.coerceAtLeast(0) * 1_000L
        return (currentPositionMs - rewindMs).coerceAtLeast(0L)
    }
}
