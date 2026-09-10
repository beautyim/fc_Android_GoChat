package com.example.demoproject.platform.network.crypto

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES/ECB/PKCS5Padding with Base64 transport — matches the BerryCam backend
 * contract byte-for-byte.
 *
 * ECB is deliberately preserved for compatibility: both client and server treat
 * the payload as an opaque blob inside an already-framed JSON envelope, and the
 * outer transport is TLS. Rotating to GCM requires a coordinated server flip and
 * is intentionally out of scope for this class.
 */
@Singleton
class ApiBodyCipher @Inject constructor() {

    /** Encrypt [plaintext] with [key], returning Base64 (no-wrap). Empty on failure. */
    fun encrypt(key: String, plaintext: String): String {
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(key))
            val raw = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(raw, Base64.NO_WRAP)
        }.getOrElse { "" }
    }

    /** Decrypt a Base64 [ciphertext]. Returns an empty string when decryption fails. */
    fun decrypt(key: String, ciphertext: String): String {
        if (ciphertext.isBlank()) return ""
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keySpec(key))
            val raw = cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP))
            String(raw, Charsets.UTF_8)
        }.getOrElse { "" }
    }

    private fun keySpec(key: String) = SecretKeySpec(key.toByteArray(Charsets.UTF_8), ALGORITHM)

    private companion object {
        const val ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
    }
}
