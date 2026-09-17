package com.example.demoproject.platform.data.promotion

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.promotionPopupDataStore by preferencesDataStore(name = "promotion_popup")

/**
 * Per-user promotion schedule persistence (non-secret prefs JSON keyed by userId).
 */
class PromotionPopupStore(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun load(userId: String): PromotionScheduleState {
        if (userId.isBlank()) return PromotionScheduleState()
        val raw = appContext.promotionPopupDataStore.data
            .map { prefs -> prefs[keyFor(userId)] }
            .first()
        if (raw.isNullOrBlank()) return PromotionScheduleState()
        return runCatching {
            json.decodeFromString(PromotionScheduleState.serializer(), raw)
        }.getOrDefault(PromotionScheduleState())
    }

    suspend fun save(userId: String, state: PromotionScheduleState) {
        if (userId.isBlank()) return
        val encoded = json.encodeToString(PromotionScheduleState.serializer(), state)
        appContext.promotionPopupDataStore.edit { prefs ->
            prefs[keyFor(userId)] = encoded
        }
    }

    suspend fun clear(userId: String) {
        if (userId.isBlank()) return
        appContext.promotionPopupDataStore.edit { prefs ->
            prefs.remove(keyFor(userId))
        }
    }

    private fun keyFor(userId: String) = stringPreferencesKey("schedule_$userId")
}
