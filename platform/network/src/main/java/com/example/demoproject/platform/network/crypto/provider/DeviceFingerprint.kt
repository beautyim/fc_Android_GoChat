package com.example.demoproject.platform.network.crypto.provider

/**
 * Device-level signals consumed by the sign-and-encrypt interceptor:
 *  - UA composition (every accessor is called for exactly one UA field)
 *  - the synthetic device-risk payload used when a POST has no JSON body
 *
 * Kept as an SPI so the network layer stays free of `android.*` calls; the real
 * implementation lives in `:core:data` where [android.content.Context] is available.
 *
 * All accessors MUST return non-null strings — empty is acceptable but `null`
 * would corrupt the `|`-separated UA string.
 */
interface DeviceFingerprint {

    // --- UA fields -------------------------------------------------------

    fun androidVersion(): String
    fun appVersion(): String
    fun deviceId(): String
    fun timeZone(): String
    fun systemLanguage(): String
    fun resolution(): String
    fun model(): String

    // --- Device risk payload -------------------------------------------

    /**
     * Returns the structured signals injected into a POST that has no JSON body.
     * The interceptor serialises this map verbatim (preserving insertion order),
     * signs it, and encrypts it — so the keys here MUST match the names the
     * server expects.
     */
    fun riskSignals(): Map<String, Any>
}
