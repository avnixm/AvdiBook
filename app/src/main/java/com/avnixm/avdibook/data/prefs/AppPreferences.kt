package com.avnixm.avdibook.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.avnixm.avdibook.data.model.BookDefaults
import com.avnixm.avdibook.data.model.ListeningSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

class AppPreferences(private val context: Context) {
    enum class ThemeMode(val value: Int) {
        SYSTEM(0),
        LIGHT(1),
        DARK(2);

        companion object {
            fun fromValue(value: Int): ThemeMode {
                return entries.firstOrNull { it.value == value } ?: SYSTEM
            }
        }
    }

    enum class TextScalePreset(val value: Int, val scaleMultiplier: Float) {
        STANDARD(0, 1.0f),
        LARGE(1, 1.08f),
        LARGEST(2, 1.16f);

        companion object {
            fun fromValue(value: Int): TextScalePreset {
                return entries.firstOrNull { it.value == value } ?: STANDARD
            }
        }
    }

    data class AccessibilitySettings(
        val textScalePreset: TextScalePreset = TextScalePreset.STANDARD,
        val reducedMotionEnabled: Boolean = false
    )

    private object Keys {
        val NotificationPermissionAsked = booleanPreferencesKey("notification_permission_asked_once")
        val OnboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val ThemeMode = intPreferencesKey("theme_mode")
        val DynamicColorEnabled = booleanPreferencesKey("dynamic_color_enabled")
        val PureBlackDarkEnabled = booleanPreferencesKey("pure_black_dark_enabled")
        val DefaultSpeed = floatPreferencesKey("default_speed")
        val DefaultSkipForwardSec = intPreferencesKey("default_skip_forward_sec")
        val DefaultSkipBackSec = intPreferencesKey("default_skip_back_sec")
        val DefaultAutoRewindSec = intPreferencesKey("default_auto_rewind_sec")
        val DefaultAutoRewindAfterPauseSec = intPreferencesKey("default_auto_rewind_after_pause_sec")
        val DefaultUseLoudnessBoost = booleanPreferencesKey("default_use_loudness_boost")
        val TextScalePreset = intPreferencesKey("text_scale_preset")
        val ReducedMotionEnabled = booleanPreferencesKey("reduced_motion_enabled")
    }

