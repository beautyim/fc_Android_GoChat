package com.example.demoproject.platform.data.model

data class CallRoom(
    val roomId: Long,
    val channel: String,
    val appId: String,
    val token: String,
    val uid: Int,
    val callType: Int,
    val peer: User? = null,
    val serverTimeMs: Long = 0L,
    /**
     * Room key for call HTTP APIs (`/call/success`, `/call/heart`, `/call/end`, …).
     * Match MQTT may use a channel-style string `room_id` that differs from [roomId].
     * Blank means fall back to [roomId] as decimal string.
     */
    val httpRoomId: String = "",
    /**
     * From `/call/create` when present; echoed on `/call/success` and `/call/heart`.
     */
    val fencingToken: String? = null,
    /**
     * `/call/create` `user_info.call_free_min` — remaining free-call minutes for the callee
     * *as of call creation*. This is the sole source of truth for whether the just-created
     * call is free or paid; it must not be re-derived later from MQTT balance-alert pushes.
     */
    val callFreeMin: Int = 0,
) {
    val isVideoCall: Boolean get() = callType != CALL_TYPE_VOICE

    /** `call_free_min > 0` at `/call/create` time means this call is free, per product rule. */
    val isFreeCall: Boolean get() = callFreeMin > 0

    /** Non-blank HTTP room key sent to the call backend. */
    val effectiveHttpRoomId: String
        get() = httpRoomId.trim().takeIf { it.isNotEmpty() } ?: roomId.toString()

    companion object {
        const val CALL_TYPE_VIDEO: Int = 1
        const val CALL_TYPE_VOICE: Int = 2

        const val FROM_HOME: Int = 0
        const val FROM_CHAT: Int = 1
        const val FROM_MATCH: Int = 2
    }
}
