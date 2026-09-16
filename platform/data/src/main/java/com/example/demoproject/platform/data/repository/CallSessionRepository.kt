package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.CallRecordPage
import com.example.demoproject.platform.data.model.CallRoom
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.flow.Flow

interface CallSessionRepository {
    suspend fun createCall(
        targetUid: Long,
        callType: Int,
        fromType: Int = CallRoom.FROM_HOME,
    ): CallCreateResult

    /**
     * Confirms the call is connected.
     * Callee: await before join (may refresh Agora fields in [CallRoom]).
     * Caller: once after remote Agora uid appears.
     */
    suspend fun reportCallSuccess(
        roomId: String,
        fencingToken: String? = null,
    ): AppResult<CallRoom?>

    /** Callee fire-and-forget; failures are logged by the caller and do not block join. */
    suspend fun acceptCall(roomId: String): AppResult<Unit>

    /** Outgoing ringing poll — `waiting` / `answered` / `ended`. */
    suspend fun fetchAnswerStatus(roomId: String): AppResult<CallAnswerStatus>

    /** @return `false` when heartbeat indicates the room is no longer active (`ok = 0`). */
    suspend fun heartbeat(
        roomId: String,
        fencingToken: String? = null,
    ): AppResult<Boolean>

    suspend fun renewRtcToken(
        roomId: String,
        fencingToken: String? = null,
    ): CallRenewTokenResult

    suspend fun endCall(roomId: String, durationSeconds: Int): AppResult<Unit>

    /** Local camera blur flag for the peer (`0` off / blur, `1` camera on). */
    suspend fun updateMaskStatus(roomId: String, cameraOn: Boolean): AppResult<Unit>

    suspend fun sendCallMessage(
        roomId: String,
        toUid: Long,
        content: String,
        msgType: Int = com.example.demoproject.platform.data.network.dto.CallMessageRequestDto.MSG_TYPE_TEXT,
    ): AppResult<Unit>

    fun observeCallRoomMessages(roomId: Long): Flow<List<Message>>

    suspend fun upsertCallRoomMessage(message: Message): AppResult<Unit>

    companion object {
        const val CREATE_BUSY: Int = 2
        const val CREATE_NOT_FRIEND: Int = 3
        /** Peer is busy / not available for a call (`ok = 4`). */
        const val CREATE_PEER_UNAVAILABLE: Int = 4
    }
}

data class CallAnswerStatus(
    val status: String,
    val answerTimeoutSec: Int = 0,
) {
    val isWaiting: Boolean get() = status.equals("waiting", ignoreCase = true)
    val isAnswered: Boolean get() = status.equals("answered", ignoreCase = true)
    val isEnded: Boolean get() = status.equals("ended", ignoreCase = true)
}

sealed interface CallRenewTokenResult {
    data class Renewed(
        val token: String,
        val expireAt: Long = 0L,
        val renewAheadSec: Int = 0,
        val serverNow: Long = 0L,
    ) : CallRenewTokenResult

    /** Backend `ok=2` — stop scheduling renewals; do not hang up. */
    data object StopRenewal : CallRenewTokenResult

    data class Retryable(val message: String = "") : CallRenewTokenResult
}
