package com.example.demoproject.platform.callkit.signaling

import kotlinx.coroutines.flow.Flow

/**
 * Transport-agnostic call signaling contract.
 *
 * Your app can implement this on top of MQTT (recommended for 1v1), WebSocket, etc.
 * CallKit only cares about events + commands, not how they are delivered.
 */
interface CallSignalingClient {
    val events: Flow<SignalingEvent>

    suspend fun sendInvite(request: OutgoingInviteRequest)

    suspend fun accept(inviteId: String)

    suspend fun reject(inviteId: String, reason: RejectReason)

    suspend fun cancel(inviteId: String)

    suspend fun end(callId: String, reason: EndReason)
}

data class OutgoingInviteRequest(
    val inviteId: String,
    val calleeUserId: String,
    val channelId: String,
    val rtcToken: String,
    val rtcUid: Int,
    val rtcAppId: String = "",
    val roomSessionId: Long = 0L,
    val fencingToken: String? = null,
    val peerNickname: String = "",
    val peerAvatarUrl: String = "",
    val peerAge: Int = 0,
    /** Absolute or relative video-show play URL for the dialing background. */
    val peerVideoUrl: String = "",
    val peerCoverUrl: String = "",
)

enum class RejectReason {
    Busy,
    Declined,
    Timeout,
    Unknown,
}

enum class EndReason {
    Hangup,
    RemoteHangup,
    InsufficientBalance,
    NetworkError,
    Timeout,
    Unknown,
}

