package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.data.network.serializer.JsonNumberOrStringAsStringSerializer
import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payload for `/call/create`. On success this carries Agora room fields; on balance
 * failures the same envelope may include a [callback] instead of a valid room id.
 *
 * `room_id` may be a numeric id or a channel-style string (e.g. `sc…`); HTTP call APIs
 * must echo the raw string. [roomSessionId] supplies the numeric id when [roomId] is
 * non-numeric.
 */
@Serializable
data class CallCreateDataDto(
    @Serializable(with = JsonNumberOrStringAsStringSerializer::class)
    @SerialName("room_id")
    val roomId: String = "",
    val channel: String = "",
    @SerialName("channel_name")
    val channelName: String = "",
    @SerialName("app_id")
    val appId: String = "",
    val token: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val uid: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val caller: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val type: Int = 1,
    @SerialName("user_info")
    val userInfo: UserDto? = null,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("m_time")
    val serverTimeMs: Long = 0,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("room_session_id")
    val roomSessionId: Long = 0,
    /**
     * Opaque session fence from create; echo on `/call/success` and `/call/heart` when present.
     */
    @SerialName("fencing_token")
    val fencingToken: String? = null,
    val callback: VipAlertCallbackDto? = null,
) {
    fun toCallRoomDto(): CallRoomDto {
        val rawRoomId = roomId.trim()
        val numericRoomId = rawRoomId.toLongOrNull()?.takeIf { it > 0L }
            ?: roomSessionId.takeIf { it > 0L }
            ?: 0L
        return CallRoomDto(
            roomId = numericRoomId,
            channel = channel,
            channelName = channelName,
            appId = appId,
            token = token,
            uid = uid,
            caller = caller,
            type = type,
            userInfo = userInfo,
            serverTimeMs = serverTimeMs,
            httpRoomId = rawRoomId,
            fencingToken = fencingToken?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    fun hasValidRoom(): Boolean {
        val raw = roomId.trim()
        return raw.isNotEmpty() && raw != "0"
    }
}
