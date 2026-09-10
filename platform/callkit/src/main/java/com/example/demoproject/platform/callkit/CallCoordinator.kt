package com.example.demoproject.platform.callkit

import com.example.demoproject.platform.callkit.signaling.CallSignalingClient
import com.example.demoproject.platform.callkit.signaling.EndReason
import com.example.demoproject.platform.callkit.signaling.OutgoingInviteRequest
import com.example.demoproject.platform.callkit.signaling.RejectReason
import com.example.demoproject.platform.callkit.signaling.SignalingEvent
import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.rtc.api.RtcClient
import com.example.demoproject.platform.rtc.api.RtcEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 1v1 call state machine that bridges signaling (MQTT) and RTC (Agora).
 *
 * Intentionally transport-agnostic: signaling is injected via [CallSignalingClient].
 */
class CallCoordinator(
    private val scope: CoroutineScope,
    private val signaling: CallSignalingClient,
    private val rtc: RtcClient,
) {
    private val _state = MutableStateFlow<CallState>(CallState.Idle)
    val state: StateFlow<CallState> = _state.asStateFlow()
    private var localHangupRequested: Boolean = false

    init {
        scope.launch {
            AppLogger.d(TAG, "start collecting signaling events")
            signaling.events.collectLatest { onSignalingEvent(it) }
        }
        scope.launch {
            AppLogger.d(TAG, "start collecting rtc events")
            rtc.events.collectLatest { onRtcEvent(it) }
        }
    }

    /**
     * Resets terminal state to [CallState.Idle] so a new call can start.
     *
     * CallCoordinator is app-scoped (singleton), so without this reset a previous
     * `Ended` state can leak into the next call screen instance and cause an
     * immediate "exit" behavior.
     */
    fun resetIfTerminal() {
        when (_state.value) {
            is CallState.Ended -> {
                AppLogger.d(TAG, "resetIfTerminal: Ended -> Idle")
                _state.value = CallState.Idle
                localHangupRequested = false
            }
            else -> Unit
        }
    }

    fun startOutgoingCall(request: OutgoingInviteRequest) {
        if (_state.value !is CallState.Idle && _state.value !is CallState.Ended) {
            AppLogger.w(TAG, "startOutgoingCall ignored currentState=${_state.value}")
            return
        }
        if (_state.value is CallState.Ended) _state.value = CallState.Idle
        AppLogger.d(
            TAG,
            "startOutgoingCall inviteId=${request.inviteId} channel=${request.channelId} " +
                "uid=${request.rtcUid} tokenLen=${request.rtcToken.length}",
        )
        _state.value = CallState.OutgoingRinging(
            inviteId = request.inviteId,
            channelId = request.channelId,
            localRtcUid = request.rtcUid,
            localRtcToken = request.rtcToken,
        )
        scope.launch {
            signaling.sendInvite(request)
        }
    }

    /**
     * Transition to [CallState.Connecting] after incoming accept.
     *
     * The actual RTC join is driven by `VideoCallViewModel.startCall()` after
     * `AgoraRtcService.ensureEngine(...)` succeeds. This avoids a race where
     * the incoming screen pushes coordinator state before Agora engine init.
     */
    fun acceptIncoming(
        inviteId: String,
        channelId: String,
        uid: Int,
        token: String,
        roomSessionId: Long = 0L,
    ) {
        val current = _state.value
        if (current !is CallState.IncomingRinging) {
            AppLogger.w(TAG, "acceptIncoming ignored currentState=${_state.value}")
            return
        }
        AppLogger.d(TAG, "acceptIncoming inviteId=$inviteId channel=$channelId uid=$uid tokenLen=${token.length}")
        _state.value = CallState.Connecting(
            callId = inviteId,
            channelId = channelId,
            roomSessionId = roomSessionId.takeIf { it > 0L } ?: current.roomSessionId,
        )
    }

    /**
     * Bind a server-created room/channel to this coordinator when the RTC join flow is
     * started outside of [startOutgoingCall]/[acceptIncoming] (e.g. VM direct join path).
     *
     * This keeps signaling `CallEnded(callId=room_id)` matchable so remote hangup can end
     * the current call screen.
     */
    fun bindCallSession(callId: String, channelId: String, roomSessionId: Long = 0L) {
        when (_state.value) {
            is CallState.Idle,
            is CallState.Ended,
            -> {
                _state.value = CallState.Connecting(
                    callId = callId,
                    channelId = channelId,
                    roomSessionId = roomSessionId,
                )
                AppLogger.d(TAG, "bindCallSession callId=$callId channelId=$channelId sessionId=$roomSessionId")
            }
            is CallState.Connecting -> {
                val current = _state.value as CallState.Connecting
                if (current.callId == callId && current.channelId != channelId) {
                    AppLogger.d(
                        TAG,
                        "bindCallSession update connecting channel ${current.channelId} -> $channelId callId=$callId",
                    )
                    _state.value = current.copy(
                        channelId = channelId,
                        roomSessionId = roomSessionId.takeIf { it > 0L } ?: current.roomSessionId,
                    )
                }
            }
            // If already in a call flow, keep current state unchanged.
            is CallState.OutgoingRinging,
            is CallState.IncomingRinging,
            is CallState.InCall,
            -> Unit
        }
    }

    /**
     * Reject an incoming call and reset state to [CallState.Ended].
     *
     * **Signaling responsibility**: the caller is expected to send the reject signal
     * BEFORE calling this (e.g. via `MqttCallSignalingClient.rejectTo`).
     */
    fun rejectIncoming(inviteId: String, reason: RejectReason) {
        if (_state.value !is CallState.IncomingRinging) {
            AppLogger.w(TAG, "rejectIncoming ignored currentState=${_state.value}")
            return
        }
        AppLogger.d(TAG, "rejectIncoming inviteId=$inviteId reason=$reason")
        _state.value = CallState.Ended(reason = EndReason.Unknown)
        rtc.leave()
    }

    fun hangup() {
        val s = _state.value
        AppLogger.d(TAG, "hangup from state=$s")
        localHangupRequested = true
        _state.value = CallState.Ended(reason = EndReason.Hangup)
        when (s) {
            is CallState.OutgoingRinging -> {
                scope.launch {
                    AppLogger.d("CallCoordinator", "hangup -> cancel inviteId=${s.inviteId}")
                    signaling.cancel(s.inviteId)
                }
            }
            is CallState.IncomingRinging -> {
                scope.launch {
                    AppLogger.d("CallCoordinator", "hangup -> reject inviteId=${s.inviteId}")
                    signaling.reject(s.inviteId, RejectReason.Declined)
                }
            }
            is CallState.Connecting -> {
                scope.launch {
                    AppLogger.d("CallCoordinator", "hangup -> end connecting callId=${s.callId}")
                    signaling.end(s.callId, EndReason.Hangup)
                }
            }
            is CallState.InCall -> {
                scope.launch {
                    AppLogger.d("CallCoordinator", "hangup -> end inCall callId=${s.callId}")
                    signaling.end(s.callId, EndReason.Hangup)
                }
            }
            else -> Unit
        }
        rtc.leave()
    }

    /**
     * Terminates call state locally without emitting signaling end/cancel/reject.
     *
     * Used when backend/transport already indicates terminal state (e.g. heartbeat status=0),
     * to avoid sending a redundant `/call/end` that may be interpreted as user hangup.
     */
    fun endBySystem(reason: EndReason) {
        val s = _state.value
        if (!isCallActiveState(s)) {
            AppLogger.d(TAG, "endBySystem ignored state=$s reason=$reason")
            return
        }
        AppLogger.d(TAG, "endBySystem from state=$s reason=$reason")
        localHangupRequested = false
        _state.value = CallState.Ended(reason = reason)
        rtc.leave()
    }

    private fun isCallActiveState(state: CallState): Boolean = when (state) {
        is CallState.OutgoingRinging,
        is CallState.IncomingRinging,
        is CallState.Connecting,
        is CallState.InCall,
        -> true
        else -> false
    }

    private fun onSignalingEvent(e: SignalingEvent) {
        AppLogger.d(TAG, "onSignalingEvent event=$e currentState=${_state.value}")
        when (e) {
            is SignalingEvent.IncomingInvite -> {
                val busy = when (_state.value) {
                    is CallState.OutgoingRinging,
                    is CallState.IncomingRinging,
                    is CallState.Connecting,
                    is CallState.InCall,
                    -> true
                    else -> false
                }
                if (busy) {
                    // Some backends re-deliver the same invite / connect envelope on the caller's
                    // MQTT topic after the callee answers. Treating it as a second invite would
                    // call `reject` → `POST /call/end` on the *active* room_id and drop the live call.
                    val duplicateForActiveSession = when (val cur = _state.value) {
                        is CallState.InCall ->
                            matchesCallRoomId(cur.callId, cur.roomSessionId, e.inviteId)
                        is CallState.Connecting ->
                            matchesCallRoomId(cur.callId, cur.roomSessionId, e.inviteId)
                        is CallState.OutgoingRinging ->
                            matchesCallRoomId(cur.inviteId, cur.roomSessionId, e.inviteId)
                        is CallState.IncomingRinging ->
                            matchesCallRoomId(cur.inviteId, cur.roomSessionId, e.inviteId)
                        else -> false
                    }
                    if (duplicateForActiveSession) {
                        AppLogger.d(
                            TAG,
                            "IncomingInvite ignored as duplicate for active session inviteId=${e.inviteId} state=${_state.value}",
                        )
                        return
                    }
                    AppLogger.w(TAG, "IncomingInvite while busy state=${_state.value}, auto reject busy")
                    scope.launch { signaling.reject(e.inviteId, RejectReason.Busy) }
                    return
                }
                _state.value = CallState.IncomingRinging(
                    inviteId = e.inviteId,
                    callerUserId = e.callerUserId,
                    callerName = e.callerName,
                    callerAvatar = e.callerAvatar,
                    callType = e.callType,
                    channelId = e.channelId,
                    rtcUid = e.rtcUid,
                    rtcToken = e.rtcToken,
                    rtcAppId = e.rtcAppId,
                    roomSessionId = e.roomSessionId,
                    callFreeMin = e.callFreeMin,
                )
            }

            is SignalingEvent.InviteAccepted -> {
                val current = _state.value
                if (current is CallState.OutgoingRinging &&
                    matchesCallRoomId(current.inviteId, current.roomSessionId, e.inviteId)
                ) {
                    _state.value = CallState.Connecting(
                        callId = e.callId.ifBlank { current.inviteId },
                        channelId = current.channelId,
                        roomSessionId = current.roomSessionId,
                    )
                    // Caller joins the channel after the callee accepts.
                    scope.launch {
                        rtc.join(
                            channelId = current.channelId,
                            uid = current.localRtcUid,
                            token = current.localRtcToken,
                        )
                    }
                }
            }

            is SignalingEvent.InviteRejected -> {
                val current = _state.value
                if (current is CallState.OutgoingRinging &&
                    matchesCallRoomId(current.inviteId, current.roomSessionId, e.inviteId)
                ) {
                    _state.value = CallState.Ended(reason = EndReason.Unknown)
                    rtc.leave()
                }
            }

            is SignalingEvent.InviteCancelled -> {
                val current = _state.value
                if (current is CallState.IncomingRinging &&
                    matchesCallRoomId(current.inviteId, current.roomSessionId, e.inviteId)
                ) {
                    _state.value = CallState.Ended(reason = EndReason.Timeout)
                    rtc.leave()
                }
            }

            is SignalingEvent.CallEnded -> {
                val current = _state.value
                when (current) {
                    is CallState.InCall -> {
                        if (e.reason == EndReason.Hangup && !localHangupRequested) {
                            AppLogger.w(
                                TAG,
                                "ignore unexpected self-hangup signaling callId=${e.callId} state=$current",
                            )
                            return
                        }
                        if (matchesCallRoomId(current.callId, current.roomSessionId, e.callId)) {
                            AppLogger.d("CallCoordinator", "CallEnded matched InCall callId=${e.callId}")
                            _state.value = CallState.Ended(reason = e.reason)
                            rtc.leave()
                        }
                    }
                    is CallState.Connecting -> {
                        if (e.reason == EndReason.Hangup && !localHangupRequested) {
                            AppLogger.w(
                                TAG,
                                "ignore unexpected self-hangup signaling while connecting callId=${e.callId}",
                            )
                            return
                        }
                        if (matchesCallRoomId(current.callId, current.roomSessionId, e.callId)) {
                            AppLogger.d("CallCoordinator", "CallEnded matched Connecting callId=${e.callId}")
                            _state.value = CallState.Ended(reason = e.reason)
                            rtc.leave()
                        }
                    }
                    is CallState.OutgoingRinging -> {
                        if (matchesCallRoomId(current.inviteId, current.roomSessionId, e.callId)) {
                            _state.value = CallState.Ended(reason = e.reason)
                            rtc.leave()
                        }
                    }
                    is CallState.IncomingRinging -> {
                        if (matchesCallRoomId(current.inviteId, current.roomSessionId, e.callId)) {
                            _state.value = CallState.Ended(reason = e.reason)
                            rtc.leave()
                        }
                    }
                    else -> Unit
                }
            }

            is SignalingEvent.Error -> {
                // Ignore stale transport errors when we are not in an active call flow.
                if (isCallActiveState(_state.value)) {
                    AppLogger.w(TAG, "SignalingError activeState=${_state.value} message=${e.message}")
                    _state.value = CallState.Ended(reason = EndReason.NetworkError)
                }
            }
        }
    }

    private fun onRtcEvent(e: RtcEvent) {
        AppLogger.d(TAG, "onRtcEvent event=$e currentState=${_state.value}")
        when (e) {
            is RtcEvent.JoinedChannel -> {
                val current = _state.value
                if (current is CallState.Connecting && current.channelId == e.channelId) {
                    _state.value = CallState.InCall(
                        callId = current.callId,
                        channelId = e.channelId,
                        localUid = e.localUid,
                        roomSessionId = current.roomSessionId,
                    )
                }
            }

            is RtcEvent.Error -> {
                // RTC emits late errors during teardown/release; they must not force-close
                // a newly opened call screen when coordinator is idle.
                if (_state.value is CallState.InCall) {
                    AppLogger.w(TAG, "RtcError in call code=${e.code} message=${e.message}")
                    _state.value = CallState.Ended(reason = EndReason.NetworkError)
                }
            }

            else -> Unit
        }
    }
}

