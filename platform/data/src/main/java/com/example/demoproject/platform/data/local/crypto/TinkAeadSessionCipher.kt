package com.example.demoproject.platform.data.local.crypto

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Tink-backed [SessionCipher] using AES-256-GCM with the keyset wrapped by an Android
 * Keystore master key (hardware-backed on devices that support it).
 *
 * Storage layout:
 *  - The raw keyset sits in a dedicated SharedPreferences file ([KEYSET_PREFS_FILE])
 *    under [KEYSET_NAME], never mixed with app preferences.
 *  - The master key is kept in Android Keystore at [MASTER_KEY_URI]; Tink creates it
 *    on first run if missing.
 *
 * All ciphertexts bind to a fixed [ASSOCIATED_DATA_V1]. This means:
 *  - Ciphertexts produced for one payload kind can never be tricked into decrypting
 *    as another (defence in depth).
 *  - If we ever rotate the associated-data domain, the version bump in the constant
 *    name immediately invalidates old entries and forces a clean login.
 *
 * The [Aead] primitive is built lazily and cached — Tink initialization does IO on
 * SharedPreferences, so we only pay that cost once per process.
 */
class TinkAeadSessionCipher(
    private val context: Context,
) : SessionCipher {

    @Volatile
    private var cachedAead: Aead? = null
    private val initLock = Any()

    override fun encrypt(plaintext: String): String {
        val aead = aead()
        return try {
            val bytes = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), ASSOCIATED_DATA_V1)
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: GeneralSecurityException) {
            throw SessionCryptoException("Failed to encrypt session payload", e)
        }
    }

    override fun decrypt(ciphertext: String): String? {
        val aead = aead()
        val raw = try {
            Base64.decode(ciphertext, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return try {
            String(aead.decrypt(raw, ASSOCIATED_DATA_V1), Charsets.UTF_8)
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    private fun aead(): Aead {
        cachedAead?.let { return it }
        return synchronized(initLock) {
            cachedAead ?: buildAead().also { cachedAead = it }
        }
    }

    private fun buildAead(): Aead {
        return try {
            AeadConfig.register()
            AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS_FILE)
                .withKeyTemplate(KeyTemplates.get(KEY_TEMPLATE))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
                .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        } catch (e: GeneralSecurityException) {
            throw SessionCryptoException("Failed to initialize Tink AEAD", e)
        } catch (e: IOException) {
            throw SessionCryptoException("Failed to read session keyset", e)
        }
    }

    companion object {
        private const val KEYSET_PREFS_FILE = "berrycam_session_keyset"
        private const val KEYSET_NAME = "berrycam_session_aead"
        private const val KEY_TEMPLATE = "AES256_GCM"
        private const val MASTER_KEY_URI = "android-keystore://berrycam_session_master_key"
        private val ASSOCIATED_DATA_V1 = "berrycam.session.v1".toByteArray(Charsets.UTF_8)
    }
}
