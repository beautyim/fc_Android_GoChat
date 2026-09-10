package com.example.demoproject.platform.rtc.api

sealed interface RtcEvent {
    data class ConnectionStateChanged(
        val state: RtcConnectionState,
        val reason: Int? = null,
    ) : RtcEvent

    data class JoinedChannel(
        val channelId: String,
        val localUid: Int,
    ) : RtcEvent

    data class RemoteUserJoined(val uid: Int) : RtcEvent

    data class RemoteUserLeft(
        val uid: Int,
        val reason: Int? = null,
    ) : RtcEvent

    /** Remote user stopped sending video or disabled camera (merged from mute / enable callbacks). */
    data class RemoteVideoMuted(
        val uid: Int,
        val muted: Boolean,
    ) : RtcEvent

    data class Error(
        val code: Int,
        val message: String,
    ) : RtcEvent

    data object TokenWillExpire : RtcEvent
}

enum class RtcConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
}

