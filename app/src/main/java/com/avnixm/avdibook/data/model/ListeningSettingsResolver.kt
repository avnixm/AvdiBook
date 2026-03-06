package com.avnixm.avdibook.data.model

import com.avnixm.avdibook.data.db.entity.BookSettingsEntity

fun resolveListeningSettings(
    settingsOverride: BookSettingsEntity?,
    defaults: ListeningSettings
): ListeningSettings {
    return settingsOverride?.toListeningSettings() ?: defaults
}
