package com.example.demoproject.platform.network.config

import com.example.demoproject.platform.network.BuildConfig
import com.example.demoproject.platform.network.constants.PaginationConstants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central environment surface for networking. Values come from `:core:network` BuildConfig
 * (wire real staging/prod URLs via config property files / buildTypes without touching UI).
 *
 * The "crypto" family of fields ([apiEncryptionKey], [apiUserAgentPrefix], [channelName],
 * [webVersion], [signKey]) is only consumed when [requestSigningEnabled] is true; they
 * drive the sign-and-encrypt interceptor per the BerryCam transport contract
 * (double-MD5 signature + AES/ECB body + encrypted `ua` header).
 */
interface NetworkConfig {
    val baseUrl: String
    val connectTimeoutSeconds: Long
    val readTimeoutSeconds: Long
    val writeTimeoutSeconds: Long
    val pageSizeDefault: Int

    /** Optional client key header value — empty disables injection. */
    val clientKey: String

    /** Shared secret used by [com.example.demoproject.platform.network.interceptor.SigningEncryptionInterceptor]. */
    val signKey: String

    /** Seed for deriving the transport AES key. Must match the server contract. */
    val apiEncryptionKey: String

    /** String prefix prepended to the encrypted User-Agent header. */
    val apiUserAgentPrefix: String

    /** Distribution channel, fed into the AES key derivation and the UA string. */
    val channelName: String

    /** Web-layer protocol version, reported inside the UA string. */
    val webVersion: String

    /** Master switch for the sign-and-encrypt interceptor. */
    val requestSigningEnabled: Boolean

    val verboseHttpLogging: Boolean
}

@Singleton
class DefaultNetworkConfig @Inject constructor() : NetworkConfig {
    override val baseUrl: String get() = BuildConfig.BASE_URL
    override val connectTimeoutSeconds: Long get() = 30L
    override val readTimeoutSeconds: Long get() = 30L
    override val writeTimeoutSeconds: Long get() = 60L
    override val pageSizeDefault: Int get() = PaginationConstants.PAGE_SIZE_DEFAULT
    override val clientKey: String get() = BuildConfig.CLIENT_KEY
    override val signKey: String get() = BuildConfig.SIGN_KEY
    override val apiEncryptionKey: String get() = BuildConfig.ENC_KEY
    override val apiUserAgentPrefix: String get() = BuildConfig.UA_PREFIX
    override val channelName: String get() = BuildConfig.CHANNEL_NAME
    override val webVersion: String get() = BuildConfig.WEB_VERSION
    override val requestSigningEnabled: Boolean get() = BuildConfig.ENABLE_REQUEST_SIGN
    override val verboseHttpLogging: Boolean get() = BuildConfig.ENABLE_VERBOSE_HTTP_LOG
}
