package com.example.demoproject.platform.data.network

import com.example.demoproject.platform.data.BuildConfig

private val PIC_BASE_URL: String = com.example.demoproject.platform.data.BuildConfig.PIC_CDN_BASE_URL
private val ASSET_BASE_URL: String = com.example.demoproject.platform.data.BuildConfig.ASSET_CDN_BASE_URL

fun String?.toPicUrlOrNull(): String? = toCdnUrlOrNull(PIC_BASE_URL)

fun String?.toAssetUrlOrNull(): String? = toCdnUrlOrNull(ASSET_BASE_URL)

/**
 * Voice / video file keys from chat JSON — these usually map to the asset bucket, not the image CDN.
 * Prefer S3, then image CDN, so relative upload paths play correctly in [android.media.MediaPlayer].
 */
fun String?.toChatBinaryUrlOrNull(): String? {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) return null
    if (raw.startsWith("http://", ignoreCase = true) ||
        raw.startsWith("https://", ignoreCase = true) ||
        raw.startsWith("content://") ||
        raw.startsWith("file://")
    ) {
        return raw
    }
    // Relative keys from `/upload` are typically on the same host as other media objects.
    return toAssetUrlOrNull() ?: toPicUrlOrNull()
}

/**
 * Converts absolute CDN URL to the backend expected relative media key.
 * Keeps non-CDN absolute URLs untouched and normalizes known resize prefixes.
 */
fun String?.toRelativeMediaPathOrNull(): String? {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) return null
    val withoutHost = stripKnownCdnPrefixes(raw)
    return withoutHost
        .trimStart('/')
        .removePrefix("fit-in/200x200/")
        .ifBlank { null }
}

private fun String?.toCdnUrlOrNull(baseUrl: String): String? {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) return null
    if (raw.isEmptyResizePrefix()) return null
    if (raw.startsWith("http://", ignoreCase = true) ||
        raw.startsWith("https://", ignoreCase = true) ||
        raw.startsWith("content://") ||
        raw.startsWith("file://")
    ) {
        return raw
    }
    // Some endpoints return malformed values like:
    // "fit-in/200x200/https://pic.267girl.com/fit-in/200x200/avatar/...png"
    // Prefer the embedded absolute URL instead of prepending the CDN host again.
    val httpsIdx = raw.indexOf("https://", ignoreCase = true)
    if (httpsIdx > 0) return raw.substring(httpsIdx)
    val httpIdx = raw.indexOf("http://", ignoreCase = true)
    if (httpIdx > 0) return raw.substring(httpIdx)
    // Backend may wrap a local content:// URI in a CDN resize prefix
    // (e.g. "fit-in/200x200/content://..."). Extract the embedded URI
    // since CDN resizing can't process local URIs.
    val contentIdx = raw.indexOf("content://")
    if (contentIdx > 0) return raw.substring(contentIdx)
    return baseUrl.trimEnd('/') + "/" + raw.removePrefix("/")
}

private fun String.isEmptyResizePrefix(): Boolean {
    val normalized = trim().trim('/').lowercase()
    return normalized == "fit-in/200x200"
}

private fun stripKnownCdnPrefixes(raw: String): String {
    val picHttps = PIC_BASE_URL.ensureTrailingSlash()
    val picHttp = picHttps.replaceFirst("https://", "http://")
    val assetHttps = ASSET_BASE_URL.ensureTrailingSlash()
    val assetHttp = assetHttps.replaceFirst("https://", "http://")
    return raw
        .removePrefix(picHttps)
        .removePrefix(picHttp)
        .removePrefix(assetHttps)
        .removePrefix(assetHttp)
}

private fun String.ensureTrailingSlash(): String = trimEnd('/') + "/"
