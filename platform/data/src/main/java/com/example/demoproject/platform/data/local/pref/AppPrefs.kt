package com.example.demoproject.platform.data.local.pref

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "app_prefs")

class AppPrefs(
    private val context: Context,
) {

    companion object {
        fun create(context: Context): AppPrefs = AppPrefs(context.applicationContext ?: context)
    }
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NEW_MESSAGE_PUSH_ENABLED = booleanPreferencesKey("new_message_push_enabled")
        val LIKES_PUSH_ENABLED = booleanPreferencesKey("likes_push_enabled")
        val SELECTED_ICEBREAKER_IDS = stringSetPreferencesKey("selected_icebreaker_ids")
        val INSTALL_REPORTED = booleanPreferencesKey("install_reported")
        val FIRST_INSTALL_DETECTED = booleanPreferencesKey("first_install_detected")
        val ADJUST_ATTRIBUTION_REPORTED = booleanPreferencesKey("adjust_attribution_reported")
        val ADJUST_ATTRIBUTION_DEFERRED_ATTEMPTS = intPreferencesKey("adjust_attribution_deferred_attempts")
        val MQTT_DEBUG_LOGGING_ENABLED = booleanPreferencesKey("mqtt_debug_logging_enabled")
        val APP_LANGUAGE_TAG = stringPreferencesKey("app_language_tag")
        val FAKE_PAYMENT_ENABLED = booleanPreferencesKey("fake_payment_enabled")
        /** Peer ids that already showed chat-detail first-visit gift bar + greeting. */
        val CHAT_DETAIL_INTRO_SEEN_PEER_IDS = stringSetPreferencesKey("chat_detail_intro_seen_peer_ids")
    }

    val onboardingCompleted: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    val notificationsEnabled: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    val newMessagePushEnabled: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.NEW_MESSAGE_PUSH_ENABLED] ?: true }

    val likesPushEnabled: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.LIKES_PUSH_ENABLED] ?: true }

    val selectedIcebreakerIds: Flow<Set<String>> =
        context.appDataStore.data.map { it[Keys.SELECTED_ICEBREAKER_IDS] ?: emptySet() }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.appDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setNewMessagePushEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.NEW_MESSAGE_PUSH_ENABLED] = enabled }
    }

    suspend fun setLikesPushEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.LIKES_PUSH_ENABLED] = enabled }
    }

    suspend fun setSelectedIcebreakerIds(ids: Set<String>) {
        context.appDataStore.edit { it[Keys.SELECTED_ICEBREAKER_IDS] = ids }
    }

    val installReported: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.INSTALL_REPORTED] ?: false }

    suspend fun setInstallReported(reported: Boolean) {
        context.appDataStore.edit { it[Keys.INSTALL_REPORTED] = reported }
    }

    val firstInstallDetected: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.FIRST_INSTALL_DETECTED] ?: false }

    suspend fun setFirstInstallDetected(detected: Boolean) {
        context.appDataStore.edit { it[Keys.FIRST_INSTALL_DETECTED] = detected }
    }

    val adjustAttributionReported: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.ADJUST_ATTRIBUTION_REPORTED] ?: false }

    suspend fun setAdjustAttributionReported(reported: Boolean) {
        context.appDataStore.edit { it[Keys.ADJUST_ATTRIBUTION_REPORTED] = reported }
    }

    /**
     * Counts how many cold starts in a row we've collected only device-level attribution
     * signals (Adjust hasn't resolved campaign/tracker data yet). Used to cap retries before
     * we give up waiting and upload whatever we have.
     */
    suspend fun incrementAdjustAttributionDeferredAttempts(): Int {
        var updated = 0
        context.appDataStore.edit { prefs ->
            updated = (prefs[Keys.ADJUST_ATTRIBUTION_DEFERRED_ATTEMPTS] ?: 0) + 1
            prefs[Keys.ADJUST_ATTRIBUTION_DEFERRED_ATTEMPTS] = updated
        }
        return updated
    }

    val mqttDebugLoggingEnabled: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.MQTT_DEBUG_LOGGING_ENABLED] ?: false }

    suspend fun setMqttDebugLoggingEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.MQTT_DEBUG_LOGGING_ENABLED] = enabled }
    }

    val appLanguageTag: Flow<String?> =
        context.appDataStore.data.map { it[Keys.APP_LANGUAGE_TAG]?.takeIf(String::isNotBlank) }

    suspend fun currentAppLanguageTag(): String? =
        context.appDataStore.data.first()[Keys.APP_LANGUAGE_TAG]?.takeIf(String::isNotBlank)

    suspend fun setAppLanguageTag(tag: String?) {
        context.appDataStore.edit { prefs ->
            val normalized = tag?.takeIf(String::isNotBlank)
            if (normalized == null) {
                prefs.remove(Keys.APP_LANGUAGE_TAG)
            } else {
                prefs[Keys.APP_LANGUAGE_TAG] = normalized
            }
        }
    }

    /** Debug-only switch that lets QA bypass real Google Play Billing and simulate a successful purchase. */
    val fakePaymentEnabled: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.FAKE_PAYMENT_ENABLED] ?: false }

    suspend fun isFakePaymentEnabled(): Boolean =
        context.appDataStore.data.first()[Keys.FAKE_PAYMENT_ENABLED] ?: false

    suspend fun setFakePaymentEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.FAKE_PAYMENT_ENABLED] = enabled }
    }

    suspend fun hasSeenChatDetailIntro(peerId: String): Boolean {
        val key = peerId.trim()
        if (key.isEmpty()) return true
        return context.appDataStore.data.first()[Keys.CHAT_DETAIL_INTRO_SEEN_PEER_IDS]
            .orEmpty()
            .contains(key)
    }

    suspend fun markChatDetailIntroSeen(peerId: String) {
        val key = peerId.trim()
        if (key.isEmpty()) return
        context.appDataStore.edit { prefs ->
            val current = prefs[Keys.CHAT_DETAIL_INTRO_SEEN_PEER_IDS].orEmpty()
            prefs[Keys.CHAT_DETAIL_INTRO_SEEN_PEER_IDS] = current + key
        }
    }
}
