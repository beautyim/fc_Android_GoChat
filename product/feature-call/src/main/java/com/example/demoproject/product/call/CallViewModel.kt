package com.example.demoproject.product.call

import android.app.Activity
import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.callkit.CallKitHolder
import com.example.demoproject.platform.callkit.CallState
import com.example.demoproject.platform.callkit.matchesCallRoomId
import com.example.demoproject.platform.callkit.signaling.EndReason
import com.example.demoproject.platform.callkit.signaling.OutgoingInviteRequest
import com.example.demoproject.platform.callkit.signaling.SignalingCoinOffer
import com.example.demoproject.platform.callkit.signaling.SignalingEvent
import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.billing.StorePurchaseLauncherHolder
import com.example.demoproject.platform.data.model.CallRoom
import com.example.demoproject.platform.data.model.GiftFromType
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.network.dto.GiftSendRequestDto
import com.example.demoproject.platform.data.network.mapper.toRechargePageDataOrNull
import com.example.demoproject.platform.data.network.toChatBinaryUrlOrNull
import com.example.demoproject.platform.data.network.toPicUrlOrNull
import com.example.demoproject.platform.data.repository.BillingPaymentType
import com.example.demoproject.platform.data.repository.BillingProductType
import com.example.demoproject.platform.data.repository.CallCreateResult
import com.example.demoproject.platform.data.repository.CallRenewTokenResult
import com.example.demoproject.platform.data.repository.RechargePageData
import com.example.demoproject.platform.data.repository.RechargeProduct
import com.example.demoproject.platform.data.repository.RechargeSaleItem
import com.example.demoproject.platform.data.repository.StorePurchaseRequest
import com.example.demoproject.platform.data.repository.StorePurchaseResult
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.INSUFFICIENT_BALANCE_ERROR_CODE
import com.example.demoproject.platform.network.result.MSG_SEND_INSUFFICIENT_BALANCE_ERROR_CODE
import com.example.demoproject.platform.network.result.isInsufficientBalance
import com.example.demoproject.platform.rtc.api.RtcCallPermissions
import com.example.demoproject.platform.rtc.api.RtcEvent
import com.example.demoproject.product.store.CoinPayGuideUiState
import com.example.demoproject.product.store.R as StoreR
import com.example.demoproject.product.store.toCoinPayGuideUiState
import com.example.demoproject.ui.designsystem.gift.GiftSvgaPreloader
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield

