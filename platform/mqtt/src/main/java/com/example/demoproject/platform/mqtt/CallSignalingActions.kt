package com.example.demoproject.platform.mqtt

interface CallSignalingActions {
    suspend fun markCallSucceeded(roomId: String)
    suspend fun endCall(roomId: String, source: String)
}

interface CurrentUserIdProvider {
    val currentUserId: String?
}
