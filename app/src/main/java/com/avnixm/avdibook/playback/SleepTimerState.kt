package com.avnixm.avdibook.playback

enum class SleepTimerMode {
    OFF,
    DURATION,
    END_OF_TRACK
}

data class SleepTimerState(
    val mode: SleepTimerMode = SleepTimerMode.OFF,
    val endTimestampMs: Long? = null,
    val remainingMs: Long? = null
)
