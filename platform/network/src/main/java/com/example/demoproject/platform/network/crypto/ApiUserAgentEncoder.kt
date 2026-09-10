package com.example.demoproject.platform.network.crypto

import com.example.demoproject.platform.network.crypto.provider.DeviceFingerprint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the encrypted `ua` header value that the legacy API expects.
 *
 * Field order (must match the server parser exactly):
 *
 *   a | androidVersion | appVersion | webVersion | deviceId | timeZone |
 *   channelName | www | systemLanguage | resolution | model | 2
 *
 * The first `a` and trailing `2` are fixed protocol markers; `www` is a constant
 * that identified the original web surface and is preserved for parity.
 *
 * The joined string is AES-encrypted with the anonymous key and prefixed with
 * [com.example.demoproject.platform.network.config.NetworkConfig.apiUserAgentPrefix] so the
 * server can tell at a glance that the header came from a compatible client.
 */
@Singleton
class ApiUserAgentEncoder @Inject constructor(
    private val cipher: ApiBodyCipher,
    private val fingerprint: DeviceFingerprint,
) {

    fun encode(
        channelName: String,
        webVersion: String,
        aesKey: String,
        prefix: String,
    ): String {
        val ua = buildPlaintext(channelName, webVersion)
        val encrypted = cipher.encrypt(aesKey, ua)
        return prefix + encrypted
    }

    /**
     * Returns the raw `|`-joined User-Agent string **before** AES encryption.
     *
     * Exposed so the transport interceptor can log the human-readable UA alongside
     * the signed request body — mirroring the legacy `Timber.d("userAgent-->…")`
     * line that developers rely on when diagnosing device / version bugs.
     */
    fun buildPlaintext(channelName: String, webVersion: String): String {
        val fields = listOf(
            MARKER_LEADING,
            fingerprint.androidVersion(),
            fingerprint.appVersion(),
            webVersion,
            fingerprint.deviceId(),
            fingerprint.timeZone(),
            channelName,
            MARKER_SURFACE,
            fingerprint.systemLanguage(),
            fingerprint.resolution(),
            fingerprint.model(),
            MARKER_TRAILING,
        )
        return fields.joinToString(separator = "|")
    }

    private companion object {
        const val MARKER_LEADING = "a"
        const val MARKER_SURFACE = "www"
        const val MARKER_TRAILING = "2"
    }
}
