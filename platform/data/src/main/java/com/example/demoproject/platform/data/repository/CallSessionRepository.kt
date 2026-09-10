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

    suspend fun reportCallSuccess(
        roomId: String,
        fencingToken: String? = null,
    ): AppResult<Unit>

    /** @return `false` when heartbeat indicates the room is no longer active (`ok = 0`). */
    suspend fun heartbeat(
        roomId: String,
        fencingToken: String? = null,
    ): AppResult<Boolean>

    suspend fun endCall(roomId: String, durationSeconds: Int): AppResult<Unit>

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
