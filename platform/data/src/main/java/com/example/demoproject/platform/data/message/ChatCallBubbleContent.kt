package com.example.demoproject.platform.data.message

import com.example.demoproject.platform.data.model.CallRoom
import com.example.demoproject.platform.data.model.Message
import org.json.JSONObject

enum class ChatCallBubbleType {
    Video,
    Voice,
}

enum class ChatCallBubbleStatus {
    Connected,
    Missed,
    Declined,
    Cancelled,
}

data class ChatCallBubbleContent(
    val callType: ChatCallBubbleType,
    val status: ChatCallBubbleStatus,
    val durationSeconds: Int? = null,
)

fun Message.callBubbleContent(): ChatCallBubbleContent? =
    content.parseCallBubbleContent()

/**
 * Resolves whether a raw call JSON payload describes a video or voice call.
 *
 * The backend sometimes encodes `call_type`/`type` as a JSON number (`2`) and sometimes as a
 * numeric string (`"2"`); [JSONObject.optInt] tolerates both, so this must stay the single
 * source of truth for call-type detection instead of ad-hoc string/regex matching elsewhere
 * (e.g. the chat list preview), which would otherwise misclassify voice calls as video calls.
 */
fun String.parseCallType(): ChatCallBubbleType? {
    val trimmed = trim()
    if (!trimmed.startsWith("{")) return null
    val lower = trimmed.lowercase()
    if ("\"channel\"" in lower && "\"token\"" in lower && "\"uid\"" in lower) return null
    val json = runCatching { JSONObject(trimmed) }.getOrNull() ?: return null
    return json.resolveCallType()
}

private fun JSONObject.resolveCallType(): ChatCallBubbleType? =
    when (optInt("call_type", optInt("type", -1))) {
        CallRoom.CALL_TYPE_VIDEO -> ChatCallBubbleType.Video
        CallRoom.CALL_TYPE_VOICE -> ChatCallBubbleType.Voice
        else -> null
    }

fun String.parseCallBubbleContent(): ChatCallBubbleContent? {
    val trimmed = trim()
    if (trimmed.isEmpty()) return null
    if (!trimmed.startsWith("{")) {
        return parseCallBubbleFromPlainText(trimmed, ChatCallBubbleType.Video)
    }
    val json = runCatching { JSONObject(trimmed) }.getOrNull() ?: return null
    val lower = trimmed.lowercase()
    if ("\"channel\"" in lower && "\"token\"" in lower && "\"uid\"" in lower) return null
    val callType = json.resolveCallType() ?: return null
    val callDesc = json.optString("call_desc").trim()
    val totalTime = json.optString("total_time").trim()
    // The backend sometimes sends `duration`/`call_duration` as a plain second count (63) and
    // sometimes as a clock-formatted string ("00:28"), so both encodings must be tried.
    val durationSeconds = json.optInt("duration", -1).takeIf { it >= 0 }
        ?: json.optInt("call_duration", -1).takeIf { it >= 0 }
        ?: json.optString("duration").trim().parseDurationSeconds()
        ?: json.optString("call_duration").trim().parseDurationSeconds()
        ?: totalTime.parseDurationSeconds()
        ?: callDesc.parseDurationSeconds()
    val callStatus = json.optInt("call_status", -1)
    val statusCode = json.optInt("status", -1)
    return when {
        callStatus == 3 || callDesc.containsMissedHint() -> {
            ChatCallBubbleContent(callType = callType, status = ChatCallBubbleStatus.Missed)
        }
        callStatus == 4 || callDesc.containsCancelledHint() -> {
            ChatCallBubbleContent(callType = callType, status = ChatCallBubbleStatus.Cancelled)
        }
        callDesc.containsDeclinedHint() -> {
            ChatCallBubbleContent(callType = callType, status = ChatCallBubbleStatus.Declined)
        }
        durationSeconds != null && durationSeconds > 0 -> {
            ChatCallBubbleContent(
                callType = callType,
                status = ChatCallBubbleStatus.Connected,
                durationSeconds = durationSeconds,
            )
        }
        totalTime.isNotBlank() || callDesc.isDurationLike() -> {
            ChatCallBubbleContent(
                callType = callType,
                status = ChatCallBubbleStatus.Connected,
                durationSeconds = durationSeconds ?: 0,
            )
        }
        statusCode == 7 -> {
            ChatCallBubbleContent(
                callType = callType,
                status = ChatCallBubbleStatus.Connected,
                durationSeconds = durationSeconds,
            )
        }
        callStatus == 0 && callDesc.isNotBlank() -> {
            parseCallBubbleFromPlainText(callDesc, callType)
        }
        callStatus >= 0 || callDesc.isNotBlank() -> {
            ChatCallBubbleContent(callType = callType, status = ChatCallBubbleStatus.Cancelled)
        }
        else -> null
    }
}

