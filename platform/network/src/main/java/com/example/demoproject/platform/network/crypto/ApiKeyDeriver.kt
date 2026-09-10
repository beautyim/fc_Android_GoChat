package com.example.demoproject.platform.network.crypto

import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Derives the 16-char AES key used by the sign-and-encrypt interceptor.
 *
 * Formula (fixed by the server and historical clients — do not change without a
 * coordinated rollout):
 *
 *   key = md5(channelName + uid + apiEncryptionKey).take(16)
 *
 * For the MVP the [uid] is always the anonymous `0`, so every client produces the
 * same key as long as [channelName] and [apiEncryptionKey] agree. The helper keeps
 * the `uid` parameter so a future rollout can pin keys to a signed-in user.
 */
@Singleton
class ApiKeyDeriver @Inject constructor() {

    fun derive(channelName: String, uid: Long, apiEncryptionKey: String): String {
        val seed = buildString(channelName.length + apiEncryptionKey.length + 20) {
            append(channelName)
            append(uid)
            append(apiEncryptionKey)
        }
        val digest = md5Hex(seed)
        return if (digest.length >= AES_KEY_LENGTH) digest.substring(0, AES_KEY_LENGTH) else digest
    }

    private fun md5Hex(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return buildString(bytes.size * 2) {
            for (b in bytes) {
                val v = b.toInt() and 0xff
                if (v < 0x10) append('0')
                append(Integer.toHexString(v))
            }
        }
    }

    private companion object {
        const val AES_KEY_LENGTH = 16
    }
}
