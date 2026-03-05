package com.avnixm.avdibook.playback

object ResumeResolver {
    fun resolveStartIndex(trackIds: List<Long>, trackId: Long?): Int {
        if (trackIds.isEmpty()) return 0
        val restoredIndex = trackId?.let(trackIds::indexOf) ?: -1
        return if (restoredIndex >= 0) restoredIndex else 0
    }
}
