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
import java.security.SecureRandom
import java.util.Arrays

/**
 * Owns the SQLCipher passphrase used by Room. The database key is random app-local
 * material; Tink wraps it with an Android Keystore master key before it is persisted.
 */
class DatabasePassphraseProvider(
    private val context: Context,
) {
    @Volatile
    private var cachedAead: Aead? = null
    @Volatile
    private var cachedPassphrase: String? = null
    private val initLock = Any()
    private val random = SecureRandom()

    fun getOrCreatePassphrase(): String {
        cachedPassphrase?.let { return it }
        return synchronized(initLock) {
            cachedPassphrase ?: loadOrCreatePassphrase().also { cachedPassphrase = it }
        }
    }

    private fun loadOrCreatePassphrase(): String {
        val prefs = context.getSharedPreferences(PASSPHRASE_PREFS_FILE, Context.MODE_PRIVATE)
        prefs.getString(PASSPHRASE_KEY, null)?.let { encrypted ->
            return decrypt(encrypted)
                ?: throw DatabaseCryptoException("Failed to decrypt database passphrase")
        }

        val passphrase = generatePassphrase()
        val persisted = prefs.edit()
            .putString(PASSPHRASE_KEY, encrypt(passphrase))
            .commit()
        if (!persisted) {
            throw DatabaseCryptoException("Failed to persist database passphrase")
        }
        return passphrase
    }

    private fun generatePassphrase(): String {
        val bytes = ByteArray(PASSPHRASE_BYTE_COUNT)
        return try {
            random.nextBytes(bytes)
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } finally {
            Arrays.fill(bytes, 0)
        }
    }

    private fun encrypt(plaintext: String): String {
        return try {
            val bytes = aead().encrypt(plaintext.toByteArray(Charsets.UTF_8), ASSOCIATED_DATA_V1)
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: GeneralSecurityException) {
            throw DatabaseCryptoException("Failed to encrypt database passphrase", e)
        }
    }

    private fun decrypt(ciphertext: String): String? {
        val raw = try {
            Base64.decode(ciphertext, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return try {
            String(aead().decrypt(raw, ASSOCIATED_DATA_V1), Charsets.UTF_8)
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
            throw DatabaseCryptoException("Failed to initialize database AEAD", e)
        } catch (e: IOException) {
            throw DatabaseCryptoException("Failed to read database keyset", e)
        }
    }

    companion object {
        private const val PASSPHRASE_BYTE_COUNT = 32
        private const val PASSPHRASE_PREFS_FILE = "demoproject_database_passphrase"
        private const val PASSPHRASE_KEY = "encrypted_passphrase"
        private const val KEYSET_PREFS_FILE = "demoproject_database_keyset"
        private const val KEYSET_NAME = "demoproject_database_aead"
        private const val KEY_TEMPLATE = "AES256_GCM"
        private const val MASTER_KEY_URI = "android-keystore://demoproject_database_master_key"
        private val ASSOCIATED_DATA_V1 = "demoproject.database.passphrase.v1".toByteArray(Charsets.UTF_8)
    }
}

class DatabaseCryptoException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
