package com.example.demoproject.platform.data.local.db.entity

import androidx.room.Index
import androidx.room.Entity
import com.example.demoproject.platform.data.model.ChatSendLimitNotice
import com.example.demoproject.platform.data.model.GiftReturnNotice
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.model.MessageStatus
import com.example.demoproject.platform.data.model.MessageType
import com.example.demoproject.platform.data.model.VipSendFailureNotice

@Entity(
    tableName = "messages",
    primaryKeys = ["ownerUserId", "id"],
    indices = [
        Index(value = ["ownerUserId", "conversationId", "createdAt"], name = "idx_messages_owner_conversation_created_at"),
    ],
)
data class MessageEntity(
    val ownerUserId: String,
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val type: String,
    val status: String,
    val createdAt: Long,
    val isGapMarker: Boolean = false,
    val translatedText: String? = null,
)

fun MessageEntity.toDomain(): Message {
    val parsedType = runCatching { MessageType.valueOf(type) }.getOrDefault(MessageType.Text)
    val normalizedType = when {
        ChatSendLimitNotice.shouldShow(content) -> MessageType.System
        VipSendFailureNotice.isPayload(content) -> MessageType.System
        else -> parsedType
    }
    val message = Message(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        content = content,
        type = normalizedType,
        status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.Sent),
        createdAt = createdAt,
        translatedText = translatedText,
    )
    if (normalizedType != MessageType.Gift) return message
    val returnNoticeText = GiftReturnNotice.resolveDisplayText(content) ?: return message
    return message.copy(
        content = returnNoticeText,
        type = MessageType.System,
    )
}

fun Message.toEntity(ownerUserId: String): MessageEntity = MessageEntity(
    ownerUserId = ownerUserId,
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    content = content,
    type = type.name,
    status = status.name,
    createdAt = createdAt,
    translatedText = translatedText,
)
