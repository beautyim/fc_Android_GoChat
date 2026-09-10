package com.example.demoproject.platform.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for `POST /msg/notice`. */
@Serializable
data class MsgNoticeListRequestDto(
    val mtime: Long = 0,
    @SerialName("last_mtime") val lastMtime: Long = 0,
)

@Serializable
data class MsgNoticeDto(
    @SerialName("chat_type") val chatType: Int = 0,
    @SerialName("msg_type") val msgType: Int = 0,
    val mtime: Long = 0,
    @SerialName("send_uid") val sendUid: Long = 0,
    @SerialName("msg_content") val msgContent: String = "",
    @SerialName("msg_status") val msgStatus: Int = 0,
)

@Serializable
data class MsgNoticeListResponseDto(
    val list: List<MsgNoticeDto> = emptyList(),
    @SerialName("has_more") val hasMoreRaw: Int = 0,
    @SerialName("last_mtime") val lastMtime: Long = 0,
) {
    val hasMore: Boolean get() = hasMoreRaw == 1
}
