package com.example.demoproject.product.chat

import com.example.demoproject.platform.data.message.ChatCallBubbleStatus
import com.example.demoproject.platform.data.message.callBubbleContent
import com.example.demoproject.platform.data.model.ChatSendLimitNotice
import com.example.demoproject.platform.data.model.GiftReturnNotice
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.model.MessageStatus
import com.example.demoproject.platform.data.model.MessageType
import com.example.demoproject.platform.data.model.giftMessageContent
import com.example.demoproject.platform.data.model.giftReturnNoticeText
import com.example.demoproject.platform.data.model.isChatControlNotice
import com.example.demoproject.platform.data.model.toMessageTimelineMillis
import com.example.demoproject.platform.data.network.toChatBinaryUrlOrNull
import com.example.demoproject.platform.data.network.toPicUrlOrNull
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

internal object ChatDetailMessageMapper {
    private const val TIME_SEPARATOR_GAP_MS = 5 * 60 * 1000L

    fun buildListItems(
        ordered: List<Message>,
        currentUserId: String,
        peerName: String,
        hasMore: Boolean,
        profileCard: ChatDetailListItem.ProfileCard?,
        stringResolver: ChatDetailStringResolver,
        giftAssets: ChatGiftAssetIndex = ChatGiftAssetIndex.Empty,
    ): List<ChatDetailListItem> {
        val rows = ArrayList<ChatDetailListItem>(ordered.size + 4)
        if (!hasMore) {
            rows += ChatDetailListItem.SafetyTips
            if (profileCard != null) rows += profileCard
        }
        var previousMillis = 0L
        ordered.forEachIndexed { index, message ->
            val millis = message.createdAt.toMessageTimelineMillis()
            if (index == 0 || millis - previousMillis >= TIME_SEPARATOR_GAP_MS) {
                rows += ChatDetailListItem.TimeSeparator(
                    label = formatSeparatorLabel(millis, stringResolver),
                    // Key on timestamp only — do not tie to message.id. Send success
                    // replaces local_* with server mtime; a per-message key would
                    // remount the separator and break reverseLayout stick-to-bottom.
                    key = "time_$millis",
                )
            }
            rows += ChatDetailListItem.MessageRow(
                message = message.toUi(currentUserId, peerName, stringResolver, giftAssets),
            )
            previousMillis = millis
        }
        return rows
    }

    fun Message.toUi(
        currentUserId: String,
        peerName: String,
        stringResolver: ChatDetailStringResolver,
        giftAssets: ChatGiftAssetIndex = ChatGiftAssetIndex.Empty,
    ): ChatDetailMessageUi {
        val millis = createdAt.toMessageTimelineMillis()
        return ChatDetailMessageUi(
            id = id,
            isMine = senderId == currentUserId,
            status = status.toUiStatus(),
            createdAtMillis = millis,
            timeLabel = formatBubbleTime(millis),
            body = resolveBody(
                isMine = senderId == currentUserId,
                peerName = peerName,
                stringResolver = stringResolver,
                giftAssets = giftAssets,
            ),
        )
    }

