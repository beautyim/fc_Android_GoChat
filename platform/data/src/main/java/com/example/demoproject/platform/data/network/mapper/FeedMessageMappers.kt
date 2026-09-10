package com.example.demoproject.platform.data.network.mapper

import com.example.demoproject.platform.data.message.CallConversationIds
import com.example.demoproject.platform.data.message.isVisibleConversationPeer
import com.example.demoproject.platform.data.model.ChatSendLimitNotice
import com.example.demoproject.platform.data.model.Conversation
import com.example.demoproject.platform.data.model.Gender
import com.example.demoproject.platform.data.model.GiftReturnNotice
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.model.MessageStatus
import com.example.demoproject.platform.data.model.MessageType
import com.example.demoproject.platform.data.model.Post
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.data.network.toAssetUrlOrNull
import com.example.demoproject.platform.data.network.toPicUrlOrNull
import com.example.demoproject.platform.data.network.dto.ConversationDto
import com.example.demoproject.platform.data.network.dto.ConversationListResponseDto
import com.example.demoproject.platform.data.network.dto.FeedsListResponseDto
import com.example.demoproject.platform.data.network.dto.MessageDto
import com.example.demoproject.platform.data.network.dto.MessageListResponseDto
import com.example.demoproject.platform.data.network.dto.MsgSendRequestDto
import com.example.demoproject.platform.data.network.dto.PostDto
import com.example.demoproject.platform.data.network.dto.PostPhotoDto
import com.example.demoproject.platform.data.network.dto.UserDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

// ── Post ──

/**
 * Maps a single feed item to a domain [Post]. [author] must already be
 * resolved from the response-level `user_infos` map — callers should use
 * [FeedsListResponseDto.toDomainPosts] which handles that lookup.
 */
fun PostDto.toDomain(author: User): Post = Post(
    id = resolvedId,
    author = author,
    content = content,
    images = mediaItemsForDomain().extractImageUrls(),
    category = when {
        postCategoryInfo != null && postCategoryInfo.label.isNotBlank() -> postCategoryInfo.label
        categoryId > 0 -> categoryId.toString()
        else -> ""
    },
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = isLiked,
    createdAt = (publishTime ?: addTime).toEpochMillis(),
)

/**
 * Resolves every feed item's author from [FeedsListResponseDto.userInfos]
 * and emits the corresponding domain [Post] list. Items whose uid is
 * absent from `user_infos` are silently dropped — this matches the
 * backend contract: a missing user means the account was deleted.
 */
fun FeedsListResponseDto.toDomainPosts(
    now: java.time.LocalDate = java.time.LocalDate.now(),
): List<Post> {
    val authors: Map<String, User> = userInfos.mapValues { it.value.toDomain(now) }
    return list.mapNotNull { item ->
        val author = authors[item.uid.toString()] ?: return@mapNotNull null
        item.toDomain(author)
    }
}

private fun List<PostPhotoDto>.extractImageUrls(): List<String> =
    mapNotNull { photo ->
        when (photo.mediaType) {
            PostPhotoDto.MEDIA_VIDEO -> photo.coverUrl.toPicUrlOrNull() ?: photo.url.toAssetUrlOrNull()
            else -> photo.url.toPicUrlOrNull()
        }
    }

/**
 * YAML samples expose `add_time` as a string containing unix seconds
 * (e.g. `"1776764683"`). Returns millis for the domain layer; `0L` on a
 * missing / unparseable value.
 */
private fun String?.toEpochMillis(): Long {
    if (this.isNullOrBlank()) return 0L
    val seconds = this.toLongOrNull() ?: return 0L
    return seconds * 1000L
}

// ── Message ──

/**
 * Domain mapping for a single message pulled from `/msg/detail-list`.
 * [conversationId] is the peer's uid as a string (our convention); for a
 * received message that's [MessageDto.sendUid], for a sent one it's
 * [MessageDto.targetUid]. The repository passes whichever is appropriate.
 */
fun MessageDto.toDomain(conversationId: String): Message {
    val rawBody = wireBody.trim()
    val isGiftReturnNotice = GiftReturnNotice.isPayload(rawBody)
    val isChatControlNotice = ChatSendLimitNotice.shouldShow(rawBody)
    val resolvedType = if (isGiftReturnNotice || isChatControlNotice) {
        MessageType.System
    } else {
        msgType.toMessageType(wireBody)
    }
    // Media / gift payloads are structured JSON (`image_url`, `voice_url`, …).
    // `previewTextBody()` only unwraps text keys and would wipe those fields to "",
    // so ChatDetail can no longer resolve CDN URLs for peer image/video bubbles.
    val content = when {
        CallConversationIds.isCallHistoryWirePayload(rawBody) -> rawBody
        isChatControlNotice -> rawBody
        isGiftReturnNotice -> GiftReturnNotice.resolveDisplayText(rawBody).orEmpty()
        msgType.isGiftMessageType(rawBody) -> rawBody.ifBlank { rawBody.previewTextBody() }
        resolvedType.keepsStructuredWireBody() -> rawBody
        else -> rawBody.previewTextBody()
    }
    return Message(
        id = mtime.toString(),
        conversationId = conversationId,
        senderId = sendUid.toString(),
        content = content,
        type = resolvedType,
        // /msg/detail-list doesn't expose client-side status; a received
        // message is logically "Delivered" once it hits the server history.
        status = if (isRead) MessageStatus.Read else MessageStatus.Delivered,
        createdAt = mtime,
        translatedText = null,
    )
}

