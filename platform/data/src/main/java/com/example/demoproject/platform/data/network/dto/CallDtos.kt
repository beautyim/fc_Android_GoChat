package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import com.example.demoproject.platform.data.network.serializer.UserInfosMapSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Requests ─────────────────────────────────────────────────────────────────

@Serializable
data class CallRecordsRequestDto(
    val page: Int = 1,
    /** 0 = all, 1 = incoming, 2 = outgoing, 3 = missed, 4 = cancelled, 5 = match. */
    val type: Int = TYPE_ALL,
    @SerialName("is_missed") val isMissed: Int = 0,
    @SerialName("is_active") val isActive: Int = 0,
    @SerialName("is_match") val isMatch: Int = 0,
) {
    companion object {
        const val TYPE_ALL: Int = 0
        const val TYPE_INCOMING: Int = 1
        const val TYPE_OUTGOING: Int = 2
        const val TYPE_MISSED: Int = 3
        const val TYPE_CANCELLED: Int = 4
        const val TYPE_MATCH: Int = 5
        const val DEFAULT_HOME_PRICE_PAGE: Int = 1
        const val MAX_PAGE: Int = 5
        const val PAGE_SIZE: Int = 20
    }
}

@Serializable
data class CallCreateRequestDto(
    @SerialName("target_uid") val targetUid: Long,
    /** 1 = video, 2 = voice */
    @SerialName("call_type") val callType: Int = 1,
    @SerialName("from_type") val fromType: Int = 0,
    /** Present only for calls launched from match success so backend can reuse match context. */
    @SerialName("is_match") val isMatch: Int? = null,
    @SerialName("is_match_call") val isMatchCall: Int? = null,
    @SerialName("match_session_id") val matchSessionId: Long? = null,
    @SerialName("match_id") val matchId: Long? = null,
)

@Serializable
data class CallRoomIdRequestDto(
    @SerialName("room_id") val roomId: String,
    /** Present only when `/call/create` returned a non-blank `fencing_token`. */
    @SerialName("fencing_token") val fencingToken: String? = null,
)

@Serializable
data class CallEndRequestDto(
    @SerialName("room_id") val roomId: String,
    val duration: Int = 0,
    @SerialName("end_type") val endType: Int = END_TYPE_USER_HANGUP,
) {
    companion object {
        const val END_TYPE_USER_HANGUP: Int = 3
        /** Server-initiated hangup when coin balance is insufficient. */
        const val END_TYPE_BALANCE: Int = 5
    }
}

@Serializable
data class CallBlurRequestDto(
    /** HTTP room key — channel-style string or numeric id as decimal string. */
    @SerialName("room_id") val roomId: String,
    /** 0 = camera off (blur), 1 = camera on */
    val status: Int,
)

@Serializable
data class CallMessageRequestDto(
    @SerialName("room_id") val roomId: String,
    @SerialName("to_uid") val toUid: Long,
    val content: String,
    @SerialName("msg_type") val msgType: Int = MSG_TYPE_TEXT,
) {
    companion object {
        const val MSG_TYPE_TEXT: Int = 1
        const val MSG_TYPE_EMOJI: Int = 23
    }
}

@Serializable
data class CallScreenshotRequestDto(
    @SerialName("target_uid") val targetUid: Long,
    /** HTTP room key — channel-style string or numeric id as decimal string. */
    @SerialName("room_id") val roomId: String,
    /** Seconds from call start when screenshot is taken. */
    val offset: Int,
    @SerialName("detect_info") val detectInfo: String = "",
    val images: List<CallScreenshotImageDto>,
)

@Serializable
data class CallScreenshotImageDto(
    val url: String,
    val height: Int,
    val width: Int,
)

// ── Responses ────────────────────────────────────────────────────────────────

@Serializable
data class CallRecordsResponseDto(
    val list: List<CallRecordDto> = emptyList(),
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("has_more") val hasMore: Int = 0,
    @Serializable(with = UserInfosMapSerializer::class)
    @SerialName("user_infos") val userInfos: Map<String, UserDto> = emptyMap(),
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("error_code") val errorCode: Int = 0,
)

@Serializable
data class CallRecordDto(
    @Serializable(with = LenientLongSerializer::class)
    val id: Long = 0,
    @Serializable(with = LenientLongSerializer::class)
    val uid: Long = 0,
    @Serializable(with = LenientLongSerializer::class)
    val caller: Long = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_match") val isMatch: Int = 0,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("room_id") val roomId: Long = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("call_type") val callType: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("call_price") val callPrice: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val status: Int = 0,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("add_time") val addTime: Long = 0,
    @SerialName("total_time") val totalTime: String = "",
    @SerialName("call_desc") val callDesc: String = "",
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_match_call") val isMatchCall: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("call_status") val callStatus: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("call_direction") val callDirection: Int = 0,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("ai_uid") val aiUid: Long = 0,
)

/**
 * Returned by `/call/create` (and optionally by `/call/success` for the callee's Agora info).
 *
 * Field presence is inferred from typical Agora + BerryCam backend patterns.
 * All fields default to safe empty values so partial responses don't crash.
 */
@Serializable
data class CallRoomDto(
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("room_id") val roomId: Long = 0,

    /** Agora channel name. Falls back to "room_{roomId}" if absent. */
    val channel: String = "",
    @SerialName("channel_name") val channelName: String = "",

    /** Agora App ID; use this when backend issues per-room app credentials. */
    @SerialName("app_id") val appId: String = "",

    /** Agora RTC token. Empty string when App Certificate is disabled. */
    val token: String = "",

    /** Agora UID for the local user in this room. */
    @Serializable(with = LenientIntSerializer::class)
    val uid: Int = 0,

    /**
     * Caller's user ID returned by `/call/create`. The server generates the Agora token for
     * this UID, so the caller must join the channel with it when [uid] is absent/zero.
     */
    @Serializable(with = LenientIntSerializer::class)
    val caller: Int = 0,

    /** 1 = video, 2 = audio */
    @Serializable(with = LenientIntSerializer::class)
    val type: Int = 1,

    @SerialName("user_info") val userInfo: UserDto? = null,

    @Serializable(with = LenientLongSerializer::class)
    @SerialName("m_time") val serverTimeMs: Long = 0,

    /**
     * Raw `/call/create` `room_id` for HTTP APIs when it is a non-numeric channel string.
     * Blank means fall back to [roomId] as decimal string in domain mapping.
     * Not a wire field — populated by [CallCreateDataDto.toCallRoomDto].
     */
    @kotlinx.serialization.Transient
    val httpRoomId: String = "",

    @SerialName("fencing_token") val fencingToken: String? = null,
) {
    private fun String?.isUsableChannel(): Boolean {
        val value = this?.trim().orEmpty()
        if (value.isEmpty()) return false
        // Backend occasionally returns placeholder channels like room_0.
        if (value == "room_0") return false
        return true
    }

    /**
     * Returns the effective Agora channel name: if the server returned a non-blank
     * [channel], use it; otherwise prefer the HTTP room key, then [roomId].
     */
    val effectiveChannel: String
        get() = channel
            .takeIf { it.isUsableChannel() }
            ?: channelName.takeIf { it.isUsableChannel() }
            ?: httpRoomId.trim().takeIf { it.isUsableChannel() }
            ?: roomId.toString()

    /**
     * The Agora UID the local user should join with.
     * Server generates the token bound to [caller] when [uid] is absent.
     */
    val effectiveUid: Int
        get() = if (uid != 0) uid else caller
}

