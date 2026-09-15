package com.example.demoproject.platform.data.model

import com.example.demoproject.platform.data.message.CallConversationIds
import com.example.demoproject.platform.data.message.callBubbleContent
import com.example.demoproject.platform.data.network.toAssetUrlOrNull
import org.json.JSONObject

/**
 * Backend system notice tied to a previously sent gift (MQTT `msg_type=6`).
 *
 * Known variants share the same JSON shape (`content` + `key` + `gift_name`) and must
 * both render as a centered system hint rather than a gift bubble:
 *  - `key=149`: the recipient has received/accepted the gift (e.g. "She has received your gift").
 *  - `key=150`: the gift was returned because the recipient didn't reply in time.
 */
object GiftReturnNotice {
    const val KEY_RECEIVED: Int = 149
    const val KEY_RETURNED: Int = 150
    private val KEYS = setOf(KEY_RECEIVED, KEY_RETURNED)

    private val NORMALIZED_RETURN_TEXT_REGEX =
        Regex("""returned\.?\s*$""", RegexOption.IGNORE_CASE)
    private val NORMALIZED_RECEIVED_TEXT_REGEX =
        // Tolerates backend typos like "receieved" while still requiring the "your gift" tail.
        Regex("""rece\w*\s+your\s+gift\.?\s*$""", RegexOption.IGNORE_CASE)
    /** Quoted gift title embedded in returned-notice display text, e.g. `"Meteor Shower" returned.` */
    private val QUOTED_GIFT_NAME_REGEX =
        Regex("""["“]([^"”]+)["”]\s*returned\.?\s*$""", RegexOption.IGNORE_CASE)

    /**
     * True for both the original JSON envelope and the already-normalized display text
     * (e.g. read back from [ConversationEntity.lastMsgText], which is persisted as plain
     * text rather than the raw JSON). Mirrors [VipSendFailureNotice.isPayload]'s handling
     * of pre-resolved notice text so conversation-list previews don't regress to
     * `MessageType.Text`/`Gift` once the JSON envelope has already been unwrapped.
     */
    fun isPayload(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return false
        return resolveDisplayText(trimmed) != null || isNormalizedNoticeText(trimmed)
    }

    /**
     * True only for coin-refund notices (`key=150` / "... returned."), not for gift-received
     * acknowledgements (`key=149`). Used to trigger an account-balance refresh after the
     * backend returns coins for an unanswered gift.
     */
    fun isCoinsReturnedPayload(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return false
        if (resolveDisplayTextForKey(trimmed, KEY_RETURNED) != null) return true
        return isNormalizedReturnedNoticeText(trimmed)
    }

    fun resolveDisplayText(raw: String): String? {
        return resolveFromJson(raw.trim()) ?: resolveFromNestedContent(raw.trim())
    }

    /**
     * Gift title used to pair this notice with the outbound gift bubble that triggered it.
     * Prefers the JSON `gift_name` field; falls back to the quoted name in returned display text.
     * Received-ack display text (`key=149`) usually has no recoverable name.
     */
    fun resolveGiftName(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        resolveGiftNameFromJson(trimmed)?.let { return it }
        if (trimmed.startsWith("{")) {
            val nested = runCatching { JSONObject(trimmed).optString("content") }.getOrNull()
                ?.trim()
                ?.takeIf { it.startsWith("{") }
            if (nested != null) {
                resolveGiftNameFromJson(nested)?.let { return it }
            }
        }
        return QUOTED_GIFT_NAME_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    /**
     * True when [raw] is already the plain display text resolved from this notice (no longer
     * the original JSON envelope), e.g. after a first pass through [resolveDisplayText]. Used to
     * re-recognize already-normalized rows read back from local storage.
     */
    fun isNormalizedNoticeText(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || trimmed.startsWith("{")) return false
        if (CallConversationIds.isCallHistoryWirePayload(trimmed)) return false
        return NORMALIZED_RETURN_TEXT_REGEX.containsMatchIn(trimmed) ||
            NORMALIZED_RECEIVED_TEXT_REGEX.containsMatchIn(trimmed)
    }

