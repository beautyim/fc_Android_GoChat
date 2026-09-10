package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.data.network.serializer.JsonAnyAsStringSerializer
import com.example.demoproject.platform.data.network.serializer.UserInfosMapSerializer
import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
data class ConversationListResponseDto(
    val list: List<ConversationDto> = emptyList(),
    @SerialName("service_list") val serviceList: List<ConversationDto> = emptyList(),
    @SerialName("system_list") val systemList: List<ConversationDto> = emptyList(),
    @SerialName("user_infos")
    @Serializable(with = UserInfosMapSerializer::class)
    val userInfos: Map<String, UserDto> = emptyMap(),
    @SerialName("has_more") val hasMoreRaw: Int = 0,
    @SerialName("last_sync_mtime") val lastSyncMtime: Long = 0,
    val unread: Int = 0,
    @SerialName("follow_unread") val followUnread: Int = 0,
) {
    val hasMore: Boolean get() = hasMoreRaw == 1
}

@Serializable
data class ConversationDto(
    @SerialName("chat_id") val chatId: Long = 0,
    @SerialName("target_uid") val targetUid: Long = 0,
    @SerialName("chat_type") val chatType: Int = CHAT_TYPE_PRIVATE,
    /** Per-conversation unread. Some packages historically used [badge] instead. */
    @Serializable(with = LenientIntSerializer::class)
    val unread: Int = 0,
    /** Legacy alias for [unread] used by older session-list payloads. */
    @Serializable(with = LenientIntSerializer::class)
    val badge: Int = 0,
    val mtime: Long = 0,
    @SerialName("last_msg") val lastMessage: MessageDto? = null,
    @SerialName("msg_type") val msgType: Int = MsgSendRequestDto.MSG_TYPE_TEXT,
    /** HTTP may return a string or a nested object for media rows. */
    @Serializable(with = JsonAnyAsStringSerializer::class)
    @SerialName("msg_content") val msgContent: String = "",
    @SerialName("send_uid") val sendUid: Long = 0,
    @SerialName("has_more") val rowHasMoreRaw: Int = 0,
    val list: List<MessageDto> = emptyList(),
    @SerialName("user_infos")
    @Serializable(with = UserInfosMapSerializer::class)
    val userInfos: Map<String, UserDto> = emptyMap(),
) {
    val rowHasMore: Boolean get() = rowHasMoreRaw == 1

    /** Resolved unread count across `unread` / `badge` aliases. */
    val resolvedUnread: Int get() = max(unread, badge).coerceAtLeast(0)

    companion object {
        const val CHAT_TYPE_PRIVATE: Int = 1
    }
}

@Serializable
data class MessageListResponseDto(
    val list: List<MessageDto> = emptyList(),
    @SerialName("has_more") val hasMoreRaw: Int = 0,
    @SerialName("last_mtime") val lastMtime: Long = 0,
    @SerialName("user_infos")
    @Serializable(with = UserInfosMapSerializer::class)
    val userInfos: Map<String, UserDto> = emptyMap(),
) {
    val hasMore: Boolean get() = hasMoreRaw == 1
}

@Serializable
data class MessageDto(
    @SerialName("send_uid") val sendUid: Long = 0,
    @SerialName("target_uid") val targetUid: Long = 0,
    @SerialName("chat_id") val chatId: Long = 0,
    @SerialName("chat_type") val chatType: Int = ConversationDto.CHAT_TYPE_PRIVATE,
    @SerialName("msg_type") val msgType: Int = MsgSendRequestDto.MSG_TYPE_TEXT,
    @Serializable(with = JsonAnyAsStringSerializer::class)
    val body: String = "",
    /** Accepts stringified JSON or nested objects (`image_url` / `url` media payloads). */
    @Serializable(with = JsonAnyAsStringSerializer::class)
    @SerialName("msg_content") val msgContent: String = "",
    val mtime: Long = 0,
    @SerialName("msg_status") val msgStatus: Int = 0,
    @SerialName("is_read") val isReadRaw: Int = 0,
    @SerialName("is_del") val isDeletedRaw: Int = 0,
    @SerialName("room_id") val roomId: Long? = null,
) {
    val isRead: Boolean get() = isReadRaw == 1 || msgStatus == 1
    val isDeleted: Boolean get() = isDeletedRaw == 1
    val wireBody: String get() = msgContent.ifBlank { body }
}

@Serializable
data class MessageListRequestDto(
    @SerialName("last_sync_mtime") val lastSyncMtime: Long = 0,
    @SerialName("top_mtime") val topMtime: Long = 0,
    @SerialName("chat_type") val chatType: Int = 0,
    val type: Int = 0,
)

@Serializable
data class MessageSyncRequestDto(
    @SerialName("last_sync_mtime") val lastSyncMtime: Long = 0,
    @SerialName("top_mtime") val topMtime: Long = 0,
    @SerialName("chat_type") val chatType: Int = 0,
    @SerialName("is_flash_chat") val isFlashChat: Int = 0,
)