private fun MessageType.keepsStructuredWireBody(): Boolean = when (this) {
    MessageType.Image,
    MessageType.Voice,
    MessageType.Video,
    MessageType.PrivatePhoto,
    MessageType.PrivateVideo -> true
    else -> false
}

/**
 * Combines a conversation row with the peer [UserDto] looked up from
 * the response-level `user_infos` map.
 */
fun ConversationDto.toDomain(peer: User): Conversation = Conversation(
    id = (targetUid.takeIf { it != 0L } ?: chatId).toString(),
    peer = peer,
    lastMessage = latestMessageForPreview()
        ?.toDomain(conversationId = (targetUid.takeIf { it != 0L } ?: chatId).toString()),
    unreadCount = resolvedUnread,
    updatedAt = mtime.takeIf { it != 0L } ?: (latestMessageForPreview()?.mtime ?: 0L),
)

/**
 * Resolves every conversation row's peer user and returns the domain
 * [Conversation] list in response order. If a row's [UserDto] is missing from
 * `user_infos` (partial envelopes, key mismatch, etc.), we still emit the row
 * with [placeholderPeerForConversation] so the client list matches server data.
 */
fun ConversationListResponseDto.toDomainConversations(
    now: java.time.LocalDate = java.time.LocalDate.now(),
): List<Conversation> {
    val peers: Map<String, User> = userInfos.mapValues { it.value.toDomain(now) }
    return list
        .filter { item ->
            val peerUid = item.targetUid.takeIf { it != 0L } ?: item.chatId
            isVisibleConversationPeer(peerUid)
        }
        .map { item ->
        val peerUid = item.targetUid.takeIf { it != 0L } ?: item.chatId
        val rowPeers = item.userInfos.mapValues { it.value.toDomain(now) }
        val peer = rowPeers[peerUid.toString()]
            ?: peers[peerUid.toString()]
            ?: placeholderPeerForConversation(peerUid)
        item.toDomain(peer)
        }
}

private fun ConversationDto.latestMessageForPreview(): MessageDto? =
    buildList {
        lastMessage?.takeIf { it.hasConversationPreviewPayload() }?.let(::add)
        MessageDto(
            sendUid = sendUid,
            targetUid = targetUid,
            chatId = chatId,
            chatType = chatType,
            msgType = msgType,
            msgContent = msgContent,
            mtime = mtime,
        ).takeIf { it.hasConversationPreviewPayload() }?.let(::add)
        addAll(list.filter { it.hasConversationPreviewPayload() })
    }
        .maxByOrNull { it.mtime }

private fun MessageDto.hasConversationPreviewPayload(): Boolean =
    wireBody.isNotBlank() ||
        (mtime != 0L && msgType != MsgSendRequestDto.MSG_TYPE_TEXT)

private fun placeholderPeerForConversation(uid: Long): User = User(
    id = uid.toString(),
    nickname = "",
    avatar = null,
    gender = Gender.Other,
    age = 0,
    bio = "",
    isOnline = false,
    lastActiveAt = 0L,
)

private fun Int.isGiftMessageType(body: String): Boolean = when (this) {
    MsgSendRequestDto.MSG_TYPE_ASK_GIFT -> body.isLikelyGiftPayload()
    else -> this in GIFT_MESSAGE_TYPES
}

private val GIFT_MESSAGE_TYPES = setOf(
    MsgSendRequestDto.MSG_TYPE_GIFT,
    MsgSendRequestDto.MSG_TYPE_LIVE_GIFT,
)

/**
 * Messages arrive interleaved sender/receiver — [currentUserId] is used
 * to compute the right [Message.conversationId] per row (peer's uid,
 * regardless of direction).
 */
fun MessageListResponseDto.toDomainMessages(
    currentUserId: String,
    conversationId: String? = null,
): List<Message> =
    list.map { msg ->
        msg.toDomain(
            conversationId = conversationId
                ?: msg.resolveConversationId(currentUserId),
        )
    }

fun MessageDto.resolveConversationId(currentUserId: String): String {
    val roomConversation = roomId?.takeIf { it > 0L }?.let(CallConversationIds::forRoom)
    if (roomConversation != null) return roomConversation
    // Control notices (101–108) and other server tips commonly use send_uid=0.
    // Prefer chat_id / the non-self target so they land in the peer conversation.
    if (sendUid == 0L) {
        val peerFromTarget = targetUid.takeIf { it != 0L && it.toString() != currentUserId }
        val peerFromChat = chatId.takeIf { it != 0L && it.toString() != currentUserId }
        return (peerFromTarget ?: peerFromChat ?: targetUid.takeIf { it != 0L } ?: chatId)
            .toString()
    }
    val peer = if (sendUid.toString() == currentUserId) {
        (targetUid.takeIf { it != 0L } ?: chatId).toString()
    } else {
        sendUid.toString()
    }
    return peer
}

