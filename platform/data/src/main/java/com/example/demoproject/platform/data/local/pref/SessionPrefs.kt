package com.example.demoproject.platform.data.local.pref

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.local.crypto.SessionCipher
import com.example.demoproject.platform.data.local.crypto.SessionCryptoException
import com.example.demoproject.platform.data.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/**
 * Persisted session store.
 *
 * Sensitive fields ([Keys.USER_ID], [Keys.TOKEN]) are encrypted via [SessionCipher]
 * (Tink AEAD, master key in Android Keystore) before landing in DataStore. The
 * non-sensitive [Keys.PROFILE_COMPLETE] flag stays as a plain boolean because
 * knowing whether a user finished onboarding carries no credential value.
 *
 * [Keys.SCHEMA_VERSION] identifies the storage layout so we can migrate legacy
 * plaintext entries written by earlier builds without forcing users to log in again
 * after a version upgrade. A reader that encounters a ciphertext it cannot decrypt
 * (e.g. keyset wiped by an OS restore) treats the session as logged-out rather than
 * crashing — the user re-authenticates and a fresh keyset is provisioned.
 */
class SessionPrefs(
    private val context: Context,
    private val cipher: SessionCipher,
) {
    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val TOKEN = stringPreferencesKey("token")
        val PROFILE_COMPLETE = booleanPreferencesKey("profile_complete")
        val SCHEMA_VERSION = intPreferencesKey("schema_version")
    }

    val sessionFlow: Flow<Session?> = context.sessionDataStore.data.map { prefs ->
        readSession(prefs)
    }

    suspend fun saveSession(session: Session) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.USER_ID] = cipher.encrypt(session.userId)
            prefs[Keys.TOKEN] = cipher.encrypt(session.token)
            prefs[Keys.PROFILE_COMPLETE] = session.profileComplete
            prefs[Keys.SCHEMA_VERSION] = SCHEMA_ENCRYPTED_V1
        }
    }

    suspend fun markProfileComplete() {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.PROFILE_COMPLETE] = true
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { it.clear() }
    }

    /**
     * One-shot migration invoked during session bootstrap: if the persisted session
     * was written with an older plaintext schema, re-encrypt it in place so future
     * reads follow the encrypted path. Idempotent — safe to call multiple times and
     * to race with the flow collector (the final state is always consistent because
     * [readSession] inspects [Keys.SCHEMA_VERSION] per-read).
     */
    suspend fun migrateIfNeeded() {
        context.sessionDataStore.edit { prefs ->
            val schema = prefs[Keys.SCHEMA_VERSION] ?: SCHEMA_LEGACY_PLAINTEXT
            if (schema >= SCHEMA_ENCRYPTED_V1) return@edit

            val legacyToken = prefs[Keys.TOKEN].orEmpty()
            if (legacyToken.isBlank()) {
                prefs[Keys.SCHEMA_VERSION] = SCHEMA_ENCRYPTED_V1
                return@edit
            }
            val legacyUserId = prefs[Keys.USER_ID].orEmpty()
            try {
                prefs[Keys.USER_ID] = cipher.encrypt(legacyUserId)
                prefs[Keys.TOKEN] = cipher.encrypt(legacyToken)
                prefs[Keys.SCHEMA_VERSION] = SCHEMA_ENCRYPTED_V1
                AppLogger.i(TAG, "Migrated legacy plaintext session to encrypted schema")
            } catch (e: SessionCryptoException) {
                AppLogger.w(TAG, "Session migration failed, clearing stale data", e)
                prefs.clear()
            }
        }
    }

    private fun readSession(prefs: Preferences): Session? {
        val rawToken = prefs[Keys.TOKEN]?.takeIf { it.isNotBlank() } ?: return null
        val rawUserId = prefs[Keys.USER_ID].orEmpty()
        val profileComplete = prefs[Keys.PROFILE_COMPLETE] ?: false
        val schema = prefs[Keys.SCHEMA_VERSION] ?: SCHEMA_LEGACY_PLAINTEXT

        return if (schema >= SCHEMA_ENCRYPTED_V1) {
            val token = cipher.decrypt(rawToken) ?: run {
                AppLogger.w(TAG, "Session token failed to decrypt; treating as logged out")
                return null
            }
            val userId = if (rawUserId.isBlank()) "" else cipher.decrypt(rawUserId).orEmpty()
            Session(userId = userId, token = token, profileComplete = profileComplete)
        } else {
            Session(userId = rawUserId, token = rawToken, profileComplete = profileComplete)
        }
    }

    companion object {
        private const val TAG = "SessionPrefs"
        private const val SCHEMA_LEGACY_PLAINTEXT = 0
        private const val SCHEMA_ENCRYPTED_V1 = 1
    }
}