    private fun Message.resolveBody(
        isMine: Boolean,
        peerName: String,
        stringResolver: ChatDetailStringResolver,
        giftAssets: ChatGiftAssetIndex,
    ): ChatDetailMessageBody {
        // Control notices must win over call-bubble heuristics: some tip payloads also
        // carry a numeric `type` that would otherwise be misread as a call type.
        if (isChatControlNotice()) {
            val key = ChatSendLimitNotice.keyOf(content)
            return ChatDetailMessageBody.SystemNotice(
                text = stringResolver.controlNotice(key),
            )
        }
        callBubbleContent()?.let { call ->
            val label = when (call.status) {
                ChatCallBubbleStatus.Missed -> stringResolver.callMissed
                ChatCallBubbleStatus.Declined -> stringResolver.callDeclined
                ChatCallBubbleStatus.Cancelled -> stringResolver.callCanceled
                ChatCallBubbleStatus.Connected -> {
                    val seconds = call.durationSeconds ?: 0
                    stringResolver.callDuration(formatDuration(seconds))
                }
            }
            return ChatDetailMessageBody.Call(
                label = label,
                isMissed = call.status == ChatCallBubbleStatus.Missed,
            )
        }
        giftReturnNoticeText()?.let {
            return ChatDetailMessageBody.SystemNotice(text = it)
        }
        if (GiftReturnNotice.isPayload(content)) {
            return ChatDetailMessageBody.SystemNotice(
                text = GiftReturnNotice.resolveDisplayText(content).orEmpty().ifBlank { content },
            )
        }
        return when (type) {
            MessageType.Text -> ChatDetailMessageBody.Text(
                text = content,
                translatedText = translatedText,
            )
            MessageType.Emoji -> ChatDetailMessageBody.Emoji(text = content)
            MessageType.Image -> {
                val media = extractImageDisplay(content)
                ChatDetailMessageBody.Image(
                    url = media.bubbleUrl,
                    previewUrl = media.previewUrl,
                    locked = false,
                )
            }
            MessageType.PrivatePhoto -> {
                val media = extractImageDisplay(content)
                ChatDetailMessageBody.Image(
                    url = media.bubbleUrl,
                    previewUrl = media.previewUrl,
                    locked = !isUnlockedMedia(content),
                )
            }
            MessageType.Video -> {
                val media = extractVideoDisplay(content)
                ChatDetailMessageBody.Video(
                    url = media.bubbleUrl,
                    previewUrl = media.previewUrl,
                    videoUrl = media.videoUrl,
                    durationLabel = extractDurationLabel(content),
                    durationSeconds = media.durationSeconds,
                    locked = false,
                )
            }
            MessageType.PrivateVideo -> {
                val media = extractVideoDisplay(content)
                ChatDetailMessageBody.Video(
                    url = media.bubbleUrl,
                    previewUrl = media.previewUrl,
                    videoUrl = media.videoUrl,
                    durationLabel = extractDurationLabel(content),
                    durationSeconds = media.durationSeconds,
                    locked = !isUnlockedMedia(content),
                )
            }
            MessageType.Gift -> {
                val gift = giftMessageContent()
                val giftTitle = gift?.title.orEmpty()
                val giftId = gift?.giftId ?: 0L
                val animationUrl = gift?.animationUrl.orEmpty().ifBlank {
                    giftAssets.resolveAnimation(giftId = giftId, title = giftTitle)
                }
                ChatDetailMessageBody.Gift(
                    title = giftTitle.ifBlank { stringResolver.giftFallback },
                    iconUrl = gift?.iconUrl.orEmpty().ifBlank {
                        giftAssets.resolveIcon(giftId = giftId, title = giftTitle)
                    },
                    price = extractGiftPrice(content),
                    isRequest = !isMine,
                    peerName = peerName,
                    canPlayAnimation = animationUrl.isNotBlank(),
                )
            }
            MessageType.Voice -> ChatDetailMessageBody.Text(
                text = stringResolver.voiceFallback,
                translatedText = null,
            )
            MessageType.System -> ChatDetailMessageBody.SystemNotice(text = content)
        }
    }

    private fun MessageStatus.toUiStatus(): ChatDetailMessageStatus = when (this) {
        MessageStatus.Sending -> ChatDetailMessageStatus.Sending
        MessageStatus.Failed -> ChatDetailMessageStatus.Failed
        else -> ChatDetailMessageStatus.Sent
    }

    private fun formatSeparatorLabel(millis: Long, stringResolver: ChatDetailStringResolver): String {
        if (millis <= 0L) return ""
        val locale = Locale.getDefault()
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = millis }
        val startOfToday = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val timePart = SimpleDateFormat("h:mm a", locale).format(Date(millis))
            .replace("am", "AM")
            .replace("pm", "PM")
        return when {
            then.timeInMillis >= startOfToday.timeInMillis -> {
                stringResolver.todaySeparator(timePart)
            }
            then.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> {
                "${SimpleDateFormat("MMM d", locale).format(Date(millis))} $timePart"
            }
            else -> {
                "${DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(millis))} $timePart"
            }
        }
    }

    private fun formatBubbleTime(millis: Long): String {
        if (millis <= 0L) return ""
        return SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(millis))
    }

    private fun formatDuration(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val minutes = TimeUnit.SECONDS.toMinutes(safe.toLong())
        val seconds = safe % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}