sealed interface CallState {
    data object Idle : CallState

    data class OutgoingRinging(
        val inviteId: String,
        val channelId: String,
        val localRtcUid: Int = 0,
        val localRtcToken: String = "",
        val roomSessionId: Long = 0L,
    ) : CallState

    data class IncomingRinging(
        val inviteId: String,
        val callerUserId: String,
        val callerName: String = "",
        val callerAvatar: String = "",
        val callType: Int = CallMediaType.Video,
        val channelId: String,
        val rtcToken: String,
        val rtcUid: Int,
        val rtcAppId: String = "",
        val roomSessionId: Long = 0L,
        /**
         * `user_info.call_free_min` from the invite push — `> 0` means this incoming call is
         * free, per product rule. Fixed at invite time, never re-derived later.
         */
        val callFreeMin: Int = 0,
    ) : CallState

    data class Connecting(
        val callId: String,
        val channelId: String,
        val roomSessionId: Long = 0L,
    ) : CallState

    data class InCall(
        val callId: String,
        val channelId: String,
        val localUid: Int,
        val roomSessionId: Long = 0L,
    ) : CallState

    data class Ended(val reason: EndReason) : CallState
}

/** True when [endedCallId] refers to the active call by HTTP room key or numeric session id. */
fun matchesCallRoomId(
    activeCallId: String,
    activeRoomSessionId: Long,
    endedCallId: String,
): Boolean {
    val ended = endedCallId.trim()
    if (ended.isEmpty()) return false
    val active = activeCallId.trim()
    if (active.isNotEmpty() && active == ended) return true
    if (activeRoomSessionId > 0L && ended == activeRoomSessionId.toString()) return true
    return false
}

private const val TAG = "CallCoordinator"

