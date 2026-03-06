package com.avnixm.avdibook.data.model

import com.avnixm.avdibook.data.db.entity.BookSettingsEntity

data class ListeningSettings(
    val playbackSpeed: Float = BookDefaults.SPEED,
    val skipForwardSec: Int = BookDefaults.SKIP_FORWARD_SEC,
    val skipBackSec: Int = BookDefaults.SKIP_BACK_SEC,
    val autoRewindSec: Int = BookDefaults.AUTO_REWIND_SEC,
    val autoRewindAfterPauseSec: Int = BookDefaults.AUTO_REWIND_AFTER_PAUSE_SEC,
    val useLoudnessBoost: Boolean = BookDefaults.USE_LOUDNESS_BOOST
) {
    fun toEntity(bookId: Long, updatedAt: Long = System.currentTimeMillis()): BookSettingsEntity {
        return BookSettingsEntity(
            bookId = bookId,
            playbackSpeed = playbackSpeed,
            skipForwardSec = skipForwardSec,
            skipBackSec = skipBackSec,
            autoRewindSec = autoRewindSec,
            autoRewindAfterPauseSec = autoRewindAfterPauseSec,
            useLoudnessBoost = useLoudnessBoost,
            updatedAt = updatedAt
        )
    }
}

fun BookSettingsEntity.toListeningSettings(): ListeningSettings {
    return ListeningSettings(
        playbackSpeed = playbackSpeed,
        skipForwardSec = skipForwardSec,
        skipBackSec = skipBackSec,
        autoRewindSec = autoRewindSec,
        autoRewindAfterPauseSec = autoRewindAfterPauseSec,
        useLoudnessBoost = useLoudnessBoost
    )
}
