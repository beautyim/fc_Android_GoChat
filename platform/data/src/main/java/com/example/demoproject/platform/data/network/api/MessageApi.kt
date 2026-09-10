package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.ConversationListResponseDto
import com.example.demoproject.platform.data.network.dto.ConversationReadRequestDto
import com.example.demoproject.platform.data.network.dto.GiftConfigResponseDto
import com.example.demoproject.platform.data.network.dto.GiftSendRequestDto
import com.example.demoproject.platform.data.network.dto.GiftSendResponseDto
import com.example.demoproject.platform.data.network.dto.MessageDeleteRequestDto
import com.example.demoproject.platform.data.network.dto.MessageListResponseDto
import com.example.demoproject.platform.data.network.dto.MessageListRequestDto
import com.example.demoproject.platform.data.network.dto.MessageSyncAckRequestDto
import com.example.demoproject.platform.data.network.dto.MessageSyncRequestDto
import com.example.demoproject.platform.data.network.dto.MsgSendRequestDto
import com.example.demoproject.platform.data.network.dto.MsgSendResponseDto
import com.example.demoproject.platform.data.network.dto.MsgUnreadRequestDto
import com.example.demoproject.platform.data.network.dto.MsgUnreadResponseDto
import com.example.demoproject.platform.network.dto.ApiResponse
import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface MessageApi {
    @POST("msg/list")
    suspend fun getConversations(@Body body: MessageListRequestDto): ApiResponse<ConversationListResponseDto>

    @POST("msg/sync")
    suspend fun syncConversations(@Body body: MessageSyncRequestDto): ApiResponse<ConversationListResponseDto>

    @POST("msg/detail")
    suspend fun getConversationDetail(@Body body: MessageChatRequestDto): ApiResponse<ChatDetailResponseDto>

    @POST("msg/sync-detail")
    suspend fun syncMessages(@Body body: MessageSyncDetailRequestDto): ApiResponse<MessageListResponseDto>

    @POST("msg/send")
    suspend fun sendMessage(@Body body: MsgSendRequestDto): ApiResponse<MsgSendResponseDto?>

    @POST("msg/delete")
    suspend fun deleteMessage(@Body body: MessageDeleteRequestDto): ApiResponse<Unit?>

    @POST("msg/clear-unread")
    suspend fun clearUnread(): ApiResponse<Unit?>

    @POST("msg/sync-ack")
    suspend fun syncAck(@Body body: MessageSyncAckRequestDto): ApiResponse<Unit?>

    @POST("msg/read")
    suspend fun markRead(@Body body: ConversationReadRequestDto): ApiResponse<Unit?>

    @POST("msg/get-unread")
    suspend fun getUnread(@Body body: MsgUnreadRequestDto): ApiResponse<MsgUnreadResponseDto>

    @POST("gift/config")
    suspend fun getGiftConfig(@Body body: List<String> = emptyList()): ApiResponse<GiftConfigResponseDto>

    @POST("gift/send")
    suspend fun sendGift(@Body body: GiftSendRequestDto): ApiResponse<GiftSendResponseDto>
}

@Serializable
data class MessageChatRequestDto(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("chat_type") val chatType: Int = 1,
)

@Serializable
data class MessageSyncDetailRequestDto(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("last_mtime") val lastMtime: Long = 0,
    @SerialName("last_sync_mtime") val lastSyncMtime: Long = 0,
    @SerialName("chat_type") val chatType: Int = 1,
)

@Serializable
data class ChatDetailResponseDto(
    @SerialName("user_info") val userInfo: com.example.demoproject.platform.data.network.dto.UserDto? = null,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("friend_status") val friendStatus: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("chat_is_reply") val chatIsReply: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("chat_coach_reply") val chatCoachReply: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("unlock_from_type") val unlockFromType: Int = -1,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("free_msg") val freeMessageCount: Int = 0,
)
