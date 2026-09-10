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
                    key = "time_$millis}_${message.id}",
                )
            }
            rows += ChatDetailListItem.MessageRow(
                message = message.toUi(currentUserId, peerName, stringResolver),
            )
            previousMillis = millis
        }
        return rows
    }

    fun Message.toUi(
        currentUserId: String,
        peerName: String,
        stringResolver: ChatDetailStringResolver,
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
            ),
        )
    }

    private fun Message.resolveBody(
        isMine: Boolean,
        peerName: String,
        stringResolver: ChatDetailStringResolver,
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
            MessageType.Image -> ChatDetailMessageBody.Image(
                url = extractMediaUrl(content),
                locked = false,
            )
            MessageType.PrivatePhoto -> ChatDetailMessageBody.Image(
                url = extractMediaUrl(content),
                locked = !isUnlockedMedia(content),
            )
            MessageType.Video -> ChatDetailMessageBody.Video(
                url = extractMediaUrl(content),
                durationLabel = extractDurationLabel(content),
                locked = false,
            )
            MessageType.PrivateVideo -> ChatDetailMessageBody.Video(
                url = extractMediaUrl(content),
                durationLabel = extractDurationLabel(content),
                locked = !isUnlockedMedia(content),
            )
            MessageType.Gift -> {
                val gift = giftMessageContent()
                ChatDetailMessageBody.Gift(
                    title = gift?.title.orEmpty().ifBlank { stringResolver.giftFallback },
                    iconUrl = gift?.iconUrl.orEmpty(),
                    price = extractGiftPrice(content),
                    isRequest = !isMine,
                    peerName = peerName,
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

internal fun extractMediaUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    if (!trimmed.startsWith("{")) return trimmed
    return runCatching {
        val json = JSONObject(trimmed)
        sequenceOf(
            "url",
            "image_url",
            "pic_url",
            "thumb_url",
            "small_url",
            "video_url",
            "media_url",
            "cover_url",
        ).map { json.optString(it).trim() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
    }.getOrDefault("")
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
            json.has("unlocked") -> json.optBoolean("unlocked", false) || json.optInt("unlocked", 0) == 1
            json.has("is_lock") -> json.optInt("is_lock", 1) == 0
            json.has("locked") -> !json.optBoolean("locked", true) && json.optInt("locked", 1) == 0
            else -> false
        }
    }.getOrDefault(false)
}

internal fun countryCodeToFlagEmoji(code: String?): String {
    val normalized = code?.trim()?.uppercase().orEmpty()
    if (normalized.length != 2 || !normalized.all { it in 'A'..'Z' }) return ""
    val first = 0x1F1E6 + (normalized[0].code - 'A'.code)
    val second = 0x1F1E6 + (normalized[1].code - 'A'.code)
    return String(Character.toChars(first)) + String(Character.toChars(second))
}
