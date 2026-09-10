package com.example.demoproject.platform.data.model

import org.json.JSONObject

/** Backend system notice indicating send action requires VIP. */
object VipSendFailureNotice {
    private val REQUIRED_KEYWORDS = listOf("vip")
    private val ACTION_KEYWORDS = listOf("send", "message", "chat")
    private val RESULT_KEYWORDS = listOf("fail", "failed", "limit", "subscribe", "unlock", "used up")

    fun isPayload(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return false
        return resolveFromJson(trimmed) || isNormalizedNoticeText(trimmed)
    }

    private fun resolveFromJson(raw: String): Boolean {
        if (!raw.startsWith("{")) return false
        return runCatching {
            val json = JSONObject(raw)
            val nested = json.optString("content")
                .trim()
                .takeIf { it.startsWith("{") }
            if (nested != null && nested != raw && resolveFromJson(nested)) return@runCatching true

            val text = buildString {
                append(json.optString("title"))
                append(' ')
                append(json.optString("subtitle"))
                append(' ')
                append(json.optString("sub_title"))
                append(' ')
                append(json.optString("content"))
                append(' ')
                append(json.optString("text"))
                append(' ')
                append(json.optString("message"))
                append(' ')
                append(json.optString("reason"))
                append(' ')
                append(json.optString("desc"))
                append(' ')
                append(json.optString("type"))
                append(' ')
                append(json.optString("notice_type"))
                append(' ')
                append(json.optString("event"))
            }
            matchesNoticeText(text)
        }.getOrDefault(false)
    }

    private fun isNormalizedNoticeText(raw: String): Boolean {
        if (raw.startsWith("{")) return false
        return matchesNoticeText(raw)
    }

    private fun matchesNoticeText(raw: String): Boolean {
        val text = raw.lowercase()
        val hasRequired = REQUIRED_KEYWORDS.any { text.contains(it) } ||
            (
                text.contains("free") &&
                    text.contains("message") &&
                    (text.contains("used up") || text.contains("limit"))
                )
        val hasAction = ACTION_KEYWORDS.any { text.contains(it) }
        val hasResult = RESULT_KEYWORDS.any { text.contains(it) }
        return hasRequired && hasAction && hasResult
    }
}
