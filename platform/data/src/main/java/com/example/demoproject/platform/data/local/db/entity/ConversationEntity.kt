package com.example.demoproject.platform.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import com.example.demoproject.platform.data.model.ChatSendLimitNotice
import com.example.demoproject.platform.data.model.Conversation
import com.example.demoproject.platform.data.model.GiftReturnNotice
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.model.MessageStatus
import com.example.demoproject.platform.data.model.MessageType
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.data.model.Gender
import com.example.demoproject.platform.data.model.VipSendFailureNotice
import com.example.demoproject.platform.data.network.dto.MsgSendRequestDto

@Entity(
    tableName = "conversations",
    primaryKeys = ["ownerUserId", "id"],
    indices = [
        Index(value = ["ownerUserId", "updatedAt"], name = "idx_conversations_owner_updated_at"),
        Index(value = ["ownerUserId", "isPinned", "updatedAt"], name = "idx_conversations_owner_pin_updated"),
    ],
)
data class ConversationEntity(
    /** Logged-in account uid as decimal string. */
    val ownerUserId: String,
    /** Peer uid as decimal string — matches [Conversation.id]. */
    val id: String,
    val peerId: Long,
    val peerNickname: String,
    val peerAvatar: String?,
    val peerIsOnline: Boolean,
    val lastMsgText: String,
    val lastMsgType: Int,
    val updatedAt: Long,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isMuted: Boolean = false,
)

fun ConversationEntity.toDomain(): Conversation {
    val peer = User(
        id = id,
        nickname = peerNickname,
        avatar = peerAvatar,
        gender = Gender.Other,
        age = 0,
        bio = "",
        isOnline = peerIsOnline,
        lastActiveAt = 0L,
    )
    val lastMessage = lastMessagePreviewOrNull()
    return Conversation(
        id = id,
        peer = peer,
        lastMessage = lastMessage,
        unreadCount = unreadCount,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isMuted = isMuted,
    )
}

internal fun ConversationEntity.lastMessagePreviewOrNull(): Message? {
    if (lastMsgText.isBlank() && lastMsgType == MsgSendRequestDto.MSG_TYPE_TEXT) return null
    return Message(
        id = "${id}_preview",
        conversationId = id,
        senderId = "",
        content = lastMsgText,
        type = lastMsgType.toMessageTypeFromBackend(lastMsgText),
        status = MessageStatus.Sent,
        createdAt = updatedAt,
        translatedText = null,
    )
}

private fun Int.toMessageTypeFromBackend(body: String): MessageType = when {
    ChatSendLimitNotice.shouldShow(body) -> MessageType.System
    GiftReturnNotice.isPayload(body) -> MessageType.System
    VipSendFailureNotice.isPayload(body) -> MessageType.System
    else -> when (this) {
    MsgSendRequestDto.MSG_TYPE_TEXT -> MessageType.Text
    MsgSendRequestDto.MSG_TYPE_VOICE -> MessageType.Voice
    MsgSendRequestDto.MSG_TYPE_IMAGE -> MessageType.Image
    MsgSendRequestDto.MSG_TYPE_GIFT,
    MsgSendRequestDto.MSG_TYPE_LIVE_GIFT -> MessageType.Gift
    MsgSendRequestDto.MSG_TYPE_ASK_GIFT -> {
        if (body.isLikelyGiftPayload()) MessageType.Gift else MessageType.System
    }
    MsgSendRequestDto.MSG_TYPE_VIDEO -> MessageType.Video
    MsgSendRequestDto.MSG_TYPE_EMOJI -> MessageType.Emoji
    MsgSendRequestDto.MSG_TYPE_PRIVATE_PHOTO -> MessageType.PrivatePhoto
    MsgSendRequestDto.MSG_TYPE_PRIVATE_VIDEO -> MessageType.PrivateVideo
    else -> MessageType.System
}
}

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


fun Conversation.toEntity(ownerUserId: String): ConversationEntity {
    val peerLong = id.toLongOrNull() ?: peer.id.toLongOrNull() ?: 0L
    return ConversationEntity(
        ownerUserId = ownerUserId,
        id = id,
        peerId = peerLong,
        peerNickname = peer.nickname,
        peerAvatar = peer.avatar,
        peerIsOnline = peer.isOnline,
        lastMsgText = lastMessage?.content.orEmpty(),
        lastMsgType = lastMessage?.type?.toBackendMsgType() ?: MsgSendRequestDto.MSG_TYPE_TEXT,
        updatedAt = updatedAt,
        unreadCount = unreadCount,
        isPinned = isPinned,
        isMuted = isMuted,
    )
}

private fun MessageType.toBackendMsgType(): Int = when (this) {
    MessageType.Text -> MsgSendRequestDto.MSG_TYPE_TEXT
    MessageType.Voice -> MsgSendRequestDto.MSG_TYPE_VOICE
    MessageType.Image -> MsgSendRequestDto.MSG_TYPE_IMAGE
    MessageType.Gift -> MsgSendRequestDto.MSG_TYPE_GIFT
    MessageType.Video -> MsgSendRequestDto.MSG_TYPE_VIDEO
    MessageType.Emoji -> MsgSendRequestDto.MSG_TYPE_EMOJI
    MessageType.PrivatePhoto -> MsgSendRequestDto.MSG_TYPE_PRIVATE_PHOTO
    MessageType.PrivateVideo -> MsgSendRequestDto.MSG_TYPE_PRIVATE_VIDEO
    MessageType.System -> MsgSendRequestDto.MSG_TYPE_TEXT
}
