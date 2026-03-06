package com.avnixm.avdibook.data

import com.avnixm.avdibook.data.db.entity.BookSettingsEntity
import com.avnixm.avdibook.data.model.ListeningSettings
import com.avnixm.avdibook.data.model.resolveListeningSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ListeningSettingsResolverTest {
    @Test
    fun resolveListeningSettings_usesOverride_whenPresent() {
        val defaults = ListeningSettings(playbackSpeed = 1.0f, skipBackSec = 10, skipForwardSec = 30)
        val override = BookSettingsEntity(
            bookId = 4L,
            playbackSpeed = 1.5f,
            skipForwardSec = 45,
            skipBackSec = 15,
            autoRewindSec = 10,
            autoRewindAfterPauseSec = 180,
            useLoudnessBoost = true,
            updatedAt = 123L
        )

        val resolved = resolveListeningSettings(
            settingsOverride = override,
            defaults = defaults
        )

        assertEquals(1.5f, resolved.playbackSpeed)
        assertEquals(15, resolved.skipBackSec)
        assertEquals(45, resolved.skipForwardSec)
    }

    @Test
    fun resolveListeningSettings_overrideRemainsEffective_afterDefaultsChange() {
        val override = BookSettingsEntity(
            bookId = 9L,
            playbackSpeed = 1.25f,
            skipForwardSec = 60,
            skipBackSec = 5,
            autoRewindSec = 10,
            autoRewindAfterPauseSec = 180,
            useLoudnessBoost = false,
            updatedAt = 456L
        )

        val resolvedOldDefaults = resolveListeningSettings(
            settingsOverride = override,
            defaults = ListeningSettings(playbackSpeed = 1.0f, skipForwardSec = 30, skipBackSec = 10)
        )
        val resolvedNewDefaults = resolveListeningSettings(
            settingsOverride = override,
            defaults = ListeningSettings(playbackSpeed = 2.0f, skipForwardSec = 15, skipBackSec = 15)
        )

        assertEquals(resolvedOldDefaults, resolvedNewDefaults)
    }
}
