package com.example.demoproject.platform.data.message

import org.json.JSONObject

/**
 * Sets `is_unlock=true` and merges optional CDN paths into a private media
 * `msg_content` JSON blob. Non-JSON content is left unchanged.
 */
fun String.withPrivateMediaUnlocked(
    playUrl: String? = null,
    imageUrl: String? = null,
    coverUrl: String? = null,
): String {
    val trimmed = trim()
    if (!trimmed.startsWith("{")) return this
    return runCatching {
        val json = JSONObject(trimmed)
        json.put("is_unlock", true)
        playUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { json.put("url", it) }
        imageUrl?.trim()?.takeIf { it.isNotEmpty() }?.let {
            json.put("image_url", it)
            if (!json.has("url") || json.optString("url").isBlank()) {
                json.put("url", it)
            }
        }
        coverUrl?.trim()?.takeIf { it.isNotEmpty() }?.let {
            json.put("cover_url", it)
            if (!json.has("small_url") || json.optString("small_url").isBlank()) {
                json.put("small_url", it)
            }
        }
        json.toString()
    }.getOrDefault(this)
}
