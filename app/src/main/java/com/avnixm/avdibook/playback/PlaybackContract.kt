package com.avnixm.avdibook.playback

import android.os.Bundle

object PlaybackContract {
    const val EXTRA_BOOK_ID = "extra_book_id"
    const val COMMAND_SET_SLEEP_DURATION = "command_set_sleep_duration"
    const val COMMAND_SET_SLEEP_END_OF_TRACK = "command_set_sleep_end_of_track"
    const val COMMAND_CLEAR_SLEEP_TIMER = "command_clear_sleep_timer"
    const val COMMAND_GET_SLEEP_TIMER_STATE = "command_get_sleep_timer_state"
    const val EXTRA_SLEEP_DURATION_MINUTES = "extra_sleep_duration_minutes"
    const val EXTRA_SLEEP_MODE = "extra_sleep_mode"
    const val EXTRA_SLEEP_END_TIMESTAMP_MS = "extra_sleep_end_timestamp_ms"
    const val EXTRA_SLEEP_REMAINING_MS = "extra_sleep_remaining_ms"
}

fun Bundle.putSleepTimerState(state: SleepTimerState) {
    putString(PlaybackContract.EXTRA_SLEEP_MODE, state.mode.name)
    putLong(PlaybackContract.EXTRA_SLEEP_END_TIMESTAMP_MS, state.endTimestampMs ?: -1L)
    putLong(PlaybackContract.EXTRA_SLEEP_REMAINING_MS, state.remainingMs ?: -1L)
}

fun Bundle.readSleepTimerState(): SleepTimerState {
    val mode = runCatching {
        SleepTimerMode.valueOf(getString(PlaybackContract.EXTRA_SLEEP_MODE) ?: SleepTimerMode.OFF.name)
    }.getOrDefault(SleepTimerMode.OFF)
    val endTimestamp = getLong(PlaybackContract.EXTRA_SLEEP_END_TIMESTAMP_MS, -1L).takeIf { it >= 0L }
    val remaining = getLong(PlaybackContract.EXTRA_SLEEP_REMAINING_MS, -1L).takeIf { it >= 0L }
    return SleepTimerState(mode = mode, endTimestampMs = endTimestamp, remainingMs = remaining)
}