internal interface ChatDetailStringResolver {
    val callMissed: String
    val callDeclined: String
    val callCanceled: String
    val giftFallback: String
    val voiceFallback: String
    fun callDuration(clock: String): String
    fun todaySeparator(time: String): String
    fun controlNotice(key: Int?): String
    fun freeMessagesLeft(count: Int): String
    fun giftSentLabel(): String
    fun giftRequestLabel(name: String): String
    fun sendGiftAction(): String
    fun onlineLabel(): String
    fun extraPhotos(count: Int): String
}

/** SVGA URL for a gift row: payload first, `gift/config` catalog as fallback. */
internal fun Message.giftAnimationUrl(giftAssets: ChatGiftAssetIndex): String {
    val gift = giftMessageContent() ?: return ""
    return gift.animationUrl.trim().ifBlank {
        giftAssets.resolveAnimation(giftId = gift.giftId, title = gift.title)
    }
}

internal data class ChatMediaDisplay(
    /** CDN URL for the chat bubble (image or video cover). */
    val bubbleUrl: String,
    /** CDN URL for fullscreen image / video cover. */
    val previewUrl: String,
    /** Absolute playback URL for videos; blank for images. */
    val videoUrl: String = "",
    val durationSeconds: Int = 0,
)

/**
 * Image payloads: bubble and preview both prefer `image_url` (清晰大图).
 * Plain relative keys (outbound send) resolve via the pic CDN.
 */
internal fun extractImageDisplay(raw: String): ChatMediaDisplay {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ChatMediaDisplay("", "")
    if (!trimmed.startsWith("{")) {
        val absolute = trimmed.toPicUrlOrNull().orEmpty()
        return ChatMediaDisplay(bubbleUrl = absolute, previewUrl = absolute)
    }
    return runCatching {
        val json = JSONObject(trimmed)
        fun opt(key: String): String = json.optString(key).trim()
        val full = sequenceOf("image_url", "pic_url", "media_url", "url")
            .map(::opt)
            .firstOrNull { it.isNotBlank() && !it.isLikelyVideoPath() }
            .orEmpty()
        val thumb = sequenceOf("small_url", "thumb_url")
            .map(::opt)
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val absolute = (full.ifBlank { thumb }).toPicUrlOrNull().orEmpty()
        ChatMediaDisplay(bubbleUrl = absolute, previewUrl = absolute)
    }.getOrDefault(ChatMediaDisplay("", ""))
}

/**
 * Video payloads: bubble / cover use pic CDN; playback URL uses the asset/binary CDN.
 */
internal fun extractVideoDisplay(raw: String): ChatMediaDisplay {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ChatMediaDisplay("", "")
    if (!trimmed.startsWith("{")) {
        val play = trimmed.toChatBinaryUrlOrNull().orEmpty()
        val cover = trimmed.toPicUrlOrNull().orEmpty()
        return ChatMediaDisplay(
            bubbleUrl = cover,
            previewUrl = cover,
            videoUrl = play,
        )
    }
    return runCatching {
        val json = JSONObject(trimmed)
        fun opt(key: String): String = json.optString(key).trim()
        val cover = sequenceOf("cover_url", "cover", "small_url", "thumb_url", "image_url")
            .map(::opt)
            .firstOrNull { it.isNotBlank() && !it.isLikelyVideoPath() }
            .orEmpty()
        val video = sequenceOf("url", "video_url", "media_url")
            .map(::opt)
            .firstOrNull { it.isNotBlank() && it.isLikelyVideoPath() }
            .orEmpty()
            .ifBlank {
                sequenceOf("url", "video_url", "media_url")
                    .map(::opt)
                    .firstOrNull { it.isNotBlank() }
                    .orEmpty()
            }
        val coverAbsolute = cover.toPicUrlOrNull().orEmpty()
        val videoAbsolute = video.toChatBinaryUrlOrNull().orEmpty()
        val seconds = json.optInt("duration", -1).takeIf { it >= 0 }
            ?: json.optInt("call_duration", -1).takeIf { it >= 0 }
            ?: 0
        ChatMediaDisplay(
            bubbleUrl = coverAbsolute,
            previewUrl = coverAbsolute,
            videoUrl = videoAbsolute,
            durationSeconds = seconds,
        )
    }.getOrDefault(ChatMediaDisplay("", ""))
}

