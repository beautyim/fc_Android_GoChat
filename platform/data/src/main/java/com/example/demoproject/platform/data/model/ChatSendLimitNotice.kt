package com.example.demoproject.platform.data.model

import org.json.JSONObject

/**
 * MQTT-backed chat control notices.
 *
 * Keys `101..108` are user-visible centered system hints whose copy comes
 * entirely from app-localized resources:
 *  - `101..106`: session-state changes (blocked / deleted / banned / forbidden).
 *  - `107`: the send-limit was hit (see [KEY_SHOW_LIMIT]).
 *  - `108`: the peer replied, lifting a prior `107` limit (see [KEY_SEND_LIMIT_LIFTED]).
 */
object ChatSendLimitNotice {
    const val KEY_PEER_BLOCKED_BY_ME: Int = 101
    const val KEY_BLOCKED_BY_PEER: Int = 102
    const val KEY_DELETED_BY_PEER: Int = 103
    const val KEY_SELF_BANNED: Int = 104
    const val KEY_PEER_BANNED: Int = 105
    const val KEY_MESSAGING_FORBIDDEN: Int = 106
    const val KEY_SHOW_LIMIT: Int = 107
    const val KEY_SEND_LIMIT_LIFTED: Int = 108

    fun keyOf(raw: String): Int? = findKey(raw.trim(), depth = 0)

    fun shouldShow(raw: String): Boolean = keyOf(raw) in VISIBLE_KEYS

    fun isSendLimit(raw: String): Boolean = keyOf(raw) == KEY_SHOW_LIMIT

    /** True for the `108` notice that fires once the peer replies, restoring send ability. */
    fun isSendLimitLifted(raw: String): Boolean = keyOf(raw) == KEY_SEND_LIMIT_LIFTED

    private fun findKey(raw: String, depth: Int): Int? {
        if (raw.isBlank() || !raw.startsWith("{") || depth > MAX_NESTING_DEPTH) return null
        return runCatching {
            val json = JSONObject(raw)
            json.readLenientInt("key")
                ?.takeIf { it in VISIBLE_KEYS }
                ?.let { return@runCatching it }
            NESTED_FIELDS.firstNotNullOfOrNull { field ->
                when (val nested = json.opt(field)) {
                    is JSONObject -> findKey(nested.toString(), depth + 1)
                    is Number -> nested.toInt().takeIf { it in VISIBLE_KEYS }
                    is String -> {
                        val trimmed = nested.trim()
                        trimmed.toIntOrNull()?.takeIf { it in VISIBLE_KEYS }
                            ?: findKey(trimmed, depth + 1)
                    }
                    else -> null
                }
            }
        }.getOrNull()
    }

    private fun JSONObject.readLenientInt(name: String): Int? {
        if (!has(name) || isNull(name)) return null
        return when (val value = opt(name)) {
            is Number -> value.toInt()
            else -> value?.toString()?.trim()?.toIntOrNull()
        }
    }

    private val VISIBLE_KEYS = setOf(
        KEY_PEER_BLOCKED_BY_ME,
        KEY_BLOCKED_BY_PEER,
        KEY_DELETED_BY_PEER,
        KEY_SELF_BANNED,
        KEY_PEER_BANNED,
        KEY_MESSAGING_FORBIDDEN,
        KEY_SHOW_LIMIT,
        KEY_SEND_LIMIT_LIFTED,
    )
    private const val MAX_NESTING_DEPTH = 4
    private val NESTED_FIELDS = listOf("data", "payload", "content", "msg_content", "body")
}

fun Message.isChatSendLimitNotice(): Boolean =
    ChatSendLimitNotice.isSendLimit(content)

fun Message.isChatControlNotice(): Boolean =
    ChatSendLimitNotice.shouldShow(content)