/**
 * `msg_type` latest mapping:
 *  1 = text, 2 = voice, 3 = image, 5 = gift, 18 = video, 23 = expression,
 *  25 = paid photo, 26 = paid video.
 * Non-media business events (notice, call, recommendation...) are currently
 * rendered as [MessageType.System].
 */
private fun Int.toMessageType(body: String): MessageType = when (this) {
    MsgSendRequestDto.MSG_TYPE_TEXT -> inferTypeFromBody(body) ?: MessageType.Text
    MsgSendRequestDto.MSG_TYPE_VOICE -> MessageType.Voice
    MsgSendRequestDto.MSG_TYPE_IMAGE -> MessageType.Image
    MsgSendRequestDto.MSG_TYPE_GIFT,
    MsgSendRequestDto.MSG_TYPE_LIVE_GIFT -> MessageType.Gift
    MsgSendRequestDto.MSG_TYPE_ASK_GIFT -> {
        if (body.isLikelyGiftPayload()) {
            MessageType.Gift
        } else {
            MessageType.System
        }
    }
    MsgSendRequestDto.MSG_TYPE_VIDEO -> MessageType.Video
    MsgSendRequestDto.MSG_TYPE_EMOJI -> MessageType.Emoji
    MsgSendRequestDto.MSG_TYPE_PRIVATE_PHOTO -> MessageType.PrivatePhoto
    MsgSendRequestDto.MSG_TYPE_PRIVATE_VIDEO -> MessageType.PrivateVideo
    else -> inferTypeFromBody(body)
        ?: inferTextFromPlainOrTextJson(body)
        ?: MessageType.System
}

private fun inferTypeFromBody(body: String): MessageType? {
    val lower = body.lowercase()
    return when {
        "\"voice_url\"" in lower || "\"audio_url\"" in lower || "\"voice\"" in lower -> MessageType.Voice
        // Peer MQTT/HTTP video bodies often use `url` + `cover`/`small_url` (not `video_url`).
        "\"video_url\"" in lower ||
            "\"play_url\"" in lower ||
            "\"cover_url\"" in lower ||
            "\"cover\"" in lower ||
            (hasJsonUrlField(lower) && VIDEO_FILE_HINT.any { it in lower }) -> MessageType.Video
        "\"image_url\"" in lower || "\"small_url\"" in lower || "\"pic_url\"" in lower || "\"thumb_url\"" in lower -> MessageType.Image
        else -> null
    }
}

private fun hasJsonUrlField(lowerBody: String): Boolean =
    "\"url\"" in lowerBody || "\"content\"" in lowerBody || "\"path\"" in lowerBody

private val VIDEO_FILE_HINT = listOf(".mp4", ".mov", ".m3u8", ".webm", ".m4v")

/**
 * `/call/send-msg` and some MQTT envelopes reuse non-catalog `msg_type` values while still
 * carrying plain text or `{"text":...}` payloads. Treat those as chat text so Room + UI
 * (including in-call overlay) classify them as [MessageType.Text] instead of [MessageType.System].
 */
private fun inferTextFromPlainOrTextJson(body: String): MessageType? {
    val t = body.trim()
    if (t.isEmpty() || t.equals("null", ignoreCase = true)) return null
    if (!t.startsWith("{")) return MessageType.Text
    val lower = t.lowercase()
    if (
        "\"voice_url\"" in lower ||
        "\"audio_url\"" in lower ||
        "\"video_url\"" in lower ||
        "\"play_url\"" in lower ||
        "\"image_url\"" in lower ||
        "\"cover_url\"" in lower
    ) {
        return null
    }
    return if (TEXT_JSON_FIELD_HINT.containsMatchIn(t)) MessageType.Text else null
}

private val TEXT_JSON_FIELD_HINT =
    Regex("\"(text|content|msg|message|msg_content|text_content|body|value)\"\\s*:")

private fun String.isLikelyGiftPayload(): Boolean {
    val lower = trim().lowercase()
    if (lower.isEmpty() || !lower.startsWith("{")) return false
    return "\"gift_id\"" in lower ||
        "\"gift_name\"" in lower ||
        "\"gift_info\"" in lower ||
        "\"svga_url\"" in lower ||
        "\"animation_url\"" in lower ||
        "\"lottie_url\"" in lower ||
        "\"number\"" in lower
}



private val previewJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

internal fun String.previewTextBody(): String {
    val trimmed = trim()
    val parsed = runCatching { previewJson.parseToJsonElement(trimmed) }.getOrNull()
    if (parsed is JsonObject) {
        listOf("text", "content", "msg", "message").forEach { key ->
            val text = runCatching { parsed[key]?.jsonPrimitive?.content }.getOrNull()
            if (!text.isNullOrBlank()) return text
        }
        return ""
    }
    val primitiveText = runCatching { parsed?.jsonPrimitive?.content }.getOrNull()
    return primitiveText?.takeIf { it.isNotBlank() } ?: trimmed
}
