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

    suspend fun getDefaultSpeed(): Float {
        return context.appDataStore.data.map { prefs ->
            prefs[Keys.DefaultSpeed] ?: BookDefaults.SPEED
        }.first()
    }

    suspend fun getDefaultSkipForwardSec(): Int {
        return context.appDataStore.data.map { prefs ->
            prefs[Keys.DefaultSkipForwardSec] ?: BookDefaults.SKIP_FORWARD_SEC
        }.first()
    }

    suspend fun getDefaultSkipBackSec(): Int {
        return context.appDataStore.data.map { prefs ->
            prefs[Keys.DefaultSkipBackSec] ?: BookDefaults.SKIP_BACK_SEC
        }.first()
    }

    suspend fun getDefaultAutoRewindSec(): Int {
        return context.appDataStore.data.map { prefs ->
            prefs[Keys.DefaultAutoRewindSec] ?: BookDefaults.AUTO_REWIND_SEC
        }.first()
    }

    suspend fun getDefaultAutoRewindAfterPauseSec(): Int {
        return context.appDataStore.data.map { prefs ->
            prefs[Keys.DefaultAutoRewindAfterPauseSec] ?: BookDefaults.AUTO_REWIND_AFTER_PAUSE_SEC
        }.first()
    }

    suspend fun getDefaultUseLoudnessBoost(): Boolean {
        return context.appDataStore.data.map { prefs ->
            prefs[Keys.DefaultUseLoudnessBoost] ?: BookDefaults.USE_LOUDNESS_BOOST
        }.first()
    }
}
