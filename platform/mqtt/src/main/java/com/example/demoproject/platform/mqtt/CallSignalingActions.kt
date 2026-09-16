package com.example.demoproject.platform.mqtt

interface CallSignalingActions {
    /** Fire-and-forget `POST /call/accept`. */
    suspend fun acceptCall(roomId: String)

    /** `POST /call/success` — optionally echo [fencingToken]. */
    suspend fun markCallSucceeded(roomId: String, fencingToken: String? = null)

    suspend fun endCall(roomId: String, source: String)
}

interface CurrentUserIdProvider {
    val currentUserId: String?
}
