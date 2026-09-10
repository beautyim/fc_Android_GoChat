package com.example.demoproject.platform.data.model

data class Conversation(
    val id: String,
    val peer: User,
    val lastMessage: Message?,
    val unreadCount: Int,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
)

/** `/msg/detail` payload used when entering a private chat. */
data class ConversationDetail(
    val conversation: Conversation,
    /** `chat_is_reply` from `/msg/detail`; `1` means the peer has replied in this chat. */
    val peerHasReplied: Boolean = false,
    /** `friend_status` from `/msg/detail`; `1` means already friends / mutual follow. */
    val friendStatus: Int = 0,
    /** `unlock_from_type` from `/msg/detail`; non-null means the chat should show a VIP unlock prompt. */
    val unlockFromType: Int? = null,
    /** Remaining free sends from `/msg/detail` `free_msg`. */
    val freeMessageCount: Int = 0,
)

data class ConversationListPage(
    val conversations: List<Conversation>,
    val hasMore: Boolean,
    val lastSyncMtime: Long,
)

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val type: MessageType,
    val status: MessageStatus,
    val createdAt: Long,
    /** Local cache of `/translation/submit` output; not part of server message JSON. */
    val translatedText: String? = null,
)

enum class MessageType { Text, Voice, Image, Video, Emoji, Gift, PrivatePhoto, PrivateVideo, System }

enum class MessageStatus { Sending, Sent, Delivered, Read, Failed }

/**
 * Different endpoints (and the locally-synthesized timestamp for outbound
 * Sending/Failed messages) have represented `createdAt` in seconds,
 * milliseconds, or microseconds. Normalizing here keeps every timestamp
 * comparable, whether it is used for chat timeline sorting
 * ([com.quantum.berrycam.ui.chat.sortedForChatDetail]) or for anchoring a new
 * pending message after the latest known message in [MessageRepositoryImpl].
 */
fun Long.toMessageTimelineMillis(): Long =
    when {
        this >= MICROSECOND_EPOCH_THRESHOLD -> this / MICROSECONDS_PER_MILLISECOND
        this in 1 until MILLISECOND_EPOCH_THRESHOLD -> this * MILLISECONDS_PER_SECOND
        else -> this
    }

/** Same normalization as [toMessageTimelineMillis], expressed in microseconds. */
fun Long.toMessageTimelineMicros(): Long =
    when {
        this >= MICROSECOND_EPOCH_THRESHOLD -> this
        this >= MILLISECOND_EPOCH_THRESHOLD -> this * MICROSECONDS_PER_MILLISECOND
        this > 0L -> this * MICROSECONDS_PER_MILLISECOND * MILLISECONDS_PER_SECOND
        else -> this
    }

fun Long.isMessageTimelineMicros(): Boolean =
    this >= MICROSECOND_EPOCH_THRESHOLD

private const val MILLISECONDS_PER_SECOND = 1_000L
internal const val MICROSECONDS_PER_MILLISECOND = 1_000L
private const val MILLISECOND_EPOCH_THRESHOLD = 1_000_000_000_000L
internal const val MICROSECOND_EPOCH_THRESHOLD = 1_000_000_000_000_000L
