package com.example.demoproject.platform.rtc.agora

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.rtc.api.RtcClient
import com.example.demoproject.platform.rtc.api.RtcConnectionState
import com.example.demoproject.platform.rtc.api.RtcEvent
import com.example.demoproject.platform.rtc.api.RtcInitConfig
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AgoraRtcClient(
    private val context: Context,
) : RtcClient {

    private val appContext = context.applicationContext
    private val engineLock = Any()

    private var engine: RtcEngine? = null
    private var currentAppId: String = ""
    private var joinedChannelId: String? = null
    private var localCameraMuted: Boolean = false
    /** Shared local preview surface reused by [createVideoView] and pre-join capture. */
    private var localPreviewView: View? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var firstFrameTimeoutRunnable: Runnable? = null
    @Volatile
    private var joinStartedAtMs: Long = 0L
    @Volatile
    private var firstLocalFrameAtMs: Long = 0L
    @Volatile
    private var firstRemoteFrameAtMs: Long = 0L
    @Volatile
    private var remoteDecodedCount: Int = 0
    private val remoteVideoMuteStates = mutableMapOf<Int, Boolean>()

    private val _events = MutableSharedFlow<RtcEvent>(
        replay = 1,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: Flow<RtcEvent> = _events.asSharedFlow()

    private val handler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            val ch = channel.orEmpty()
            AppLogger.d(TAG, "onJoinChannelSuccess channel=$ch uid=$uid elapsed=$elapsed")
            if (ch.isNotBlank()) {
                joinedChannelId = ch
                _events.tryEmit(RtcEvent.JoinedChannel(channelId = ch, localUid = uid))
            }
            _events.tryEmit(RtcEvent.ConnectionStateChanged(RtcConnectionState.Connected))
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            AppLogger.d(TAG, "onUserJoined uid=$uid elapsed=$elapsed")
            _events.tryEmit(RtcEvent.RemoteUserJoined(uid))
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            AppLogger.d(TAG, "onUserOffline uid=$uid reason=$reason")
            _events.tryEmit(RtcEvent.RemoteUserLeft(uid = uid, reason = reason))
        }

        override fun onUserMuteVideo(uid: Int, muted: Boolean) {
            emitRemoteVideoMuted(uid = uid, muted = muted, source = "onUserMuteVideo")
        }

        override fun onUserEnableVideo(uid: Int, enabled: Boolean) {
            emitRemoteVideoMuted(uid = uid, muted = !enabled, source = "onUserEnableVideo")
        }

        override fun onUserEnableLocalVideo(uid: Int, enabled: Boolean) {
            emitRemoteVideoMuted(uid = uid, muted = !enabled, source = "onUserEnableLocalVideo")
        }

        override fun onFirstLocalVideoFramePublished(
            source: Constants.VideoSourceType?,
            elapsed: Int,
        ) {
            if (firstLocalFrameAtMs == 0L) {
                firstLocalFrameAtMs = System.currentTimeMillis()
            }
            AppLogger.d(
                TAG,
                "firstLocalVideoFramePublished channel=${joinedChannelId.orEmpty()} source=$source elapsedMs=$elapsed wallTimeMs=${System.currentTimeMillis()}",
            )
        }

        override fun onFirstRemoteVideoDecoded(
            uid: Int,
            width: Int,
            height: Int,
            elapsed: Int,
        ) {
            if (firstRemoteFrameAtMs == 0L) {
                firstRemoteFrameAtMs = System.currentTimeMillis()
            }
            remoteDecodedCount += 1
            val sinceJoin = if (joinStartedAtMs > 0L) System.currentTimeMillis() - joinStartedAtMs else -1L
            AppLogger.d(
                TAG,
                "firstRemoteVideoDecoded count=$remoteDecodedCount channel=${joinedChannelId.orEmpty()} uid=$uid size=${width}x$height elapsedMs=$elapsed sinceJoinMs=$sinceJoin wallTimeMs=${System.currentTimeMillis()}",
            )
        }

        override fun onLocalVideoStateChanged(
            source: Constants.VideoSourceType?,
            state: Int,
            error: Int,
        ) {
            AppLogger.d(
                TAG,
                "localVideoStateChanged channel=${joinedChannelId.orEmpty()} source=$source state=$state(${describeLocalVideoState(state)}) error=$error(${describeLocalVideoError(error)}) wallTimeMs=${System.currentTimeMillis()}",
            )
        }

        override fun onLocalVideoStats(
            source: Constants.VideoSourceType?,
            stats: IRtcEngineEventHandler.LocalVideoStats?,
        ) {
            if (stats == null) return
            AppLogger.d(
                TAG,
                "localVideoStats channel=${joinedChannelId.orEmpty()} source=$source sentBitrateKbps=${stats.sentBitrate} sentFps=${stats.sentFrameRate} captureFps=${stats.captureFrameRate} targetBitrateKbps=${stats.targetBitrate} targetFps=${stats.targetFrameRate} txPacketLoss=${stats.txPacketLossRate} encoderOutputFps=${stats.encoderOutputFrameRate}",
            )
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            AppLogger.d(TAG, "onConnectionStateChanged state=$state reason=$reason")
            val mapped = when (state) {
                Constants.CONNECTION_STATE_CONNECTED -> RtcConnectionState.Connected
                Constants.CONNECTION_STATE_CONNECTING -> RtcConnectionState.Connecting
                Constants.CONNECTION_STATE_RECONNECTING -> RtcConnectionState.Reconnecting
                else -> RtcConnectionState.Disconnected
            }
            _events.tryEmit(RtcEvent.ConnectionStateChanged(state = mapped, reason = reason))
        }

        override fun onTokenPrivilegeWillExpire(token: String?) {
            AppLogger.w(TAG, "onTokenPrivilegeWillExpire tokenLen=${token?.length ?: 0}")
            _events.tryEmit(RtcEvent.TokenWillExpire)
        }

        override fun onError(err: Int) {
            AppLogger.w(TAG, "onError err=$err")
            _events.tryEmit(RtcEvent.Error(code = err, message = "agora onError($err)"))
        }
    }

    override fun initialize(config: RtcInitConfig) {
        synchronized(engineLock) {
            if (config.appId.isBlank()) {
                AppLogger.w(TAG, "initialize failed blank appId")
                _events.tryEmit(RtcEvent.Error(code = -1, message = "RtcInitConfig.appId is blank"))
                return
            }
            if (engine != null && currentAppId == config.appId) {
                AppLogger.d(TAG, "initialize skipped already initialized appIdLen=${config.appId.length}")
                return
            }
            AppLogger.d(TAG, "initialize recreate engine appIdLen=${config.appId.length}")
            destroyEngineLocked()
            currentAppId = config.appId
            val cfg = RtcEngineConfig().apply {
                mContext = appContext
                mAppId = config.appId
                mEventHandler = handler
            }
            engine = RtcEngine.create(cfg).also { rtc ->
                rtc.enableVideo()
                rtc.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                rtc.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            }
        }
    }

    override fun join(channelId: String, uid: Int, token: String, enableVideo: Boolean) {
        val rtc = synchronized(engineLock) { engine }
        if (rtc == null) {
            AppLogger.w(TAG, "join failed engine null")
            _events.tryEmit(RtcEvent.Error(code = -2, message = "RtcClient not initialized"))
            return
        }
        if (channelId.isBlank()) {
            AppLogger.w(TAG, "join failed channel blank")
            _events.tryEmit(RtcEvent.Error(code = -3, message = "channelId is blank"))
            return
        }
        AppLogger.d(
            TAG,
            "join start channel=$channelId uid=$uid tokenLen=${token.length} enableVideo=$enableVideo",
        )
        remoteVideoMuteStates.clear()
        startFirstFrameWatchdog(channelId = channelId, uid = uid)
        if (enableVideo) {
            localCameraMuted = false
            // Singleton engines reuse the last facing after switchCamera(); reset to front
            // before capture so every video call starts on the front camera.
            preferFrontCamera(rtc)
            startLocalCapture(rtc)
        } else {
            localCameraMuted = true
            rtc.muteLocalVideoStream(true)
            rtc.enableLocalVideo(false)
        }
        _events.tryEmit(RtcEvent.ConnectionStateChanged(RtcConnectionState.Connecting))
        val options = ChannelMediaOptions().apply {
            autoSubscribeAudio = true
            autoSubscribeVideo = enableVideo
            publishCameraTrack = enableVideo
            publishMicrophoneTrack = true
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        }
        joinedChannelId = channelId
        val rc = rtc.joinChannel(token, channelId, uid, options)
        AppLogger.d(TAG, "joinChannel returned rc=$rc channel=$channelId uid=$uid")
        if (enableVideo) {
            startLocalCapture(rtc)
        } else {
            rtc.muteLocalVideoStream(true)
            rtc.enableLocalVideo(false)
        }
    }

    override fun leave() {
        AppLogger.d(TAG, "leave joinedChannelId=$joinedChannelId")
        stopFirstFrameWatchdog()
        remoteVideoMuteStates.clear()
        synchronized(engineLock) {
            engine?.leaveChannel()
            joinedChannelId = null
            localCameraMuted = false
        }
        _events.tryEmit(RtcEvent.ConnectionStateChanged(RtcConnectionState.Disconnected))
    }

    override fun release() {
        AppLogger.d(TAG, "release")
        synchronized(engineLock) {
            destroyEngineLocked()
        }
    }

    override fun setMicMuted(muted: Boolean) {
        synchronized(engineLock) { engine }?.muteLocalAudioStream(muted)
    }

    /**
     * Matches Agora guidance: turning the camera "off" should stop capture and send a black frame,
     * not merely stop encoding (which can freeze the last frame for the remote).
     */
    override fun setCameraMuted(muted: Boolean) {
        val rtc = synchronized(engineLock) { engine } ?: return
        localCameraMuted = muted
        if (muted) {
            rtc.muteLocalVideoStream(true)
            rtc.enableLocalVideo(false)
            runCatching { rtc.stopPreview() }
                .onFailure { AppLogger.w(TAG, "stopPreview: ${it.message}", it) }
            rtc.setupLocalVideo(null)
        } else {
            rtc.enableLocalVideo(true)
            rtc.muteLocalVideoStream(false)
            refreshLocalPreview()
        }
    }

    override fun setSpeakerphoneEnabled(enabled: Boolean) {
        synchronized(engineLock) { engine }?.setEnableSpeakerphone(enabled)
    }

    override fun switchCamera() {
        synchronized(engineLock) { engine }?.switchCamera()
    }

    override fun renewToken(token: String) {
        synchronized(engineLock) { engine }?.renewToken(token)
    }

    override fun createVideoView(): View = synchronized(engineLock) {
        localPreviewView ?: createLocalRendererView().also { localPreviewView = it }
    }

    override fun createRemoteVideoView(): View =
        TextureView(appContext)

    override fun setupLocalVideo(view: View?) {
        val rtc = synchronized(engineLock) { engine } ?: return
        if (view == null) {
            if (!localCameraMuted && joinedChannelId != null) {
                bindLocalCaptureSurface(rtc)
            } else {
                rtc.setupLocalVideo(null)
            }
            return
        }
        // UI may still host the preview SurfaceView (collapsed) while the camera is off; do not
        // re-attach a canvas or the last local frame can reappear in the local preview window.
        if (localCameraMuted) return
        rtc.setupLocalVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        rtc.enableLocalVideo(true)
        rtc.muteLocalVideoStream(false)
        rtc.startPreview()
    }

    override fun setupRemoteVideo(uid: Int, view: View?) {
        val rtc = synchronized(engineLock) { engine } ?: return
        if (view == null || uid == 0) return
        rtc.setupRemoteVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    override fun refreshLocalPreview() {
        val rtc = synchronized(engineLock) { engine } ?: return
        if (localCameraMuted) return
        runCatching {
            localPreviewView?.let { view ->
                rtc.setupLocalVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            }
            rtc.enableLocalVideo(true)
            rtc.muteLocalVideoStream(false)
            rtc.startPreview()
        }.onFailure { AppLogger.w(TAG, "refreshLocalPreview failed: ${it.message}", it) }
    }

    private fun destroyEngineLocked() {
        stopFirstFrameWatchdog()
        remoteVideoMuteStates.clear()
        val rtc = engine ?: run {
            currentAppId = ""
            joinedChannelId = null
            return
        }
        runCatching { rtc.stopPreview() }
            .onFailure { AppLogger.w(TAG, "stopPreview: ${it.message}", it) }
        runCatching { rtc.leaveChannel() }
            .onFailure { AppLogger.w(TAG, "leaveChannel: ${it.message}", it) }
        engine = null
        joinedChannelId = null
        localPreviewView = null
        currentAppId = ""
        _events.tryEmit(RtcEvent.ConnectionStateChanged(RtcConnectionState.Disconnected))
        runCatching { RtcEngine.destroy() }
            .onFailure { AppLogger.w(TAG, "RtcEngine.destroy failed: ${it.message}", it) }
    }

    @Deprecated("Use release()", ReplaceWith("release()"))
    fun destroyEngine() = release()

    private companion object {
        const val TAG = "AgoraRtcClient"
        const val FIRST_FRAME_TIMEOUT_MS = 7_000L
    }

    private fun startFirstFrameWatchdog(channelId: String, uid: Int) {
        stopFirstFrameWatchdog()
        val startAt = System.currentTimeMillis()
        joinStartedAtMs = startAt
        firstLocalFrameAtMs = 0L
        firstRemoteFrameAtMs = 0L
        remoteDecodedCount = 0
        firstFrameTimeoutRunnable = Runnable {
            val now = System.currentTimeMillis()
            val elapsed = now - startAt
            if (joinedChannelId != channelId) return@Runnable
            if (firstLocalFrameAtMs == 0L) {
                AppLogger.w(
                    TAG,
                    "first-frame-timeout local-publish-missing channel=$channelId uid=$uid elapsedMs=$elapsed joinAtMs=$startAt nowMs=$now",
                )
                recoverStalledLocalCapture()
            }
            if (firstRemoteFrameAtMs == 0L) {
                AppLogger.w(
                    TAG,
                    "first-frame-timeout remote-decode-missing channel=$channelId uid=$uid elapsedMs=$elapsed joinAtMs=$startAt nowMs=$now",
                )
            }
        }
        mainHandler.postDelayed(firstFrameTimeoutRunnable!!, FIRST_FRAME_TIMEOUT_MS)
    }

    /**
     * Best-effort recovery when [FIRST_FRAME_TIMEOUT_MS] elapses without a published local frame.
     * Forces a full stop/restart of the capture pipeline (and rebinds the preview canvas if it
     * already has a valid `Surface`) in case the engine's camera capturer got wedged. Runs once
     * per [startFirstFrameWatchdog] cycle; does not retry further after this.
     */
    private fun recoverStalledLocalCapture() {
        if (localCameraMuted || joinedChannelId == null) return
        val rtc = synchronized(engineLock) { engine } ?: return
        AppLogger.w(TAG, "recoverStalledLocalCapture channel=$joinedChannelId")
        runCatching {
            rtc.stopPreview()
            rtc.enableLocalVideo(false)
            rtc.enableLocalVideo(true)
            rtc.muteLocalVideoStream(false)
            val view = localPreviewView
            if (view is SurfaceView && view.holder.surface.isValid) {
                rtc.setupLocalVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            }
            rtc.startPreview()
        }.onFailure { AppLogger.w(TAG, "recoverStalledLocalCapture failed: ${it.message}", it) }
    }

    /**
     * Forces the front camera before local capture starts.
     *
     * Must run before [RtcEngine.startPreview] / [RtcEngine.enableLocalVideo]. When the engine is
     * reused across calls, a prior [RtcEngine.switchCamera] can leave the rear camera selected.
     */
    private fun preferFrontCamera(rtc: RtcEngine) {
        runCatching {
            runCatching { rtc.stopPreview() }
                .onFailure { AppLogger.w(TAG, "stopPreview before front camera: ${it.message}", it) }
            rtc.enableLocalVideo(false)
            val rc = rtc.setCameraCapturerConfiguration(
                CameraCapturerConfiguration(
                    CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT,
                ),
            )
            if (rc != 0) {
                AppLogger.w(TAG, "setCameraCapturerConfiguration front rc=$rc")
            }
        }.onFailure { AppLogger.w(TAG, "preferFrontCamera failed: ${it.message}", it) }
    }

    /**
     * Starts local camera capture/publish for [join] without binding a preview canvas.
     *
     * The shared preview [View] returned by [createVideoView] has not been attached to any
     * window yet at join time, so its `Surface` is invalid. Calling [RtcEngine.setupLocalVideo]
     * with that invalid canvas before [RtcEngine.startPreview] has been observed to wedge camera
     * capture on some OEM devices (e.g. OnePlus/Oplus): once the UI later attaches the real view
     * and calls `setupLocalVideo()` again with a genuinely valid `Surface` (see
     * `bindLocalVideoWhenSurfaceReady` in core/rtc-compose-ui), the engine no longer restarts the
     * capturer, so local video is never published (see `first-frame-timeout
     * local-publish-missing`). Binding the real canvas is deferred entirely to [setupLocalVideo]
     * / [refreshLocalPreview], which only run once the UI's `Surface` is actually valid.
     */
    private fun startLocalCapture(rtc: RtcEngine) {
        runCatching {
            rtc.enableLocalVideo(true)
            rtc.muteLocalVideoStream(false)
            rtc.startPreview()
        }.onFailure { AppLogger.w(TAG, "startLocalCapture failed: ${it.message}", it) }
    }

    /**
     * Rebinds the shared preview canvas on an already-active call, e.g. when a caller explicitly
     * clears the local canvas via `setupLocalVideo(null)` while unmuted. Unlike [startLocalCapture]
     * this is not used at join time.
     */
    private fun bindLocalCaptureSurface(rtc: RtcEngine) {
        runCatching {
            val view = createVideoView()
            rtc.setupLocalVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            rtc.enableLocalVideo(true)
            rtc.muteLocalVideoStream(false)
            rtc.startPreview()
        }.onFailure { AppLogger.w(TAG, "bindLocalCaptureSurface failed: ${it.message}", it) }
    }

    private fun createLocalRendererView(): SurfaceView =
        SurfaceView(appContext).apply {
            setZOrderMediaOverlay(true)
        }

    private fun stopFirstFrameWatchdog() {
        firstFrameTimeoutRunnable?.let(mainHandler::removeCallbacks)
        firstFrameTimeoutRunnable = null
        joinStartedAtMs = 0L
        firstLocalFrameAtMs = 0L
        firstRemoteFrameAtMs = 0L
        remoteDecodedCount = 0
    }

    private fun emitRemoteVideoMuted(uid: Int, muted: Boolean, source: String) {
        val previous = remoteVideoMuteStates[uid]
        if (previous == muted) {
            AppLogger.d(
                TAG,
                "remoteVideoMuted deduped uid=$uid muted=$muted source=$source channel=${joinedChannelId.orEmpty()}",
            )
            return
        }
        remoteVideoMuteStates[uid] = muted
        AppLogger.d(
            TAG,
            "remoteVideoMuted emit uid=$uid muted=$muted source=$source prev=$previous channel=${joinedChannelId.orEmpty()}",
        )
        _events.tryEmit(RtcEvent.RemoteVideoMuted(uid = uid, muted = muted))
    }

    private fun describeLocalVideoState(state: Int): String =
        when (state) {
            Constants.LOCAL_VIDEO_STREAM_STATE_STOPPED -> "Stopped"
            Constants.LOCAL_VIDEO_STREAM_STATE_CAPTURING -> "Capturing"
            Constants.LOCAL_VIDEO_STREAM_STATE_ENCODING -> "Encoding"
            Constants.LOCAL_VIDEO_STREAM_STATE_FAILED -> "Failed"
            else -> "Unknown"
        }

    private fun describeLocalVideoError(error: Int): String =
        when (error) {
            0 -> "Ok"
            1 -> "Failure"
            2 -> "NoPermission"
            3 -> "DeviceBusy"
            4 -> "CaptureFailure"
            5 -> "EncodeFailure"
            else -> "Unknown"
        }
}
