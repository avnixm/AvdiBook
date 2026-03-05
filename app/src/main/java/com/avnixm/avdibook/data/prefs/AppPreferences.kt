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
    private object Keys {
        val NotificationPermissionAsked = booleanPreferencesKey("notification_permission_asked_once")
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

    suspend fun isNotificationPermissionAskedOnce(): Boolean {
        return notificationPermissionAskedOnce.first()
    }

    suspend fun setNotificationPermissionAskedOnce(value: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.NotificationPermissionAsked] = value
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
