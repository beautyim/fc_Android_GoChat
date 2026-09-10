package com.example.demoproject.platform.analytics.adjust

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private val Context.analyticsDataStore by preferencesDataStore(name = "analytics_prefs")

class AnalyticsEventStore constructor(
    private val context: Context,
) {
    private object Keys {
        val FIRST_DIALOG_TRACKED = booleanPreferencesKey("first_dialog_tracked")
        val LEGACY_FIRST_DIALOG_USERS = stringSetPreferencesKey("first_dialog_users")
    }

    suspend fun markFirstDialogTrackedIfNeeded(): Boolean {
        var shouldTrack = false
        context.analyticsDataStore.edit { prefs ->
            val alreadyTracked = prefs[Keys.FIRST_DIALOG_TRACKED] == true ||
                prefs[Keys.LEGACY_FIRST_DIALOG_USERS].orEmpty().isNotEmpty()
            if (!alreadyTracked) {
                prefs[Keys.FIRST_DIALOG_TRACKED] = true
                shouldTrack = true
            } else if (prefs[Keys.FIRST_DIALOG_TRACKED] != true) {
                prefs[Keys.FIRST_DIALOG_TRACKED] = true
            }
        }
        return shouldTrack
    }
}