    val notificationPermissionAskedOnce: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[Keys.NotificationPermissionAsked] ?: false
    }

    val onboardingCompleted: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[Keys.OnboardingCompleted] ?: false
    }

    val themeMode: Flow<ThemeMode> = context.appDataStore.data.map { prefs ->
        ThemeMode.fromValue(prefs[Keys.ThemeMode] ?: ThemeMode.SYSTEM.value)
    }

    val dynamicColorEnabled: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[Keys.DynamicColorEnabled] ?: true
    }

    val pureBlackDarkEnabled: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[Keys.PureBlackDarkEnabled] ?: false
    }

    val listeningDefaults: Flow<ListeningSettings> = context.appDataStore.data.map { prefs ->
        ListeningSettings(
            playbackSpeed = prefs[Keys.DefaultSpeed] ?: BookDefaults.SPEED,
            skipForwardSec = prefs[Keys.DefaultSkipForwardSec] ?: BookDefaults.SKIP_FORWARD_SEC,
            skipBackSec = prefs[Keys.DefaultSkipBackSec] ?: BookDefaults.SKIP_BACK_SEC,
            autoRewindSec = prefs[Keys.DefaultAutoRewindSec] ?: BookDefaults.AUTO_REWIND_SEC,
            autoRewindAfterPauseSec = prefs[Keys.DefaultAutoRewindAfterPauseSec] ?: BookDefaults.AUTO_REWIND_AFTER_PAUSE_SEC,
            useLoudnessBoost = prefs[Keys.DefaultUseLoudnessBoost] ?: BookDefaults.USE_LOUDNESS_BOOST
        )
    }

    val accessibilitySettings: Flow<AccessibilitySettings> = context.appDataStore.data.map { prefs ->
        AccessibilitySettings(
            textScalePreset = TextScalePreset.fromValue(
                prefs[Keys.TextScalePreset] ?: TextScalePreset.STANDARD.value
            ),
            reducedMotionEnabled = prefs[Keys.ReducedMotionEnabled] ?: false
        )
    }

    suspend fun isNotificationPermissionAskedOnce(): Boolean {
        return notificationPermissionAskedOnce.first()
    }

    suspend fun setNotificationPermissionAskedOnce(value: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.NotificationPermissionAsked] = value
        }
    }

    suspend fun isOnboardingCompleted(): Boolean {
        return onboardingCompleted.first()
    }

    suspend fun setOnboardingCompleted(value: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.OnboardingCompleted] = value
        }
    }

    suspend fun getThemeMode(): ThemeMode {
        return themeMode.first()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.ThemeMode] = mode.value
        }
    }

    suspend fun isDynamicColorEnabled(): Boolean {
        return dynamicColorEnabled.first()
    }

    suspend fun setDynamicColorEnabled(value: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.DynamicColorEnabled] = value
        }
    }

    suspend fun isPureBlackDarkEnabled(): Boolean {
        return pureBlackDarkEnabled.first()
    }

    suspend fun setPureBlackDarkEnabled(value: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.PureBlackDarkEnabled] = value
        }
    }

    suspend fun getListeningDefaults(): ListeningSettings {
        return listeningDefaults.first()
    }

    suspend fun setListeningDefaults(value: ListeningSettings) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.DefaultSpeed] = value.playbackSpeed
            prefs[Keys.DefaultSkipForwardSec] = value.skipForwardSec
            prefs[Keys.DefaultSkipBackSec] = value.skipBackSec
            prefs[Keys.DefaultAutoRewindSec] = value.autoRewindSec
            prefs[Keys.DefaultAutoRewindAfterPauseSec] = value.autoRewindAfterPauseSec
            prefs[Keys.DefaultUseLoudnessBoost] = value.useLoudnessBoost
        }
    }

    suspend fun getDefaultSpeed(): Float {
        return getListeningDefaults().playbackSpeed
    }

    suspend fun setDefaultSpeed(value: Float) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.DefaultSpeed] = value
        }
    }

    suspend fun getDefaultSkipForwardSec(): Int {
        return getListeningDefaults().skipForwardSec
    }

    suspend fun setDefaultSkipForwardSec(value: Int) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.DefaultSkipForwardSec] = value
        }
    }

    suspend fun getDefaultSkipBackSec(): Int {
        return getListeningDefaults().skipBackSec
    }

    suspend fun setDefaultSkipBackSec(value: Int) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.DefaultSkipBackSec] = value
        }
    }

    suspend fun getDefaultAutoRewindSec(): Int {
        return getListeningDefaults().autoRewindSec
    }

    suspend fun setDefaultAutoRewindSec(value: Int) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.DefaultAutoRewindSec] = value
        }
    }

    suspend fun getDefaultAutoRewindAfterPauseSec(): Int {
        return getListeningDefaults().autoRewindAfterPauseSec
    }

    suspend fun setDefaultAutoRewindAfterPauseSec(value: Int) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.DefaultAutoRewindAfterPauseSec] = value
        }
    }

    suspend fun getDefaultUseLoudnessBoost(): Boolean {
        return getListeningDefaults().useLoudnessBoost
    }

    suspend fun setDefaultUseLoudnessBoost(value: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.DefaultUseLoudnessBoost] = value
        }
    }

    suspend fun getAccessibilitySettings(): AccessibilitySettings {
        return accessibilitySettings.first()
    }

    suspend fun setTextScalePreset(value: TextScalePreset) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.TextScalePreset] = value.value
        }
    }

    suspend fun setReducedMotionEnabled(value: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.ReducedMotionEnabled] = value
        }
    }
}
