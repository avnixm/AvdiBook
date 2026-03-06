package com.avnixm.avdibook.ui.common

import java.util.concurrent.TimeUnit

object TimeFormatters {
    fun formatClock(milliseconds: Long): String {
        val safeMillis = milliseconds.coerceAtLeast(0L)
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(safeMillis)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    fun formatHoursMinutes(milliseconds: Long): String {
        val safeMillis = milliseconds.coerceAtLeast(0L)
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(safeMillis)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m left"
            else -> "${minutes}m left"
        }
    }
}
