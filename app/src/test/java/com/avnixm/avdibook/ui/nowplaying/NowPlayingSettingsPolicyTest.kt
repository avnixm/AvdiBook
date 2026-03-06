package com.avnixm.avdibook.ui.nowplaying

import com.avnixm.avdibook.data.db.entity.BookSettingsEntity
import com.avnixm.avdibook.data.model.ListeningSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingSettingsPolicyTest {
    @Test
    fun quickSpeedOverride_createsOverrideFromEffectiveSettings_whenMissing() {
        val updated = NowPlayingSettingsPolicy.quickSpeedOverride(
            bookId = 7L,
            currentOverride = null,
            effectiveSettings = ListeningSettings(
                playbackSpeed = 1.0f,
                skipForwardSec = 30,
                skipBackSec = 10,
                autoRewindSec = 10,
                autoRewindAfterPauseSec = 180,
                useLoudnessBoost = true
            ),
            speed = 1.8f,
            nowMs = 1_000L
        )

        assertEquals(7L, updated.bookId)
        assertEquals(1.8f, updated.playbackSpeed)
        assertEquals(30, updated.skipForwardSec)
        assertEquals(10, updated.skipBackSec)
        assertEquals(1_000L, updated.updatedAt)
    }

    @Test
    fun quickSpeedOverride_updatesExistingOverride_onlyForCurrentBookEntity() {
        val existing = BookSettingsEntity(
            bookId = 12L,
            playbackSpeed = 1.25f,
            skipForwardSec = 45,
            skipBackSec = 15,
            autoRewindSec = 5,
            autoRewindAfterPauseSec = 300,
            useLoudnessBoost = false,
            updatedAt = 100L
        )

        val updated = NowPlayingSettingsPolicy.quickSpeedOverride(
            bookId = 12L,
            currentOverride = existing,
            effectiveSettings = ListeningSettings(),
            speed = 1.5f,
            nowMs = 2_000L
        )

        assertEquals(existing.bookId, updated.bookId)
        assertEquals(1.5f, updated.playbackSpeed)
        assertEquals(existing.skipForwardSec, updated.skipForwardSec)
        assertEquals(existing.skipBackSec, updated.skipBackSec)
        assertEquals(2_000L, updated.updatedAt)
    }
}
