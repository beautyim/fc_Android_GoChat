package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.network.api.CallApi
import com.example.demoproject.platform.data.network.dto.CallEndRequestDto
import com.example.demoproject.platform.data.network.dto.CallRoomIdRequestDto
import com.example.demoproject.platform.mqtt.CallSignalingActions
import com.example.demoproject.platform.network.safeApiCallUnit

class HttpCallSignalingActions(
    private val callApi: CallApi,
) : CallSignalingActions {

    override suspend fun markCallSucceeded(roomId: String) {
        safeApiCallUnit { callApi.success(CallRoomIdRequestDto(roomId = roomId)) }
    }

    override suspend fun endCall(roomId: String, source: String) {
        safeApiCallUnit {
            callApi.end(
                CallEndRequestDto(
                    roomId = roomId,
                    endType = CallEndRequestDto.END_TYPE_USER_HANGUP,
                ),
            )
        }
    }
}