@Serializable
data class MsgSendRequestDto(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("chat_type") val chatType: Int = ConversationDto.CHAT_TYPE_PRIVATE,
    val data: MsgSendDataDto,
) {
    companion object {
        const val MSG_TYPE_TEXT: Int = 1
        const val MSG_TYPE_VOICE: Int = 2
        const val MSG_TYPE_IMAGE: Int = 3
        const val MSG_TYPE_GIFT: Int = 5
        const val MSG_TYPE_ASK_GIFT: Int = 6
        const val MSG_TYPE_LIVE_GIFT: Int = 7
        const val MSG_TYPE_VIDEO: Int = 18
        const val MSG_TYPE_EMOJI: Int = 23
        const val MSG_TYPE_PRIVATE_PHOTO: Int = 25
        const val MSG_TYPE_PRIVATE_VIDEO: Int = 26
    }
}

@Serializable
data class MsgSendDataDto(
    @SerialName("msg_type") val msgType: Int,
    @SerialName("msg_content") val msgContent: MsgContentDto,
)

@Serializable
data class MsgContentDto(
    /** Text (`msg_type = 1`) / emoji body. */
    val content: String? = null,
    /** Video binary path (`msg_type = 18`). Also accepted as a legacy image alias. */
    val url: String? = null,
    val duration: Int? = null,
    /** Image original path (`msg_type = 3`). Required for peer clients to resolve the bubble. */
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("small_url") val smallUrl: String? = null,
    @SerialName("image_width") val imageWidth: Int? = null,
    @SerialName("image_height") val imageHeight: Int? = null,
    /** Video cover path (`msg_type = 18`). */
    val cover: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("upload_id") val uploadId: Int? = null,
)

@Serializable
data class MsgSendResponseDto(
    val mtime: Long = 0,
    // Nullable so a response that omits this field doesn't get coerced to 0 and wipe out the
    // global coin balance store; only apply it when the backend actually sent a value.
    val balance: Int? = null,
)

@Serializable
data class MessageDeleteRequestDto(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("chat_type") val chatType: Int = ConversationDto.CHAT_TYPE_PRIVATE,
    val mtime: Long = 0,
)

@Serializable
data class MessageSyncAckRequestDto(
    @SerialName("last_sync_mtime") val lastSyncMtime: Long = 0,
)

@Serializable
data class ConversationReadRequestDto(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("chat_type") val chatType: Int = ConversationDto.CHAT_TYPE_PRIVATE,
)

@Serializable
data class MsgUnreadRequestDto(
    @SerialName("chat_type") val chatType: Int = ConversationDto.CHAT_TYPE_PRIVATE,
    @SerialName("chat_id") val chatId: Long,
)

@Serializable
data class MsgUnreadResponseDto(
    @Serializable(with = LenientIntSerializer::class)
    val count: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val total: Int = 0,
    /** Legacy / package-variant alias for [count]. */
    @Serializable(with = LenientIntSerializer::class)
    val unread: Int = 0,
) {
    val resolvedCount: Int get() = max(count, unread).coerceAtLeast(0)
}

@Serializable
data class GiftConfigResponseDto(
    val version: Int = 0,
    val list: List<GiftDto> = emptyList(),
)

@Serializable
data class GiftDto(
    @SerialName("gift_id") val giftId: Long = 0,
    val icon: String = "",
    val title: String = "",
    val price: Int = 0,
    @SerialName("svga_name") val svgaName: String = "",
    @SerialName("svga_url") val svgaUrl: String = "",
    @SerialName("unlit_icon") val unlitIcon: String = "",
    val hidden: Int = 0,
    @SerialName("is_new") val isNew: Int = 0,
    val number: Int = 0,
    @SerialName("gift_source") val giftSource: Int = 0,
    @SerialName("from_type") val fromType: Int = 0,
    @SerialName("call_type") val callType: Int = 0,
)

@Serializable
data class GiftSendRequestDto(
    @SerialName("gift_id") val giftId: Long,
    @SerialName("to_uid") val toUid: Long,
    @SerialName("from_type") val fromType: Int = FROM_TYPE_CHAT,
    val number: Int = 1,
    /**
     * Call-scene room key (`from_type=2`). Same wire shape as `/call/message`:
     * channel-style string (e.g. `sc…`) or numeric id as decimal string.
     * Do not send `room_session_id` alone when the active call is keyed by a channel.
     */
    @SerialName("room_id") val roomId: String? = null,
    val action: Int = 1,
) {
    companion object {
        const val FROM_TYPE_CHAT: Int = 1
        const val FROM_TYPE_CALL: Int = 2
        const val FROM_TYPE_INTIMACY: Int = 10
    }
}

@Serializable
data class GiftSendResponseDto(
    val balance: Int = 0,
    val account: GiftAccountDto? = null,
    val mtime: Long = 0,
    @SerialName("gift_info") val giftInfo: GiftDto? = null,
)

@Serializable
data class GiftAccountDto(
    val money: Int = 0,
    val recharge: Int = 0,
)