/** @deprecated Prefer [extractImageDisplay] / [extractVideoDisplay]. */
internal fun extractMediaUrl(raw: String): String = extractImageDisplay(raw).previewUrl

private fun String.isLikelyVideoPath(): Boolean {
    val lower = lowercase()
    return lower.endsWith(".mp4") ||
        lower.endsWith(".mov") ||
        lower.endsWith(".m4v") ||
        lower.endsWith(".webm") ||
        lower.endsWith(".mkv")
}

internal fun extractDurationLabel(raw: String): String? {
    val trimmed = raw.trim()
    if (!trimmed.startsWith("{")) return null
    return runCatching {
        val json = JSONObject(trimmed)
        val clock = sequenceOf("duration_text", "total_time", "duration_label")
            .map { json.optString(it).trim() }
            .firstOrNull { it.isNotBlank() }
        if (!clock.isNullOrBlank()) return@runCatching clock
        val seconds = json.optInt("duration", -1).takeIf { it >= 0 }
            ?: json.optInt("call_duration", -1).takeIf { it >= 0 }
        seconds?.let {
            val minutes = it / 60
            val rem = it % 60
            "%02d:%02d".format(minutes, rem)
        }
    }.getOrNull()
}

internal fun extractGiftPrice(raw: String): Int? {
    val trimmed = raw.trim()
    if (!trimmed.startsWith("{")) return null
    return runCatching {
        val json = JSONObject(trimmed)
        val nested = json.optJSONObject("gift_info") ?: json.optJSONObject("gift")
        sequenceOf(
            nested?.optInt("price", -1) ?: -1,
            nested?.optInt("coin", -1) ?: -1,
            json.optInt("price", -1),
            json.optInt("coin", -1),
            json.optInt("gold", -1),
        ).firstOrNull { it >= 0 }
    }.getOrNull()
}

internal fun isUnlockedMedia(raw: String): Boolean {
    val trimmed = raw.trim()
    if (!trimmed.startsWith("{")) return true
    return runCatching {
        val json = JSONObject(trimmed)
        when {
            json.has("is_unlock") ->
                json.optBoolean("is_unlock", false) || json.optInt("is_unlock", 0) == 1
            json.has("unlocked") ->
                json.optBoolean("unlocked", false) || json.optInt("unlocked", 0) == 1
            json.has("is_lock") -> json.optInt("is_lock", 1) == 0
            json.has("locked") ->
                !json.optBoolean("locked", true) && json.optInt("locked", 1) == 0
            else -> false
        }
    }.getOrDefault(false)
}

/**
 * Locked private-photo / private-video payload fields used by the unlock sheet.
 * Returns null when [raw] is not a JSON object or has no usable [media_id].
 */
internal data class PrivateMediaUnlockPayload(
    val mediaId: Long,
    val price: Int,
)

internal fun extractPrivateMediaUnlockPayload(raw: String): PrivateMediaUnlockPayload? {
    val trimmed = raw.trim()
    if (!trimmed.startsWith("{")) return null
    return runCatching {
        val json = JSONObject(trimmed)
        val mediaId = when {
            json.has("media_id") -> json.optLong("media_id", 0L)
            else -> 0L
        }
        if (mediaId <= 0L) return@runCatching null
        val price = sequenceOf("view_price", "price", "coin", "gold")
            .map { json.optInt(it, -1) }
            .firstOrNull { it >= 0 }
            ?: 0
        PrivateMediaUnlockPayload(mediaId = mediaId, price = price)
    }.getOrNull()
}

internal fun countryCodeToFlagEmoji(code: String?): String {
    val normalized = code?.trim()?.uppercase().orEmpty()
    if (normalized.length != 2 || !normalized.all { it in 'A'..'Z' }) return ""
    val first = 0x1F1E6 + (normalized[0].code - 'A'.code)
    val second = 0x1F1E6 + (normalized[1].code - 'A'.code)
    return String(Character.toChars(first)) + String(Character.toChars(second))
}
