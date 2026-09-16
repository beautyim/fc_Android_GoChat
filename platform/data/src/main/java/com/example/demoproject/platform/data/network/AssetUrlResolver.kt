package com.example.demoproject.platform.data.network

import com.example.demoproject.platform.data.BuildConfig

private val PIC_BASE_URL: String = com.example.demoproject.platform.data.BuildConfig.PIC_CDN_BASE_URL
private val ASSET_BASE_URL: String = com.example.demoproject.platform.data.BuildConfig.ASSET_CDN_BASE_URL

fun String?.toPicUrlOrNull(): String? = toCdnUrlOrNull(PIC_BASE_URL)

fun String?.toAssetUrlOrNull(): String? = toCdnUrlOrNull(ASSET_BASE_URL)

/**
 * Voice / video file keys from chat JSON / album / video-show — these map to the asset
 * bucket, not the image CDN. The pic host is an image-resize API and returns HTTP 400
 * (`RequestTypeError`) for `.mp4` and other binaries.
 *
 * Absolute pic-CDN video URLs are rewritten onto the asset host; local `content://` /
 * `file://` URIs are left untouched.
 */
fun String?.toChatBinaryUrlOrNull(): String? {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) return null
    if (raw.startsWith("content://") || raw.startsWith("file://")) {
        return raw
    }
    if (raw.startsWith("http://", ignoreCase = true) ||
        raw.startsWith("https://", ignoreCase = true)
    ) {
        if (raw.isLikelyVideoBinary()) {
            val relative = stripKnownCdnPrefixes(raw).trimStart('/')
            if (relative.isNotBlank() &&
                !relative.startsWith("http://", ignoreCase = true) &&
                !relative.startsWith("https://", ignoreCase = true)
            ) {
                return relative.toAssetUrlOrNull() ?: raw
            }
        }
        return raw
    }
    // Relative keys from `/upload` / `album/...` live on the asset host.
    return toAssetUrlOrNull() ?: toPicUrlOrNull()
}

private fun String.isLikelyVideoBinary(): Boolean {
    val path = substringBefore('?').substringBefore('#').lowercase()
    return path.endsWith(".mp4") ||
        path.endsWith(".webm") ||
        path.endsWith(".mov") ||
        path.endsWith(".m4v") ||
        path.endsWith(".mkv") ||
        path.endsWith(".3gp")
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