class CallViewModel(
    application: Application,
    private val targetUserId: String = "",
    initialNickname: String = "",
    initialAvatarUrl: String = "",
    initialAge: Int = 0,
    initialVideoUrl: String = "",
    initialCoverUrl: String = "",
) : AndroidViewModel(application) {
    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(
        CallUiState(
            peerUserId = targetUserId,
            peerNickname = initialNickname,
            peerAge = initialAge,
            peerAvatarUrl = initialAvatarUrl.toPicUrlOrNull().orEmpty()
                .ifBlank { initialAvatarUrl },
            videoUrl = initialVideoUrl.toChatBinaryUrlOrNull().orEmpty()
                .ifBlank { initialVideoUrl },
            coverUrl = initialCoverUrl.toPicUrlOrNull().orEmpty()
                .ifBlank { initialCoverUrl },
        ),
    )
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CallEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var hostActivity: Activity? = null
    private var outgoingStarted: Boolean = false
    private var isOutgoingCaller: Boolean = false
    private var fencingToken: String? = null
    private var callerSuccessReported: Boolean = false
    private var roomSessionId: Long = 0L
    private var giftCatalogJob: Job? = null
    private var durationJob: Job? = null
    private var heartJob: Job? = null
    private var answerStatusJob: Job? = null
    private var renewJob: Job? = null
    private var giftRequestJob: Job? = null
    private var inCallBootstrapped: Boolean = false
    private var stopTokenRenewal: Boolean = false
    /** When true, dismissing [CallUiState.coinPayGuide] also exits the call screen. */
    private var exitAfterCoinPayGuideDismiss: Boolean = false

    init {
        viewModelScope.launch {
            runtime.accountBalanceStore.coins.collect { coins ->
                _uiState.update { it.copy(coinBalance = coins) }
            }
        }
        viewModelScope.launch {
            val coordinator = CallKitHolder.coordinator
            if (coordinator == null) {
                _uiState.update {
                    it.copy(
                        phase = CallRingingPhase.Ended,
                        errorMessage = str(R.string.call_status_coordinator_missing),
                    )
                }
                _effects.send(CallEffect.ShowMessage(str(R.string.call_status_coordinator_missing)))
                _effects.send(CallEffect.Exit)
                return@launch
            }
            coordinator.resetIfTerminal()
            coordinator.state.collect { state ->
                applyCoordinatorState(state)
            }
        }
        viewModelScope.launch {
            CallKitHolder.rtc?.events?.collect { event ->
                onRtcEvent(event)
            }
        }
        viewModelScope.launch {
            CallKitHolder.signaling?.events?.collect { event ->
                onSignalingUxEvent(event)
            }
        }
        viewModelScope.launch {
            maybeStartOutgoing()
        }
    }

    fun bindActivity(activity: Activity?) {
        hostActivity = activity
    }

    fun onIntent(intent: CallIntent) {
        when (intent) {
            CallIntent.Hangup -> hangup()
            CallIntent.Answer -> answer()
            CallIntent.Report -> openReportSheet()
            CallIntent.DismissReport -> _uiState.update {
                it.copy(isReportSheetVisible = false, report = null)
            }
            is CallIntent.ToggleReportReason -> toggleReportReason(intent.reasonId)
            CallIntent.SubmitReport -> submitReport()
            CallIntent.Like -> likePeer()
            CallIntent.OpenMore -> _uiState.update { it.copy(isMoreSheetVisible = true) }
            CallIntent.DismissMore -> _uiState.update { it.copy(isMoreSheetVisible = false) }
            is CallIntent.SetMicEnabled -> {
                CallKitHolder.rtc?.setMicMuted(!intent.enabled)
                _uiState.update { it.copy(micEnabled = intent.enabled) }
            }
            is CallIntent.SetCameraEnabled -> setCameraEnabled(intent.enabled)
            CallIntent.FlipCamera -> {
                CallKitHolder.rtc?.switchCamera()
            }
            CallIntent.OpenGiftSheet -> openGiftSheet()
            CallIntent.DismissGiftSheet -> _uiState.update { it.copy(isGiftSheetVisible = false) }
            is CallIntent.SelectGift -> _uiState.update { it.copy(selectedGiftId = intent.giftId) }
            CallIntent.SendSelectedGift -> sendGift(selectedOnly = true, dismissQuickBar = true)
            is CallIntent.SendQuickGift -> sendGift(giftId = intent.giftId, dismissQuickBar = true)
            is CallIntent.DraftChanged -> _uiState.update { it.copy(draftMessage = intent.text) }
            CallIntent.SendMessage -> sendCallMessage()
            is CallIntent.ToggleMessageTranslation -> toggleTranslation(intent.messageId)
            CallIntent.DismissGiftAnimation -> _uiState.update { it.copy(giftAnimationUrl = null) }
            CallIntent.DismissGiftSentTip -> _uiState.update { it.copy(giftSentTip = null) }
            CallIntent.OpenCoins -> viewModelScope.launch { _effects.send(CallEffect.OpenStore) }
            CallIntent.DismissCoinPayGuide -> dismissCoinPayGuide()
            is CallIntent.PurchaseCoinPayGuideCoin -> purchaseCoinPayGuide(intent.offerId, isSale = false)
            is CallIntent.PurchaseCoinPayGuideSale -> purchaseCoinPayGuide(intent.offerId, isSale = true)
        }
    }

    private suspend fun maybeStartOutgoing() {
        if (targetUserId.isBlank()) return
        val target = targetUserId.toLongOrNull()?.takeIf { it > 0L } ?: run {
            _uiState.update {
                it.copy(
                    phase = CallRingingPhase.Ended,
                    errorMessage = str(R.string.call_status_no_peer),
                )
            }
            _effects.send(CallEffect.Exit)
            return
        }
        val coordinator = CallKitHolder.coordinator ?: return
        if (outgoingStarted) return
        when (coordinator.state.value) {
            is CallState.IncomingRinging,
            is CallState.OutgoingRinging,
            is CallState.Connecting,
            is CallState.InCall,
            -> return
            else -> Unit
        }
        outgoingStarted = true
        isOutgoingCaller = true
        _uiState.update { it.copy(phase = CallRingingPhase.Preparing) }
        when (
            val created = runtime.callSessionRepository.createCall(
                targetUid = target,
                callType = CallRoom.CALL_TYPE_VIDEO,
                fromType = CallRoom.FROM_HOME,
            )
        ) {
            is CallCreateResult.Failure -> {
                _uiState.update {
                    it.copy(
                        phase = CallRingingPhase.Ended,
                        errorMessage = created.message.ifBlank {
                            str(
                                R.string.call_status_create_failed_fmt,
                                created.code.toString(),
                                created.message,
                            )
                        },
                    )
                }
                if (created.isInsufficientBalance()) {
                    // Stay on call screen so CoinPayGuideSheet can show; exit on dismiss.
                    showCoinPayGuideForCreate(created)
                } else {
                    _effects.send(
                        CallEffect.ShowMessage(
                            created.message.ifBlank {
                                str(
                                    R.string.call_status_create_failed_fmt,
                                    created.code.toString(),
                                    "",
                                )
                            },
                        ),
                    )
                    _effects.send(CallEffect.Exit)
                }
            }
            is CallCreateResult.Success -> {
                val room = created.room
                fencingToken = room.fencingToken
                roomSessionId = room.roomId
                val peer = room.peer
                val show = peer?.videoShow
                _uiState.update {
                    it.copy(
                        phase = CallRingingPhase.Outgoing,
                        peerUserId = peer?.id ?: targetUserId,
                        peerNickname = peer?.nickname?.takeIf { n -> n.isNotBlank() }
                            ?: it.peerNickname,
                        peerAge = peer?.age?.takeIf { a -> a > 0 } ?: it.peerAge,
                        peerAvatarUrl = peer?.avatar?.takeIf { a -> a.isNotBlank() }
                            ?: it.peerAvatarUrl,
                        videoUrl = show?.videoUrl?.takeIf { u -> u.isNotBlank() } ?: it.videoUrl,
                        coverUrl = show?.coverUrl?.takeIf { u -> !u.isNullOrBlank() }
                            ?: it.coverUrl,
                        callRoomId = room.effectiveHttpRoomId,
                    )
                }
                coordinator.resetIfTerminal()
                coordinator.startOutgoingCall(
                    OutgoingInviteRequest(
                        inviteId = room.effectiveHttpRoomId,
                        calleeUserId = target.toString(),
                        channelId = room.channel.ifBlank { room.roomId.toString() },
                        rtcToken = room.token,
                        rtcUid = room.uid,
                        rtcAppId = room.appId,
                        roomSessionId = room.roomId,
                        fencingToken = room.fencingToken,
                        peerNickname = _uiState.value.peerNickname,
                        peerAvatarUrl = _uiState.value.peerAvatarUrl,
                        peerAge = _uiState.value.peerAge,
                        peerVideoUrl = _uiState.value.videoUrl,
                        peerCoverUrl = _uiState.value.coverUrl,
                    ),
                )
                _uiState.update { it.copy(rtcSurfacesActive = true) }
                startAnswerStatusPolling(room.effectiveHttpRoomId)
            }
        }
    }

    private fun startAnswerStatusPolling(roomId: String) {
        answerStatusJob?.cancel()
        answerStatusJob = viewModelScope.launch {
            var totalWaitMs = DEFAULT_ANSWER_TIMEOUT_MS
            var elapsedMs = 0L
            delay(ANSWER_STATUS_FIRST_DELAY_MS)
            elapsedMs += ANSWER_STATUS_FIRST_DELAY_MS
            while (isActive) {
                val phase = _uiState.value.phase
                if (phase != CallRingingPhase.Outgoing && phase != CallRingingPhase.Preparing) {
                    return@launch
                }
                if (elapsedMs >= totalWaitMs) {
                    AppLogger.d(TAG, "answer-status timeout roomId=$roomId")
                    CallKitHolder.coordinator?.hangup()
                    return@launch
                }
                when (
                    val result = withTimeoutOrNull(ANSWER_STATUS_REQUEST_TIMEOUT_MS) {
                        runtime.callSessionRepository.fetchAnswerStatus(roomId)
                    }
                ) {
                    is AppResult.Success -> {
                        val status = result.data
                        if (status.answerTimeoutSec > 0) {
                            totalWaitMs = status.answerTimeoutSec * 1_000L
                        }
                        when {
                            status.isAnswered -> {
                                AppLogger.d(TAG, "answer-status answered roomId=$roomId")
                                CallKitHolder.coordinator?.notifyPeerAnswered(roomId)
                                return@launch
                            }
                            status.isEnded -> {
                                AppLogger.d(TAG, "answer-status ended roomId=$roomId")
                                CallKitHolder.coordinator?.hangup()
                                return@launch
                            }
                            else -> Unit
                        }
                    }
                    null -> AppLogger.d(TAG, "answer-status request timeout roomId=$roomId")
                    is AppResult.Failure ->
                        AppLogger.d(TAG, "answer-status fail roomId=$roomId msg=${result.message}")
                }
                delay(ANSWER_STATUS_INTERVAL_MS)
                elapsedMs += ANSWER_STATUS_INTERVAL_MS
            }
        }
    }

    private fun onRtcEvent(event: RtcEvent) {
        when (event) {
            is RtcEvent.JoinedChannel -> {
                _uiState.update { it.copy(rtcSurfacesActive = true) }
            }
            is RtcEvent.RemoteUserJoined -> {
                val mediaSessionLive = when (CallKitHolder.coordinator?.state?.value) {
                    is CallState.Connecting,
                    is CallState.InCall,
                    -> true
                    else -> false
                }
                _uiState.update {
                    val enterInCall = mediaSessionLive || it.rtcSurfacesActive
                    it.copy(
                        remoteRtcUid = event.uid,
                        // Enter in-call UI only after the remote peer joins the RTC channel.
                        phase = if (enterInCall) CallRingingPhase.InCall else it.phase,
                        rtcSurfacesActive = true,
                    )
                }
                if (_uiState.value.phase == CallRingingPhase.InCall) {
                    bootstrapInCall()
                }
                maybeReportCallerSuccess()
            }
            is RtcEvent.RemoteUserLeft -> {
                if (_uiState.value.remoteRtcUid == event.uid) {
                    _uiState.update { it.copy(remoteRtcUid = 0) }
                }
            }
            is RtcEvent.TokenWillExpire -> {
                viewModelScope.launch { renewTokenOnce(fromSdk = true) }
            }
            else -> Unit
        }
    }

    private fun maybeReportCallerSuccess() {
        if (!isOutgoingCaller || callerSuccessReported) return
        val roomId = _uiState.value.callRoomId
        if (roomId.isBlank()) return
        if (_uiState.value.remoteRtcUid <= 0) return
        callerSuccessReported = true
        viewModelScope.launch {
            when (
                val result = runtime.callSessionRepository.reportCallSuccess(
                    roomId = roomId,
                    fencingToken = fencingToken,
                )
            ) {
                is AppResult.Success -> {
                    result.data?.fencingToken?.let { fencingToken = it }
                    AppLogger.d(TAG, "caller /call/success ok roomId=$roomId")
                    startHeartbeat(roomId)
                    scheduleTokenRenewal(roomId)
                }
                is AppResult.Failure -> {
                    callerSuccessReported = false
                    AppLogger.w(TAG, "caller /call/success failed: ${result.message}")
                }
            }
        }
    }

    private fun applyCoordinatorState(state: CallState) {
        when (state) {
            is CallState.IncomingRinging -> {
                isOutgoingCaller = false
                fencingToken = state.fencingToken ?: fencingToken
                roomSessionId = state.roomSessionId.takeIf { it > 0L } ?: roomSessionId
                _uiState.update {
                    it.copy(
                        phase = CallRingingPhase.Incoming,
                        peerUserId = state.callerUserId,
                        peerNickname = state.callerName.ifBlank { it.peerNickname },
                        peerAge = state.callerAge.takeIf { a -> a > 0 } ?: it.peerAge,
                        peerAvatarUrl = state.callerAvatar.toPicUrlOrNull()
                            ?: state.callerAvatar.takeIf { a -> a.isNotBlank() }
                            ?: it.peerAvatarUrl,
                        videoUrl = state.peerVideoUrl.toChatBinaryUrlOrNull()
                            ?: state.peerVideoUrl.takeIf { u -> u.isNotBlank() }
                            ?: it.videoUrl,
                        coverUrl = state.peerCoverUrl.toPicUrlOrNull()
                            ?: state.peerCoverUrl.takeIf { u -> u.isNotBlank() }
                            ?: it.coverUrl,
                        callRoomId = state.inviteId.ifBlank { it.callRoomId },
                        errorMessage = null,
                    )
                }
            }
            is CallState.OutgoingRinging -> {
                isOutgoingCaller = true
                fencingToken = state.fencingToken ?: fencingToken
                roomSessionId = state.roomSessionId.takeIf { it > 0L } ?: roomSessionId
                _uiState.update {
                    it.copy(
                        phase = CallRingingPhase.Outgoing,
                        peerUserId = state.peerUserId.ifBlank { it.peerUserId },
                        peerNickname = state.peerNickname.ifBlank { it.peerNickname },
                        peerAge = state.peerAge.takeIf { a -> a > 0 } ?: it.peerAge,
                        peerAvatarUrl = state.peerAvatarUrl.toPicUrlOrNull()
                            ?: state.peerAvatarUrl.takeIf { a -> a.isNotBlank() }
                            ?: it.peerAvatarUrl,
                        videoUrl = state.peerVideoUrl.toChatBinaryUrlOrNull()
                            ?: state.peerVideoUrl.takeIf { u -> u.isNotBlank() }
                            ?: it.videoUrl,
                        coverUrl = state.peerCoverUrl.toPicUrlOrNull()
                            ?: state.peerCoverUrl.takeIf { u -> u.isNotBlank() }
                            ?: it.coverUrl,
                        callRoomId = state.inviteId.ifBlank { it.callRoomId },
                        errorMessage = null,
                    )
                }
            }
            is CallState.Connecting -> {
                answerStatusJob?.cancel()
                fencingToken = state.fencingToken ?: fencingToken
                roomSessionId = state.roomSessionId.takeIf { it > 0L } ?: roomSessionId
                _uiState.update {
                    it.copy(
                        // Stay on Incoming/Outgoing until remote peer joins — never show Connecting.
                        phase = phaseWhileWaitingForRemote(it),
                        callRoomId = state.callId.ifBlank { it.callRoomId },
                        rtcSurfacesActive = true,
                    )
                }
                if (_uiState.value.remoteRtcUid > 0) {
                    bootstrapInCall()
                }
            }
            is CallState.InCall -> {
                answerStatusJob?.cancel()
                fencingToken = state.fencingToken ?: fencingToken
                roomSessionId = state.roomSessionId.takeIf { it > 0L } ?: roomSessionId
                _uiState.update {
                    it.copy(
                        phase = phaseWhileWaitingForRemote(it),
                        callRoomId = state.callId.ifBlank { it.callRoomId },
                        rtcSurfacesActive = true,
                    )
                }
                if (_uiState.value.remoteRtcUid > 0) {
                    bootstrapInCall()
                }
            }
            is CallState.Ended -> {
                stopInCallJobs()
                _uiState.update {
                    it.copy(
                        phase = CallRingingPhase.Ended,
                        rtcSurfacesActive = false,
                        remoteRtcUid = 0,
                    )
                }
                when {
                    state.reason == EndReason.InsufficientBalance -> {
                        exitAfterCoinPayGuideDismiss = true
                        presentCoinPayGuide(fromCallback = null)
                    }
                    _uiState.value.coinPayGuide != null -> {
                        // Keep screen until the user dismisses the recharge guide.
                        exitAfterCoinPayGuideDismiss = true
                    }
                    else -> viewModelScope.launch { _effects.send(CallEffect.Exit) }
                }
            }
            CallState.Idle -> {
                if (targetUserId.isBlank() &&
                    _uiState.value.phase == CallRingingPhase.Preparing &&
                    !outgoingStarted
                ) {
                    // Incoming route opened before invite applied — wait for IncomingRinging.
                }
            }
        }
    }

    private fun phaseWhileWaitingForRemote(state: CallUiState): CallRingingPhase {
        if (state.remoteRtcUid > 0) return CallRingingPhase.InCall
        return when (state.phase) {
            CallRingingPhase.Incoming -> CallRingingPhase.Incoming
            CallRingingPhase.Outgoing,
            CallRingingPhase.Preparing,
            CallRingingPhase.Connecting,
            -> if (isOutgoingCaller) CallRingingPhase.Outgoing else CallRingingPhase.Incoming
            CallRingingPhase.InCall ->
                if (isOutgoingCaller) CallRingingPhase.Outgoing else CallRingingPhase.Incoming
            CallRingingPhase.Ended -> CallRingingPhase.Ended
        }
    }

    private fun bootstrapInCall() {
        if (inCallBootstrapped) return
        inCallBootstrapped = true
        _uiState.update {
            it.copy(
                showGiftQuickBar = true,
                likePhase = CallLikePhase.Visible,
                chatItems = listOf(CallChatItem.SystemTips()),
            )
        }
        startDurationTicker()
        loadGiftCatalog(notifyFailure = false)
        val roomId = _uiState.value.callRoomId
        if (roomId.isNotBlank()) {
            // Callee starts heart on InCall; caller also starts after success — safe to start here
            // once, and again after caller success if not yet running.
            if (!isOutgoingCaller || callerSuccessReported) {
                startHeartbeat(roomId)
                scheduleTokenRenewal(roomId)
            }
        }
    }

    private fun startHeartbeat(roomId: String) {
        if (heartJob?.isActive == true) return
        heartJob = viewModelScope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                when (
                    val result = runtime.callSessionRepository.heartbeat(
                        roomId = roomId,
                        fencingToken = fencingToken,
                    )
                ) {
                    is AppResult.Success -> {
                        if (!result.data) {
                            AppLogger.d(TAG, "heartbeat inactive roomId=$roomId")
                            // Ended(InsufficientBalance) opens CoinPayGuide; exit on dismiss.
                            CallKitHolder.coordinator?.endBySystem(EndReason.InsufficientBalance)
                            return@launch
                        }
                    }
                    is AppResult.Failure ->
                        AppLogger.d(TAG, "heartbeat fail roomId=$roomId msg=${result.message}")
                }
            }
        }
    }

    private fun scheduleTokenRenewal(roomId: String, delayMs: Long = 0L) {
        if (stopTokenRenewal) return
        renewJob?.cancel()
        renewJob = viewModelScope.launch {
            if (delayMs > 0L) delay(delayMs)
            renewTokenOnce(fromSdk = false)
        }
    }

    private suspend fun renewTokenOnce(fromSdk: Boolean) {
        if (stopTokenRenewal) return
        val roomId = _uiState.value.callRoomId
        if (roomId.isBlank()) return
        var backoffMs = 0L
        repeat(4) { attempt ->
            if (backoffMs > 0L) delay(backoffMs)
            when (
                val result = runtime.callSessionRepository.renewRtcToken(
                    roomId = roomId,
                    fencingToken = fencingToken,
                )
            ) {
                is CallRenewTokenResult.Renewed -> {
                    CallKitHolder.rtc?.renewToken(result.token)
                    val nextDelay = computeRenewDelayMs(result)
                    AppLogger.d(
                        TAG,
                        "renew-token ok fromSdk=$fromSdk attempt=$attempt nextDelayMs=$nextDelay",
                    )
                    if (nextDelay > 0L) {
                        scheduleTokenRenewal(roomId, nextDelay)
                    }
                    return
                }
                CallRenewTokenResult.StopRenewal -> {
                    AppLogger.d(TAG, "renew-token stop (ok=2)")
                    stopTokenRenewal = true
                    return
                }
                is CallRenewTokenResult.Retryable -> {
                    AppLogger.d(TAG, "renew-token retryable attempt=$attempt msg=${result.message}")
                    backoffMs = when (attempt) {
                        0 -> 5_000L
                        1 -> 10_000L
                        else -> 15_000L
                    }
                }
            }
        }
    }

    private fun computeRenewDelayMs(result: CallRenewTokenResult.Renewed): Long {
        val expireAt = result.expireAt
        if (expireAt <= 0L) return 0L
        val nowSec = if (result.serverNow > 0L) {
            result.serverNow
        } else {
            System.currentTimeMillis() / 1_000L
        }
        val ahead = result.renewAheadSec.takeIf { it > 0 } ?: DEFAULT_RENEW_AHEAD_SEC
        val delaySec = expireAt - nowSec - ahead
        return max(delaySec, 5L) * 1_000L
    }

    private fun startDurationTicker() {
        durationJob?.cancel()
        durationJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _uiState.update { it.copy(callDurationSec = it.callDurationSec + 1) }
            }
        }
    }

    private fun stopInCallJobs() {
        durationJob?.cancel()
        heartJob?.cancel()
        answerStatusJob?.cancel()
        renewJob?.cancel()
        giftRequestJob?.cancel()
        giftCatalogJob?.cancel()
        inCallBootstrapped = false
        stopTokenRenewal = false
    }

    private fun likePeer() {
        val state = _uiState.value
        if (state.likePhase != CallLikePhase.Visible) return
        val peerId = state.peerUserId
        if (peerId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(likePhase = CallLikePhase.Liked) }
            when (val result = runtime.profileRepository.followUser(peerId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(likePhase = CallLikePhase.Hidden) }
                    scheduleGiftRequest()
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(likePhase = CallLikePhase.Visible) }
                    _effects.send(CallEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun scheduleGiftRequest() {
        giftRequestJob?.cancel()
        giftRequestJob = viewModelScope.launch {
            delay(Random.nextLong(2_000, 5_001))
            ensureGiftCatalogLoaded()
            val catalog = _uiState.value.gifts.filter { it.id > 0L && it.iconUrl.isNotBlank() }
            val pool = catalog.filter { it.price in 10..50 }.ifEmpty { catalog }
            val pick = pool.randomOrNull() ?: return@launch
            val nickname = _uiState.value.peerNickname.ifBlank {
                str(R.string.call_ringing_unknown_peer)
            }
            val item = CallChatItem.GiftRequest(
                id = "gift_request_${System.currentTimeMillis()}",
                senderName = nickname,
                giftIconUrl = pick.iconUrl,
                giftPrice = pick.price,
                giftId = pick.id,
            )
            _uiState.update {
                it.copy(chatItems = it.chatItems + item)
            }
        }
    }

    private fun openGiftSheet() {
        _uiState.update {
            it.copy(
                isGiftSheetVisible = true,
                isGiftCatalogLoading = it.gifts.isEmpty(),
            )
        }
        if (_uiState.value.gifts.isEmpty()) {
            loadGiftCatalog(notifyFailure = true)
        }
    }

    private suspend fun ensureGiftCatalogLoaded() {
        if (_uiState.value.gifts.isNotEmpty()) return
        loadGiftCatalog(notifyFailure = false)
        giftCatalogJob?.join()
    }

    private fun loadGiftCatalog(notifyFailure: Boolean) {
        if (giftCatalogJob?.isActive == true) return
        giftCatalogJob = viewModelScope.launch {
            _uiState.update { it.copy(isGiftCatalogLoading = true) }
            when (val result = runtime.messageRepository.getGiftConfig()) {
                is AppResult.Success -> {
                    val gifts = result.data.gifts.map { gift ->
                        CallGiftUi(
                            id = gift.id,
                            title = gift.title,
                            price = gift.price,
                            iconUrl = gift.iconUrl,
                            svgaUrl = gift.svgaUrl,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isGiftCatalogLoading = false,
                            gifts = gifts,
                            selectedGiftId = it.selectedGiftId ?: gifts.firstOrNull()?.id,
                        )
                    }
                    val selectedId = _uiState.value.selectedGiftId
                    val url = gifts.firstOrNull { it.id == selectedId }?.svgaUrl
                    GiftSvgaPreloader.prefetch(getApplication(), url)
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isGiftCatalogLoading = false) }
                    if (notifyFailure) {
                        _effects.send(CallEffect.ShowMessage(result.message))
                    }
                }
            }
        }
    }

    private fun sendGift(
        giftId: Long? = null,
        selectedOnly: Boolean = false,
        dismissQuickBar: Boolean,
    ) {
        val state = _uiState.value
        val resolvedId = giftId ?: state.selectedGiftId ?: return
        if (selectedOnly && state.selectedGiftId == null) return
        val selected = state.gifts.firstOrNull { it.id == resolvedId } ?: return
        val peerId = state.peerUserId
        val roomId = state.callRoomId
        if (peerId.isBlank() || roomId.isBlank() || state.isGiftSending) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGiftSending = true, selectedGiftId = resolvedId) }
            GiftSvgaPreloader.prefetch(getApplication(), selected.svgaUrl)
            when (
                val result = runtime.messageRepository.sendGift(
                    conversationId = peerId,
                    giftId = resolvedId,
                    fromType = GiftFromType.CALL,
                    roomId = roomId,
                )
            ) {
                is AppResult.Success -> {
                    val animationUrl = selected.svgaUrl
                        .takeIf { it.isNotBlank() }
                        ?: result.data.gift?.svgaUrl?.takeIf { it.isNotBlank() }
                    _uiState.update {
                        it.copy(
                            isGiftSending = false,
                            isGiftSheetVisible = false,
                            showGiftQuickBar = if (dismissQuickBar) false else it.showGiftQuickBar,
                            giftAnimationUrl = animationUrl,
                            giftSentTip = CallGiftSentTipUi(
                                giftTitle = selected.title,
                                giftIconUrl = selected.iconUrl,
                            ),
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isGiftSending = false) }
                    if (result.isInsufficientBalance()) {
                        maybeShowCoinPayGuide(result)
                    } else {
                        _effects.send(CallEffect.ShowMessage(result.message))
                    }
                }
            }
        }
    }

    private fun sendCallMessage() {
        val text = _uiState.value.draftMessage.trim()
        if (text.isEmpty()) return
        val roomId = _uiState.value.callRoomId
        val peerUid = _uiState.value.peerUserId.toLongOrNull()?.takeIf { it > 0L }
        if (roomId.isBlank() || peerUid == null || _uiState.value.isSendingMessage) return
        val localId = "local_${System.currentTimeMillis()}"
        val item = CallChatItem.Message(
            id = localId,
            isLocal = true,
            senderName = str(R.string.call_incall_you),
            text = text,
        )
        _uiState.update {
            it.copy(
                draftMessage = "",
                isSendingMessage = true,
                chatItems = it.chatItems + item,
            )
        }
        viewModelScope.launch {
            when (
                val result = runtime.callSessionRepository.sendCallMessage(
                    roomId = roomId,
                    toUid = peerUid,
                    content = text,
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSendingMessage = false) }
                }
                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            isSendingMessage = false,
                            chatItems = state.chatItems.filterNot { it.id == localId },
                        )
                    }
                    if (result.isInsufficientBalance()) {
                        maybeShowCoinPayGuide(result)
                    } else {
                        _effects.send(CallEffect.ShowMessage(result.message))
                    }
                }
            }
        }
    }

    private fun setCameraEnabled(enabled: Boolean) {
        CallKitHolder.rtc?.setCameraMuted(!enabled)
        _uiState.update { it.copy(cameraEnabled = enabled) }
        if (enabled) {
            CallKitHolder.rtc?.refreshLocalPreview()
        }
        val roomId = _uiState.value.callRoomId
        if (roomId.isBlank()) return
        viewModelScope.launch {
            when (
                val result = runtime.callSessionRepository.updateMaskStatus(
                    roomId = roomId,
                    cameraOn = enabled,
                )
            ) {
                is AppResult.Failure ->
                    AppLogger.d(TAG, "mask-status fail roomId=$roomId msg=${result.message}")
                else -> Unit
            }
        }
    }

    private fun onSignalingUxEvent(event: SignalingEvent) {
        when (event) {
            is SignalingEvent.BalanceAlert -> {
                if (!matchesActiveRoom(event.roomKey, event.roomSessionId)) return
                if (event.balance >= 0) {
                    runtime.accountBalanceStore.update(event.balance)
                }
                viewModelScope.launch { showBalanceAlertGuide(event) }
            }
            is SignalingEvent.InCallChat -> {
                if (!matchesActiveRoom(event.roomKey)) return
                appendInCallChat(event)
            }
            is SignalingEvent.PeerMaskStatus -> {
                if (!matchesActiveRoom(event.roomKey)) return
                _uiState.update { it.copy(peerMasked = event.masked) }
            }
            else -> Unit
        }
    }

    private fun matchesActiveRoom(roomKey: String, sessionId: Long = 0L): Boolean {
        val active = _uiState.value.callRoomId
        if (active.isBlank()) return false
        if (matchesCallRoomId(active, roomSessionId, roomKey)) return true
        if (sessionId > 0L && matchesCallRoomId(active, roomSessionId, sessionId.toString())) {
            return true
        }
        return false
    }

    private fun appendInCallChat(event: SignalingEvent.InCallChat) {
        if (event.content.isBlank()) return
        when (event.msgType) {
            IN_CALL_MSG_TYPE_TIP -> {
                val tip = CallChatItem.BillingTip(
                    id = event.messageId.ifBlank { "tip_${System.currentTimeMillis()}" },
                    text = event.content,
                )
                _uiState.update { it.copy(chatItems = it.chatItems + tip) }
            }
            else -> {
                val sender = if (event.sendUid > 0L &&
                    event.sendUid.toString() == _uiState.value.peerUserId
                ) {
                    _uiState.value.peerNickname.ifBlank { str(R.string.call_ringing_unknown_peer) }
                } else {
                    _uiState.value.peerNickname.ifBlank { str(R.string.call_ringing_unknown_peer) }
                }
                val message = CallChatItem.Message(
                    id = event.messageId.ifBlank { "remote_${System.currentTimeMillis()}" },
                    isLocal = false,
                    senderName = sender,
                    text = event.content,
                )
                _uiState.update { state ->
                    if (state.chatItems.any { it.id == message.id }) state
                    else state.copy(chatItems = state.chatItems + message)
                }
            }
        }
    }

    private fun dismissCoinPayGuide() {
        _uiState.update { it.copy(coinPayGuide = null) }
        if (!exitAfterCoinPayGuideDismiss) return
        exitAfterCoinPayGuideDismiss = false
        viewModelScope.launch { _effects.send(CallEffect.Exit) }
    }

    private fun maybeShowCoinPayGuide(failure: AppResult.Failure) {
        if (!failure.isInsufficientBalance()) return
        val biz = failure as? AppResult.BizError
        presentCoinPayGuide(
            fromCallback = biz?.callback.toRechargePageDataOrNull(),
            fromType = biz?.fromType,
        )
    }

    private fun showCoinPayGuideForCreate(failure: CallCreateResult.Failure) {
        exitAfterCoinPayGuideDismiss = true
        presentCoinPayGuide(
            fromCallback = failure.rechargePageData,
            fromType = GiftSendRequestDto.FROM_TYPE_CALL,
        )
    }

    private fun presentCoinPayGuide(
        fromCallback: RechargePageData?,
        fromType: Int? = GiftSendRequestDto.FROM_TYPE_CALL,
    ) {
        val balance = _uiState.value.coinBalance
        _uiState.update {
            it.copy(
                coinPayGuide = CoinPayGuideUiState(
                    isLoading = true,
                    balance = balance,
                ),
            )
        }
        viewModelScope.launch {
            yield()
            val page = resolveCoinPayGuidePage(fromCallback)
            val fallbackLabel =
                getApplication<Application>().getString(StoreR.string.store_super_discount)
            val ui = page?.toCoinPayGuideUiState(
                fallbackSuperDiscountLabel = fallbackLabel,
                fromType = fromType,
            )
            if (ui == null || ui.isCatalogEmpty) {
                _uiState.update { state ->
                    if (state.coinPayGuide?.isLoading == true) {
                        state.copy(coinPayGuide = null)
                    } else {
                        state
                    }
                }
                if (exitAfterCoinPayGuideDismiss) {
                    exitAfterCoinPayGuideDismiss = false
                    _effects.send(CallEffect.OpenStore)
                    _effects.send(CallEffect.Exit)
                } else {
                    _effects.send(CallEffect.OpenStore)
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    coinPayGuide = ui.copy(
                        isLoading = false,
                        balance = if (ui.balance > 0) ui.balance else balance,
                    ),
                )
            }
        }
    }

    private suspend fun resolveCoinPayGuidePage(
        fromCallback: RechargePageData?,
    ): RechargePageData? {
        if (fromCallback != null && !fromCallback.isEmpty) {
            return when (val full = runtime.coinRepository.getRechargePage()) {
                is AppResult.Success -> fromCallback.withCarouselFrom(full.data)
                is AppResult.Failure -> fromCallback
            }
        }
        return when (val full = runtime.coinRepository.getRechargePage()) {
            is AppResult.Success -> full.data.takeUnless { it.isEmpty }
            is AppResult.Failure -> null
        }
    }

    private suspend fun showBalanceAlertGuide(event: SignalingEvent.BalanceAlert) {
        val fallback = getApplication<Application>().getString(StoreR.string.store_super_discount)
        var page = RechargePageData(
            balance = event.balance,
            hotProducts = listOfNotNull(event.payItem?.toRechargeProduct()),
            products = emptyList(),
            saleItems = listOfNotNull(event.salePayItem?.toRechargeSaleItem()),
        )
        if (page.isEmpty) {
            when (val full = runtime.coinRepository.getRechargePage()) {
                is AppResult.Success -> page = page.withCarouselFrom(full.data)
                is AppResult.Failure -> Unit
            }
        }
        val guide = page.toCoinPayGuideUiState(
            fallbackSuperDiscountLabel = fallback,
            fromType = GiftSendRequestDto.FROM_TYPE_CALL,
        )
        if (guide.isCatalogEmpty) {
            _effects.send(CallEffect.OpenStore)
            return
        }
        _uiState.update { it.copy(coinPayGuide = guide) }
    }

    private fun purchaseCoinPayGuide(offerId: Long, isSale: Boolean) {
        val guide = _uiState.value.coinPayGuide ?: return
        if (guide.purchasingOfferId != null) return
        val sale = guide.saleOffers.firstOrNull { it.id == offerId }
        val coin = guide.coinOffers.firstOrNull { it.id == offerId }
        val sku: String
        val goodsId: Long
        when {
            isSale && sale != null -> {
                sku = sale.sku
                goodsId = sale.id
            }
            !isSale && coin != null -> {
                sku = coin.sku
                goodsId = coin.id
            }
            else -> return
        }
        val activity = hostActivity
        if (activity == null) {
            viewModelScope.launch {
                _effects.send(
                    CallEffect.ShowMessage(
                        getApplication<Application>().getString(StoreR.string.store_status_no_activity),
                    ),
                )
            }
            return
        }
        val launcher = StorePurchaseLauncherHolder.launcher
        if (launcher == null) {
            viewModelScope.launch {
                _effects.send(
                    CallEffect.ShowMessage(
                        getApplication<Application>().getString(StoreR.string.store_status_launcher_missing),
                    ),
                )
            }
            return
        }
        val request = StorePurchaseRequest(
            uiId = "call-coin-guide-$goodsId",
            goodsId = goodsId,
            productId = sku,
            productType = BillingProductType.Coins,
            paymentType = BillingPaymentType.GooglePlay,
            fromType = guide.fromType ?: GiftSendRequestDto.FROM_TYPE_CALL,
        )
        viewModelScope.launch {
            _uiState.update {
                it.copy(coinPayGuide = it.coinPayGuide?.copy(purchasingOfferId = offerId))
            }
            launcher.launch(activity, request) { result ->
                when (result) {
                    is StorePurchaseResult.Success -> {
                        _uiState.update { it.copy(coinPayGuide = null) }
                        viewModelScope.launch {
                            _effects.send(
                                CallEffect.ShowMessage(
                                    getApplication<Application>()
                                        .getString(StoreR.string.store_status_purchase_verified),
                                ),
                            )
                        }
                    }
                    is StorePurchaseResult.Canceled -> {
                        _uiState.update {
                            it.copy(coinPayGuide = it.coinPayGuide?.copy(purchasingOfferId = null))
                        }
                    }
                    is StorePurchaseResult.Failed -> {
                        _uiState.update {
                            it.copy(coinPayGuide = it.coinPayGuide?.copy(purchasingOfferId = null))
                        }
                        viewModelScope.launch {
                            _effects.send(CallEffect.ShowMessage(result.message))
                        }
                    }
                }
            }
        }
    }

    private fun openReportSheet() {
        val peerId = _uiState.value.peerUserId.toLongOrNull()?.takeIf { it > 0L } ?: return
        _uiState.update {
            it.copy(
                isReportSheetVisible = true,
                report = CallReportUiState(isLoading = true),
            )
        }
        viewModelScope.launch {
            when (val result = runtime.reportRepository.loadReportInit(peerId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            report = CallReportUiState(
                                isLoading = false,
                                reasons = result.data.reasons,
                            ),
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isReportSheetVisible = false, report = null) }
                    _effects.send(CallEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun toggleReportReason(reasonId: Int) {
        _uiState.update { state ->
            val report = state.report ?: return@update state
            val next = report.selectedReasonIds.toMutableSet()
            if (!next.add(reasonId)) next.remove(reasonId)
            state.copy(report = report.copy(selectedReasonIds = next))
        }
    }

    private fun submitReport() {
        val state = _uiState.value
        val peerId = state.peerUserId.toLongOrNull()?.takeIf { it > 0L } ?: return
        val report = state.report ?: return
        if (report.isSubmitting || report.selectedReasonIds.isEmpty()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(report = it.report?.copy(isSubmitting = true))
            }
            when (
                val result = runtime.reportRepository.submitReport(
                    targetUid = peerId,
                    content = "",
                    reasonIds = report.selectedReasonIds.toList(),
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isReportSheetVisible = false, report = null) }
                    _effects.send(CallEffect.ShowMessage(str(R.string.call_report_success)))
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(report = it.report?.copy(isSubmitting = false))
                    }
                    _effects.send(CallEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun SignalingCoinOffer.toRechargeProduct(): RechargeProduct? {
        if (id <= 0L && sku.isBlank()) return null
        return RechargeProduct(
            id = id,
            sku = sku,
            coinAmount = diamond,
            price = moneyDesc,
            originalPrice = originalDesc.takeIf { it.isNotBlank() },
            saleLabel = saleDesc.takeIf { it.isNotBlank() },
            labelType = 0,
            iconUrl = null,
            coinIndex = 0,
        )
    }

    private fun SignalingCoinOffer.toRechargeSaleItem(): RechargeSaleItem? {
        if (id <= 0L && sku.isBlank()) return null
        return RechargeSaleItem(
            id = id,
            sku = sku,
            baseCoins = diamond,
            bonusCoins = giveCoins,
            totalCoins = diamond + giveCoins,
            price = moneyDesc,
            originalPrice = originalDesc.takeIf { it.isNotBlank() },
            discountRate = saleDesc.takeIf { it.isNotBlank() },
            styleIndex = 0,
            iconUrl = null,
            coinIndex = 0,
            showBonusAsMatch = false,
            matchCount = 0,
            superDiscountLabel = null,
        )
    }

    private fun toggleTranslation(messageId: String) {
        _uiState.update { state ->
            state.copy(
                chatItems = state.chatItems.map { item ->
                    if (item is CallChatItem.Message && item.id == messageId && item.isLocal) {
                        val translation = item.translation
                            ?: str(R.string.call_incall_translation_placeholder)
                        item.copy(
                            translation = translation,
                            showTranslation = !item.showTranslation,
                        )
                    } else {
                        item
                    }
                },
            )
        }
    }

    private fun hangup() {
        CallKitHolder.coordinator?.hangup()
            ?: viewModelScope.launch { _effects.send(CallEffect.Exit) }
    }

    private fun answer() {
        val coordinator = CallKitHolder.coordinator ?: return
        val ringing = coordinator.state.value as? CallState.IncomingRinging ?: return
        val app = getApplication<Application>()
        if (!RtcCallPermissions.mediaGranted(app)) {
            viewModelScope.launch {
                _effects.send(CallEffect.ShowMessage(str(R.string.call_permission_required)))
            }
            return
        }
        viewModelScope.launch {
            val roomId = ringing.inviteId
            fencingToken = ringing.fencingToken ?: fencingToken
            // Fire-and-forget /call/accept — does not block join.
            launch {
                when (val accept = runtime.callSessionRepository.acceptCall(roomId)) {
                    is AppResult.Failure ->
                        AppLogger.d(TAG, "acceptCall fail roomId=$roomId msg=${accept.message}")
                    else -> Unit
                }
            }
            when (
                val success = runtime.callSessionRepository.reportCallSuccess(
                    roomId = roomId,
                    fencingToken = fencingToken,
                )
            ) {
                is AppResult.Success -> {
                    val refreshed = success.data
                    if (!refreshed?.fencingToken.isNullOrBlank()) {
                        fencingToken = refreshed.fencingToken
                    }
                    val channel = refreshed?.channel?.takeIf { it.isNotBlank() }
                        ?: ringing.channelId
                    val token = refreshed?.token?.takeIf { it.isNotBlank() }
                        ?: ringing.rtcToken
                    val uid = refreshed?.uid?.takeIf { it > 0 } ?: ringing.rtcUid
                    val appId = refreshed?.appId?.takeIf { it.isNotBlank() }
                        ?: ringing.rtcAppId
                    coordinator.acceptIncoming(
                        inviteId = roomId,
                        channelId = channel,
                        uid = uid,
                        token = token,
                        rtcAppId = appId,
                        roomSessionId = ringing.roomSessionId,
                        fencingToken = fencingToken,
                    )
                    _uiState.update { it.copy(rtcSurfacesActive = true) }
                }
                is AppResult.Failure -> {
                    if (success.isInsufficientBalance()) {
                        // Stay on incoming ringing so the user can recharge and retry answer.
                        maybeShowCoinPayGuide(success)
                    } else {
                        _effects.send(CallEffect.ShowMessage(success.message))
                        _effects.send(CallEffect.Exit)
                    }
                }
            }
        }
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private companion object {
        const val TAG = "CallViewModel"
        const val HEARTBEAT_INTERVAL_MS = 30_000L
        const val ANSWER_STATUS_FIRST_DELAY_MS = 3_000L
        const val ANSWER_STATUS_INTERVAL_MS = 3_000L
        const val ANSWER_STATUS_REQUEST_TIMEOUT_MS = 5_000L
        const val DEFAULT_ANSWER_TIMEOUT_MS = 60_000L
        const val DEFAULT_RENEW_AHEAD_SEC = 300
        const val IN_CALL_MSG_TYPE_TIP = 4
    }
}

private fun CallCreateResult.Failure.isInsufficientBalance(): Boolean =
    rechargePageData != null ||
        code == INSUFFICIENT_BALANCE_ERROR_CODE ||
        code == MSG_SEND_INSUFFICIENT_BALANCE_ERROR_CODE
