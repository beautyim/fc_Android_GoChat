package com.example.demoproject.platform.rtc.api

import android.view.View
import kotlinx.coroutines.flow.Flow

/**
 * Stable RTC API surface exposed to apps. Concrete SDK implementations (e.g. Agora)
 * live in separate modules and are injected via DI.
 */
interface RtcClient {
    val events: Flow<RtcEvent>

    fun initialize(config: RtcInitConfig)

    /**
     * Join a 1v1 call channel. Implementations may treat [token] as optional for
     * projects that enable App Certificate + dynamic tokens.
     */
    fun join(
        channelId: String,
        uid: Int = 0,
        token: String = "",
        enableVideo: Boolean = true,
        receiveOnly: Boolean = false,
    )

    fun leave()

    fun release()

    fun setMicMuted(muted: Boolean)

    fun setCameraMuted(muted: Boolean)

    /** Routes call audio output to the speakerphone when [enabled], otherwise the earpiece. */
    fun setSpeakerphoneEnabled(enabled: Boolean)

    fun switchCamera()

    fun renewToken(token: String)

    /** Typically a [android.view.TextureView] for Compose-friendly local preview. */
    fun createVideoView(): View

    /** Prefer [android.view.TextureView] for remote render (works well with [VideoCanvas]). */
    fun createRemoteVideoView(): View = createVideoView()

    fun setupLocalVideo(view: View?)

    fun setupRemoteVideo(uid: Int, view: View?)

    /**
     * After turning the local camera back on ([setCameraMuted](false)), call this if preview stays black.
     * Does not replace [setupLocalVideo]; keep your view attached.
     */
    fun refreshLocalPreview()
}

