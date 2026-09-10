package com.example.demoproject.platform.callkit.signaling

import com.example.demoproject.platform.callkit.CallMediaType

sealed interface SignalingEvent {
    data class IncomingInvite(
        val inviteId: String,
        val callerUserId: String,
        val callerName: String = "",
        val callerAvatar: String = "",
        val callType: Int = CallMediaType.Video,
        val channelId: String,
        val rtcToken: String,
        val rtcUid: Int,
        val rtcAppId: String = "",
        /** Numeric `room_session_id` when [inviteId] is a channel-style HTTP room key. */
        val roomSessionId: Long = 0L,
        /**
         * `user_info.call_free_min` from the invite push — `> 0` means this incoming call is
         * free, per product rule. Fixed at invite time, never re-derived later.
         */
        val callFreeMin: Int = 0,
    ) : SignalingEvent

    data class InviteAccepted(val inviteId: String, val callId: String) : SignalingEvent

    data class InviteRejected(val inviteId: String, val reason: RejectReason) : SignalingEvent

    data class InviteCancelled(val inviteId: String) : SignalingEvent

    data class CallEnded(val callId: String, val reason: EndReason) : SignalingEvent

    data class Error(val message: String, val cause: Throwable? = null) : SignalingEvent
}