private fun parseCallBubbleFromPlainText(text: String, callType: ChatCallBubbleType): ChatCallBubbleContent? {
    val normalized = text.trim()
    if (normalized.isEmpty()) return null
    val lower = normalized.lowercase()
    return when {
        lower.contains("call duration") -> {
            val duration = DURATION_LABEL_REGEX.find(normalized)?.groupValues?.getOrNull(1)
                ?.parseClockDurationSeconds()
            ChatCallBubbleContent(
                callType = callType,
                status = ChatCallBubbleStatus.Connected,
                durationSeconds = duration,
            )
        }
        lower.contains("miss") -> ChatCallBubbleContent(callType = callType, status = ChatCallBubbleStatus.Missed)
        lower.contains("declin") || lower.contains("reject") -> {
            ChatCallBubbleContent(callType = callType, status = ChatCallBubbleStatus.Declined)
        }
        lower.contains("cancel") -> ChatCallBubbleContent(callType = callType, status = ChatCallBubbleStatus.Cancelled)
        normalized.parseClockDurationSeconds() != null -> {
            ChatCallBubbleContent(
                callType = callType,
                status = ChatCallBubbleStatus.Connected,
                durationSeconds = normalized.parseClockDurationSeconds(),
            )
        }
        else -> null
    }
}

private fun String.containsMissedHint(): Boolean {
    val lower = lowercase()
    return "miss" in lower
}

private fun String.containsDeclinedHint(): Boolean {
    val lower = lowercase()
    return "declin" in lower || "reject" in lower
}

private fun String.containsCancelledHint(): Boolean {
    val lower = lowercase()
    return "cancel" in lower
}

private fun String.isDurationLike(): Boolean =
    parseClockDurationSeconds() != null || DURATION_TOKEN_REGEX.containsMatchIn(this)

private fun String.parseDurationSeconds(): Int? =
    parseClockDurationSeconds()
        ?: DURATION_TOKEN_REGEX.find(this)?.let { match ->
            val minutes = match.groupValues[1].toIntOrNull() ?: return@let null
            val seconds = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            minutes * 60 + seconds
        }

private fun String.parseClockDurationSeconds(): Int? {
    val match = CLOCK_DURATION_REGEX.matchEntire(trim()) ?: return null
    val minutes = match.groupValues[1].toIntOrNull() ?: return null
    val seconds = match.groupValues[2].toIntOrNull() ?: return null
    return minutes * 60 + seconds
}

private val CLOCK_DURATION_REGEX = Regex("""^(\d{1,2}):(\d{2})$""")
private val DURATION_LABEL_REGEX =
    Regex("""call duration:\s*(\d{1,2}:\d{2})""", RegexOption.IGNORE_CASE)
private val DURATION_TOKEN_REGEX =
    Regex("""(\d+)\s*m(?:in(?:ute)?s?)?(?:\s*(\d+)\s*s(?:ec(?:ond)?s?)?)?""", RegexOption.IGNORE_CASE)
