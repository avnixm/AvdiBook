package com.avnixm.avdibook.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

class AppPreferences(private val context: Context) {
    private object Keys {
        val NotificationPermissionAsked = booleanPreferencesKey("notification_permission_asked_once")
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
}