    fun isNormalizedReturnedNoticeText(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || trimmed.startsWith("{")) return false
        if (CallConversationIds.isCallHistoryWirePayload(trimmed)) return false
        return NORMALIZED_RETURN_TEXT_REGEX.containsMatchIn(trimmed)
    }

    private fun resolveDisplayTextForKey(raw: String, key: Int): String? {
        return resolveFromJson(raw, allowedKeys = setOf(key))
            ?: resolveFromNestedContent(raw, allowedKeys = setOf(key))
    }

    private fun resolveFromNestedContent(
        raw: String,
        allowedKeys: Set<Int> = KEYS,
    ): String? {
        if (!raw.startsWith("{")) return null
        val nested = runCatching { JSONObject(raw).optString("content") }.getOrNull()
            ?.trim()
            ?.takeIf { it.startsWith("{") }
            ?: return null
        return resolveFromJson(nested, allowedKeys = allowedKeys)
    }

    private fun resolveFromJson(
        raw: String,
        allowedKeys: Set<Int> = KEYS,
    ): String? {
        if (!raw.startsWith("{")) return null
        return runCatching {
            val json = JSONObject(raw)
            if (readKey(json) !in allowedKeys) return@runCatching null
            if (json.optString("gift_name").isBlank()) return@runCatching null
            json.optString("content").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun resolveGiftNameFromJson(raw: String): String? {
        if (!raw.startsWith("{")) return null
        return runCatching {
            val json = JSONObject(raw)
            if (readKey(json) !in KEYS) return@runCatching null
            json.optString("gift_name").trim().takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun readKey(json: JSONObject): Int {
        if (!json.has("key")) return 0
        return when (val value = json.opt("key")) {
            is Number -> value.toInt()
            else -> value?.toString()?.toIntOrNull() ?: 0
        }
    }
}

fun Message.isGiftReturnNotice(): Boolean {
    if (CallConversationIds.isCallHistoryWirePayload(content)) return false
    if (callBubbleContent() != null) return false
    if (GiftReturnNotice.resolveDisplayText(content) != null) return true
    return type == MessageType.System && GiftReturnNotice.isNormalizedNoticeText(content)
}

/** True when this row is a gift coin-refund notice (`key=150`), not a gift-received ack. */
fun Message.isGiftCoinsReturnedNotice(): Boolean {
    if (CallConversationIds.isCallHistoryWirePayload(content)) return false
    if (callBubbleContent() != null) return false
    if (GiftReturnNotice.isCoinsReturnedPayload(content)) return true
    return type == MessageType.System && GiftReturnNotice.isNormalizedReturnedNoticeText(content)
}

fun Message.giftReturnNoticeText(): String? {
    if (!isGiftReturnNotice()) return null
    return GiftReturnNotice.resolveDisplayText(content) ?: content.trim()
}

/** Gift title for pairing this notice with its outbound gift bubble, when recoverable. */
fun Message.giftReturnNoticeGiftName(): String? {
    if (!isGiftReturnNotice()) return null
    return GiftReturnNotice.resolveGiftName(content)
}

data class GiftMessageContent(
    val giftId: Long,
    val title: String,
    val iconUrl: String,
    val animationUrl: String,
    val count: Int,
)

fun Message.giftMessageContent(): GiftMessageContent? {
    if (type != MessageType.Gift) return null
    if (VipSendFailureNotice.isPayload(content)) return null
    if (GiftReturnNotice.isPayload(content)) return null
    return GiftMessageContentCodec.parse(content)
}

object GiftMessageContentCodec {
    fun parse(raw: String): GiftMessageContent? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("{")) {
            return parseJson(trimmed)
        }
        return GiftMessageContent(
            giftId = 0L,
            title = trimmed,
            iconUrl = "",
            animationUrl = "",
            count = 1,
        )
    }

    fun encode(gift: Gift, count: Int = 1): String =
        JSONObject()
            .put("gift_id", gift.id)
            .put("title", gift.title)
            .put("icon", gift.iconUrl)
            .put("svga_url", gift.svgaUrl)
            .put("number", count.coerceAtLeast(1))
            .toString()

    private fun parseJson(raw: String): GiftMessageContent? =
        runCatching {
            if (GiftReturnNotice.isPayload(raw)) return@runCatching null
            val json = JSONObject(raw)
            val nested = json.optJSONObject("gift_info")
                ?: json.optJSONObject("gift")
            val source = nested ?: json
            val title = sequenceOf(
                source.optString("title"),
                source.optString("gift_name"),
                source.optString("name"),
                json.optString("title"),
                json.optString("gift_name"),
            ).firstOrNull { it.isNotBlank() }.orEmpty()
            if (title.isBlank() && source.optLong("gift_id", 0L) == 0L) return@runCatching null
            GiftMessageContent(
                giftId = source.optLong("gift_id", json.optLong("gift_id", 0L)),
                title = title.ifBlank { source.optLong("gift_id", 0L).toString() },
                // Gift payloads carry the same relative asset keys as `gift/config`
                // (e.g. `s/gift/xxx.png`), so they need the CDN host to be loadable.
                iconUrl = sequenceOf(
                    source.optString("icon"),
                    source.optString("icon_url"),
                    json.optString("icon"),
                ).firstOrNull { it.isNotBlank() }.toAssetUrlOrNull().orEmpty(),
                animationUrl = sequenceOf(
                    source.optString("svga_url"),
                    source.optString("animation_url"),
                    source.optString("lottie_url"),
                    json.optString("svga_url"),
                ).firstOrNull { it.isNotBlank() }.toAssetUrlOrNull().orEmpty(),
                count = sequenceOf(
                    source.optInt("number", 0),
                    source.optInt("count", 0),
                    json.optInt("number", 0),
                    json.optInt("count", 0),
                ).firstOrNull { it > 0 } ?: 1,
            )
        }.getOrNull()
}
