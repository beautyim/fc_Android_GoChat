package com.example.demoproject.platform.mqtt

import com.example.demoproject.platform.network.serializer.JsonNumberOrStringAsStringSerializer
import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Parsed shape of the server-pushed MQTT payload when someone calls us.
 *
 * `room_id` may be a numeric id or a channel-style string (e.g. `sc…`).
 * [roomSessionId] supplies the numeric id when [roomId] is non-numeric.
 */
@Serializable
data class CallMqttNotificationDto(
    @Serializable(with = JsonNumberOrStringAsStringSerializer::class)
    @SerialName("room_id")
    val roomId: String = "",
    val channel: String = "",
    @SerialName("channel_name") val channelName: String = "",
    val token: String = "",
    @SerialName("app_id") val appId: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val uid: Int = 0,
    /** 1 = video, 2 = audio. */
    @Serializable(with = LenientIntSerializer::class)
    val type: Int = 1,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("caller_uid") val callerUid: Long = 0,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("caller") val callerUidCompat: Long = 0,
    @SerialName("caller_name") val callerName: String = "",
    @SerialName("caller_avatar") val callerAvatar: String = "",
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("room_session_id")
    val roomSessionId: Long = 0,
    /**
     * `user_info.call_free_min` from the incoming-invite push — remaining free-call minutes
     * for the callee *as of invite time*. Not a wire-level top-level field; populated by the
     * caller from the nested `user_info` object since this DTO is otherwise flat.
     * `> 0` means this incoming call is free, per product rule (fixed at invite time, never
     * re-derived from later MQTT balance-alert pushes).
     */
    val callFreeMin: Int = 0,
) {
    private fun String?.isUsableChannel(): Boolean {
        val value = this?.trim().orEmpty()
        if (value.isEmpty()) return false
        if (value == "room_0") return false
        return true
    }

    /** Numeric id for local bookkeeping; prefers numeric [roomId], else [roomSessionId]. */
    val numericRoomId: Long
        get() = roomId.trim().toLongOrNull()?.takeIf { it > 0L }
            ?: roomSessionId.takeIf { it > 0L }
            ?: 0L

    /** Room key for call HTTP APIs and signaling invite/call ids. */
    val effectiveHttpRoomId: String
        get() = roomId.trim().takeIf { it.isNotEmpty() && it != "0" }
            ?: numericRoomId.takeIf { it > 0L }?.toString().orEmpty()

    val effectiveChannel: String
        get() = channel
            .takeIf { it.isUsableChannel() }
            ?: channelName.takeIf { it.isUsableChannel() }
            ?: roomId.trim().takeIf { it.isUsableChannel() && it.toLongOrNull() == null }
            ?: numericRoomId.takeIf { it > 0L }?.toString().orEmpty()

    val effectiveCallerUid: Long
        get() = when {
            callerUid > 0L -> callerUid
            callerUidCompat > 0L -> callerUidCompat
            else -> 0L
        }

    val hasValidRoom: Boolean
        get() = effectiveHttpRoomId.isNotEmpty()

    companion object {
        const val MSG_TYPE_CALL_INVITE = 101
        const val MSG_TYPE_CALL_ENDED = 104
    }
}
