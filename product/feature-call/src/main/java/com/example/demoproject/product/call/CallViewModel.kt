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
import com.example.demoproject.platform.data.repository.MatchStartResult
import com.example.demoproject.platform.data.repository.RechargePageData
import com.example.demoproject.platform.data.repository.StorePurchaseRequest
import com.example.demoproject.platform.data.repository.StorePurchaseResult
import com.example.demoproject.platform.data.repository.resolveCallBalanceAlertRemainingSeconds
import com.example.demoproject.platform.data.repository.resolveMatchBalanceAlertRemainingSeconds
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.INSUFFICIENT_BALANCE_ERROR_CODE
import com.example.demoproject.platform.network.result.MSG_SEND_INSUFFICIENT_BALANCE_ERROR_CODE
import com.example.demoproject.platform.network.result.isInsufficientBalance
import com.example.demoproject.platform.rtc.api.RtcCallPermissions
import com.example.demoproject.platform.rtc.api.RtcConnectionState
import com.example.demoproject.platform.rtc.api.RtcEvent
import com.example.demoproject.product.store.CallBalanceOfferGuideUiState
import com.example.demoproject.product.store.CallHangupContinueUiState
import com.example.demoproject.product.store.CallHangupRechargeUiState
import com.example.demoproject.product.store.CoinPayGuideUiState
import com.example.demoproject.product.store.R as StoreR
import com.example.demoproject.product.store.toCallHangupRechargeUiState
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
    initialIsMatchCall: Boolean = false,
    initialNickname: String = "",
    initialAvatarUrl: String = "",
    initialAge: Int = 0,
    initialVideoUrl: String = "",
    initialCoverUrl: String = "",
    initialMatchEntryId: String = "",
) : AndroidViewModel(application) {
    private val runtime = NetworkRuntime.get(application)
    private val matchEntry = runtime.matchSessionCoordinator.consumeCallEntry(initialMatchEntryId)
    private val _uiState = MutableStateFlow(
        CallUiState(
            isMatchCall = initialIsMatchCall,
            phase = if (matchEntry != null) CallRingingPhase.Connecting else CallRingingPhase.Preparing,
            peerUserId = matchEntry?.peer?.userId ?: targetUserId,
            peerNickname = matchEntry?.peer?.nickname ?: initialNickname,
            peerAge = matchEntry?.peer?.age ?: initialAge,
            peerAvatarUrl = (matchEntry?.peer?.avatarUrl ?: initialAvatarUrl)
                .toPicUrlOrNull().orEmpty()
                .ifBlank { matchEntry?.peer?.avatarUrl ?: initialAvatarUrl },
            videoUrl = initialVideoUrl.toChatBinaryUrlOrNull().orEmpty()
                .ifBlank { initialVideoUrl },
            coverUrl = initialCoverUrl.toPicUrlOrNull().orEmpty()
                .ifBlank { initialCoverUrl },
            callRoomId = matchEntry?.room?.roomId.orEmpty(),
            matchSessionId = matchEntry?.matchSessionId ?: 0L,
            matchId = matchEntry?.matchId,
            matchTimeSeconds = matchEntry?.room?.matchTimeSeconds ?: 0,
            nextTimeSeconds = matchEntry?.room?.nextTimeSeconds ?: 0,
            isMatchReceiveOnly = matchEntry?.room?.isReceiveOnly == true,
            isFreeCall = false,
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
    private var matchNextJob: Job? = null
    private var matchNextTransitionActive: Boolean = false
    private var matchSessionReleased: Boolean = false
    private var inCallBootstrapped: Boolean = false
    private var stopTokenRenewal: Boolean = false
    /** When true, dismissing [CallUiState.coinPayGuide] also exits the call screen. */
    private var exitAfterCoinPayGuideDismiss: Boolean = false

    init {
        viewModelScope.launch {
            runtime.accountBalanceStore.coins.collect { coins ->
                _uiState.update {
                    it.copy(
                        coinBalance = coins,
                        coinPayGuide = it.coinPayGuide?.copy(balance = coins),
                        balanceOfferGuide = it.balanceOfferGuide?.copy(balance = coins),
                    )
                }
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
            if (initialIsMatchCall) {
                startDirectMatchCall()
            } else {
                maybeStartOutgoing()
            }
        }
    }

    fun bindActivity(activity: Activity?) {
        hostActivity = activity
    }

    fun onIntent(intent: CallIntent) {
        when (intent) {
            CallIntent.Hangup -> hangup()
            CallIntent.NextMatch -> nextMatch()
            CallIntent.Answer -> answer()
            CallIntent.Report -> openReportPage()
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
            CallIntent.DismissHangupRecharge -> dismissHangupRecharge()
            is CallIntent.PurchaseHangupRechargeCoin ->
                purchaseHangupRecharge(intent.offerId, isSale = false)
            is CallIntent.PurchaseHangupRechargeSale ->
                purchaseHangupRecharge(intent.offerId, isSale = true)
            CallIntent.DismissHangupContinue -> dismissHangupContinue()
            CallIntent.HangupContinueVideo -> continueHangupVideoCall()
            CallIntent.HangupContinueChat -> openHangupChat()
            CallIntent.OpenBalanceOfferGuide -> openBalanceOfferGuide(fromUser = true)
            CallIntent.DismissBalanceOfferGuide -> dismissBalanceOfferGuide()
            CallIntent.BalanceOfferContinue -> purchasePrimaryBalanceOffer()
            CallIntent.BalanceOfferMoreOptions -> openBalanceOfferMoreOptions()
            is CallIntent.PurchaseBalanceOfferVip ->
                purchaseBalanceOffer(intent.offerId, isVip = true)
            is CallIntent.PurchaseBalanceOfferSale ->
                purchaseBalanceOffer(intent.offerId, isVip = false)
        }
    }

    private fun selfRtcUid(): Int =
        runtime.sessionManager.currentUserId?.toIntOrNull()?.takeIf { it > 0 } ?: 0

    private suspend fun startDirectMatchCall() {
        val entry = matchEntry
        val room = entry?.room
        if (entry == null || room == null || !room.canJoinDirectly) {
            val message = str(R.string.call_status_no_peer)
            runtime.matchSessionCoordinator.publishContinuation(
                com.example.demoproject.platform.data.match.MatchContinuation.Failure(message),
            )
            _effects.send(CallEffect.OpenMatch)
            return
        }
        val coordinator = CallKitHolder.coordinator
        if (coordinator == null) {
            val message = str(R.string.call_status_coordinator_missing)
            runtime.matchSessionCoordinator.publishContinuation(
                com.example.demoproject.platform.data.match.MatchContinuation.Failure(message),
            )
            _effects.send(CallEffect.OpenMatch)
            return
        }
        // Match tokens are signed for our own uid, and `room_info` usually omits it. Joining with
        // uid=0 makes Agora reject the token (onError 110 / invalid-token disconnect).
        val localRtcUid = room.localRtcUid.takeIf { it > 0 } ?: selfRtcUid()
        if (localRtcUid <= 0) {
            val message = str(R.string.call_status_no_peer)
            runtime.matchSessionCoordinator.publishContinuation(
                com.example.demoproject.platform.data.match.MatchContinuation.Failure(message),
            )
            _effects.send(CallEffect.OpenMatch)
            return
        }
        isOutgoingCaller = true
        fencingToken = room.fencingToken
        roomSessionId = room.roomSessionId.takeIf { it > 0L }
            ?: room.roomId.toLongOrNull()?.takeIf { it > 0L }
            ?: 0L
        coordinator.resetIfTerminal()
        coordinator.startDirectMatchCall(
            callId = room.roomId,
            channelId = room.channelId,
            uid = localRtcUid,
            token = room.rtcToken,
            rtcAppId = room.rtcAppId,
            roomSessionId = roomSessionId,
            fencingToken = room.fencingToken,
            receiveOnly = room.isReceiveOnly,
        )
        _uiState.update {
            it.copy(
                phase = CallRingingPhase.Connecting,
                rtcSurfacesActive = true,
            )
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
                        isFreeCall = room.isFreeCall,
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
            is RtcEvent.ConnectionStateChanged -> {
                _uiState.update {
                    if (!it.isMatchCall || it.phase != CallRingingPhase.Connecting) {
                        it
                    } else {
                        it.copy(
                            connectingNotice = if (event.state == RtcConnectionState.Reconnecting) {
                                CallConnectingNotice.PoorNetwork
                            } else {
                                null
                            },
                        )
                    }
                }
            }
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
                        connectingNotice = null,
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
                        isFreeCall = state.callFreeMin > 0,
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
                        phase = if (it.isMatchCall && it.remoteRtcUid <= 0) {
                            CallRingingPhase.Connecting
                        } else {
                            phaseWhileWaitingForRemote(it)
                        },
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
                        phase = if (it.isMatchCall && it.remoteRtcUid <= 0) {
                            CallRingingPhase.Connecting
                        } else {
                            phaseWhileWaitingForRemote(it)
                        },
                        callRoomId = state.callId.ifBlank { it.callRoomId },
                        rtcSurfacesActive = true,
                    )
                }
                if (_uiState.value.remoteRtcUid > 0) {
                    bootstrapInCall()
                }
            }
            is CallState.Ended -> {
                // Capture before stopInCallJobs clears the in-call bootstrap flag.
                val wasConnected = inCallBootstrapped
                val wasMatchConnecting =
                    _uiState.value.isMatchCall &&
                        _uiState.value.phase == CallRingingPhase.Connecting &&
                        !wasConnected
                if (matchNextTransitionActive) {
                    stopInCallJobs()
                    _uiState.update {
                        it.copy(rtcSurfacesActive = false, remoteRtcUid = 0)
                    }
                    return
                }
                if (_uiState.value.isMatchCall &&
                    state.reason == EndReason.RemoteHangup &&
                    !matchSessionReleased
                ) {
                    stopInCallJobs()
                    _uiState.update {
                        it.copy(
                            phase = CallRingingPhase.Connecting,
                            connectingNotice = if (wasMatchConnecting) {
                                CallConnectingNotice.PeerLeft
                            } else {
                                null
                            },
                            rtcSurfacesActive = false,
                            remoteRtcUid = 0,
                        )
                    }
                    matchNextTransitionActive = true
                    viewModelScope.launch {
                        if (wasMatchConnecting) delay(REMATCH_DELAY_MS)
                        performMatchNext(endCurrentCall = false, callConnected = wasConnected)
                    }
                    return
                }
                val wasConnectedFreeCall = _uiState.value.isFreeCall && wasConnected
                val showHangupRecharge =
                    state.reason == EndReason.InsufficientBalance && wasConnected
                stopInCallJobs()
                // Hangup / server end must leave the call page — except insufficient-balance
                // after a connected call, which opens the hangup recharge sheet first.
                exitAfterCoinPayGuideDismiss = false
                _uiState.update {
                    it.copy(
                        phase = CallRingingPhase.Ended,
                        rtcSurfacesActive = false,
                        remoteRtcUid = 0,
                        showBalanceFloat = false,
                        isBalanceOfferGuideVisible = false,
                        balanceOfferGuide = null,
                        coinPayGuide = null,
                        hangupRecharge = null,
                        hangupContinue = null,
                        isMoreSheetVisible = false,
                        isGiftSheetVisible = false,
                        isReportSheetVisible = false,
                        showGiftQuickBar = false,
                    )
                }
                refreshCallFreeMinAfterCall(wasConnectedFreeCall = wasConnectedFreeCall)
                if (_uiState.value.isMatchCall) {
                    viewModelScope.launch {
                        endAndCloseMatchSession()
                        if (showHangupRecharge) {
                            presentHangupRecharge()
                        } else {
                            _effects.send(CallEffect.Exit)
                        }
                    }
                } else if (showHangupRecharge) {
                    presentHangupRecharge()
                } else {
                    viewModelScope.launch { _effects.send(CallEffect.Exit) }
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
                _uiState.update {
                    it.copy(
                        callDurationSec = it.callDurationSec + 1,
                    )
                }
                refreshBalanceOfferGates()
            }
        }
        // Re-evaluate immediately in case a_type=7 arrived before InCall / ticker start.
        refreshBalanceOfferGates()
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

    /**
     * Re-fetch `call_free_min` after hangup so Online free badges drop once the free quota
     * is consumed. Optimistic local decrement covers slow `/app/open`.
     */
    private fun refreshCallFreeMinAfterCall(wasConnectedFreeCall: Boolean) {
        if (wasConnectedFreeCall) {
            val current = runtime.callFreeMinStore.callFreeMin.value
            runtime.callFreeMinStore.update((current - 1).coerceAtLeast(0))
        }
        viewModelScope.launch {
            when (
                val open = runtime.appSessionRepository.openApp(hasProxy = false, hasVpn = false)
            ) {
                is AppResult.Success -> {
                    val data = open.data ?: return@launch
                    runtime.callFreeMinStore.update(data.callFreeMin)
                    runtime.matchQuotaStore.update(matchFreeCount = data.matchFreeCount)
                    runtime.accountBalanceStore.update(data.accountMoney)
                }
                is AppResult.Failure ->
                    AppLogger.d(TAG, "refresh call_free_min after call failed: ${open.message}")
            }
        }
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
                // Wallet is account-wide — refresh even if this alert is not for the active room UX.
                if (event.balance >= 0) {
                    runtime.accountBalanceStore.update(event.balance)
                }
                if (!matchesActiveRoom(event.roomKey, event.roomSessionId)) return
                applyBalanceAlert(event)
            }
            is SignalingEvent.BalanceSync -> {
                if (event.balance >= 0) {
                    runtime.accountBalanceStore.update(event.balance)
                }
            }
            is SignalingEvent.InCallChat -> {
                if (!matchesActiveRoom(event.roomKey)) return
                appendInCallChat(event)
            }
            is SignalingEvent.PeerMaskStatus -> {
                if (!matchesActiveRoom(event.roomKey)) return
                _uiState.update { it.copy(peerMasked = event.masked) }
            }
            is SignalingEvent.Error -> {
                _uiState.update {
                    if (it.isMatchCall && it.phase == CallRingingPhase.Connecting) {
                        it.copy(connectingNotice = CallConnectingNotice.PoorNetwork)
                    } else {
                        it
                    }
                }
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

    private fun presentHangupRecharge() {
        val snapshot = _uiState.value
        _uiState.update {
            it.copy(
                hangupRecharge = CallHangupRechargeUiState(
                    isLoading = true,
                    peerNickname = snapshot.peerNickname,
                    peerAge = snapshot.peerAge,
                    peerAvatarUrl = snapshot.peerAvatarUrl,
                ),
                hangupContinue = null,
            )
        }
        viewModelScope.launch {
            yield()
            val page = resolveCoinPayGuidePage(fromCallback = null)
            val fallbackLabel =
                getApplication<Application>().getString(StoreR.string.store_super_discount)
            val ui = page?.toCallHangupRechargeUiState(
                peerNickname = snapshot.peerNickname,
                peerAge = snapshot.peerAge,
                peerAvatarUrl = snapshot.peerAvatarUrl,
                fallbackSuperDiscountLabel = fallbackLabel,
                fromType = GiftSendRequestDto.FROM_TYPE_CALL,
            )
            if (ui == null || ui.isCatalogEmpty) {
                _uiState.update { it.copy(hangupRecharge = null) }
                _effects.send(CallEffect.OpenStore)
                _effects.send(CallEffect.Exit)
                return@launch
            }
            _uiState.update { it.copy(hangupRecharge = ui.copy(isLoading = false)) }
        }
    }

    private fun dismissHangupRecharge() {
        _uiState.update { it.copy(hangupRecharge = null) }
        viewModelScope.launch { _effects.send(CallEffect.Exit) }
    }

    private fun purchaseHangupRecharge(offerId: Long, isSale: Boolean) {
        val guide = _uiState.value.hangupRecharge ?: return
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
                        getApplication<Application>()
                            .getString(StoreR.string.store_status_launcher_missing),
                    ),
                )
            }
            return
        }
        val request = StorePurchaseRequest(
            uiId = "call-hangup-recharge-$goodsId",
            goodsId = goodsId,
            productId = sku,
            productType = BillingProductType.Coins,
            paymentType = BillingPaymentType.GooglePlay,
            fromType = guide.fromType ?: GiftSendRequestDto.FROM_TYPE_CALL,
        )
        viewModelScope.launch {
            _uiState.update {
                it.copy(hangupRecharge = it.hangupRecharge?.copy(purchasingOfferId = offerId))
            }
            launcher.launch(activity, request) { result ->
                when (result) {
                    is StorePurchaseResult.Success -> {
                        val peer = _uiState.value
                        _uiState.update {
                            it.copy(
                                hangupRecharge = null,
                                hangupContinue = CallHangupContinueUiState(
                                    peerNickname = peer.peerNickname,
                                    peerAge = peer.peerAge,
                                    peerAvatarUrl = peer.peerAvatarUrl,
                                    peerUserId = peer.peerUserId,
                                    videoUrl = peer.videoUrl,
                                    coverUrl = peer.coverUrl,
                                ),
                            )
                        }
                    }
                    is StorePurchaseResult.Canceled -> {
                        _uiState.update {
                            it.copy(
                                hangupRecharge = it.hangupRecharge?.copy(purchasingOfferId = null),
                            )
                        }
                    }
                    is StorePurchaseResult.ExternalCheckoutOpened -> {
                        _uiState.update {
                            it.copy(
                                hangupRecharge = it.hangupRecharge?.copy(purchasingOfferId = null),
                            )
                        }
                        viewModelScope.launch {
                            _effects.send(
                                CallEffect.ShowMessage(
                                    getApplication<Application>()
                                        .getString(StoreR.string.store_status_external_checkout_opened),
                                ),
                            )
                        }
                    }
                    is StorePurchaseResult.Failed -> {
                        _uiState.update {
                            it.copy(
                                hangupRecharge = it.hangupRecharge?.copy(purchasingOfferId = null),
                            )
                        }
                        viewModelScope.launch {
                            _effects.send(CallEffect.ShowMessage(result.message))
                        }
                    }
                }
            }
        }
    }

    private fun dismissHangupContinue() {
        _uiState.update { it.copy(hangupContinue = null) }
        viewModelScope.launch { _effects.send(CallEffect.Exit) }
    }

    private fun continueHangupVideoCall() {
        val continueState = _uiState.value.hangupContinue ?: return
        val userId = continueState.peerUserId.ifBlank { _uiState.value.peerUserId }
        if (userId.isBlank()) {
            dismissHangupContinue()
            return
        }
        _uiState.update { it.copy(hangupContinue = null) }
        viewModelScope.launch {
            _effects.send(
                CallEffect.RestartVideoCall(
                    userId = userId,
                    nickname = continueState.peerNickname,
                    age = continueState.peerAge,
                    avatarUrl = continueState.peerAvatarUrl,
                    videoUrl = continueState.videoUrl,
                    coverUrl = continueState.coverUrl,
                ),
            )
        }
    }

    private fun openHangupChat() {
        val continueState = _uiState.value.hangupContinue ?: return
        val conversationId = continueState.peerUserId.ifBlank { _uiState.value.peerUserId }
        if (conversationId.isBlank()) {
            dismissHangupContinue()
            return
        }
        val nickname = continueState.peerNickname
        _uiState.update { it.copy(hangupContinue = null) }
        viewModelScope.launch {
            _effects.send(
                CallEffect.OpenChatDetail(
                    conversationId = conversationId,
                    nickname = nickname,
                ),
            )
        }
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

    private fun applyBalanceAlert(event: SignalingEvent.BalanceAlert) {
        val fallback = getApplication<Application>().getString(StoreR.string.store_super_discount)
        val state = _uiState.value
        val liveRemaining = if (state.isMatchCall) {
            resolveMatchBalanceAlertRemainingSeconds(
                totalDurationSeconds = event.totalDurationSeconds,
                elapsedSeconds = state.callDurationSec,
            )
        } else {
            resolveCallBalanceAlertRemainingSeconds(
                isFreeCall = state.isFreeCall,
                rawDurationSeconds = event.remainingSeconds,
                totalDurationSeconds = event.totalDurationSeconds,
                elapsedSeconds = state.callDurationSec,
            )
        }
        val offer = event.toCallBalanceOffer(
            liveRemainingSeconds = liveRemaining,
            isFreeCall = _uiState.value.isFreeCall,
            fallbackSaleBadge = fallback,
        )
        _uiState.update {
            it.copy(
                balanceAlertRoomKey = event.roomKey,
                balanceAlertRawDuration = event.remainingSeconds,
                balanceAlertTotalDuration = event.totalDurationSeconds,
                balanceAlertSaleThreshold = event.saleRechargeAlertTimeSeconds,
                balanceAlertRechargeThreshold = event.rechargeAlertTimeSeconds,
                balanceOffer = offer,
            )
        }
        refreshBalanceOfferGates()
    }

    private fun refreshBalanceOfferGates() {
        val state = _uiState.value
        if (state.balanceAlertRoomKey.isBlank() || state.balanceOffer == null) return
        if (state.phase != CallRingingPhase.InCall) {
            _uiState.update { it.copy(showBalanceFloat = false) }
            return
        }
        val liveRemaining = if (state.isMatchCall) {
            resolveMatchBalanceAlertRemainingSeconds(
                totalDurationSeconds = state.balanceAlertTotalDuration,
                elapsedSeconds = state.callDurationSec,
            )
        } else {
            resolveCallBalanceAlertRemainingSeconds(
                isFreeCall = state.isFreeCall,
                rawDurationSeconds = state.balanceAlertRawDuration,
                totalDurationSeconds = state.balanceAlertTotalDuration,
                elapsedSeconds = state.callDurationSec,
            )
        }
        val offer = state.balanceOffer.copy(remainingSeconds = liveRemaining)
        val showFloat = if (state.isMatchCall) {
            shouldShowMatchBalanceAlertFloatingWindow(
                remainingSeconds = liveRemaining,
                saleRechargeAlertTimeSeconds = state.balanceAlertSaleThreshold,
            )
        } else {
            shouldShowBalanceAlertFloatingWindow(
                remainingSeconds = liveRemaining,
                elapsedSeconds = state.callDurationSec,
                isFreeCall = state.isFreeCall,
                freeCallTriggerConsumed = state.freeCallTriggerConsumed,
            )
        }
        val shouldAuto = if (state.isMatchCall &&
            state.balanceAlertRechargeThreshold <= 0
        ) {
            false
        } else {
            shouldAutoShowBalanceAlertOffer(
                remainingSeconds = liveRemaining,
                rechargeAlertTimeSeconds = state.balanceAlertRechargeThreshold,
            )
        }
        val alreadyAuto = state.autoShownBalanceOfferRoomId == state.balanceAlertRoomKey
        _uiState.update {
            it.copy(
                balanceOffer = offer,
                showBalanceFloat = showFloat &&
                    !it.isBalanceOfferGuideVisible &&
                    it.coinPayGuide == null,
                balanceOfferGuide = it.balanceOfferGuide?.copy(
                    remainingSeconds = liveRemaining,
                    balance = it.coinBalance,
                ),
            )
        }
        if (shouldAuto && !alreadyAuto) {
            openBalanceOfferGuide(fromUser = false)
        }
    }

    private fun openBalanceOfferGuide(fromUser: Boolean) {
        val state = _uiState.value
        val offer = state.balanceOffer ?: return
        if (state.coinPayGuide != null) return
        val roomKey = state.balanceAlertRoomKey
        if (!fromUser && state.autoShownBalanceOfferRoomId == roomKey) return
        if (state.isBalanceOfferGuideVisible) {
            if (!fromUser) {
                _uiState.update { it.copy(autoShownBalanceOfferRoomId = roomKey) }
            }
            return
        }
        com.example.demoproject.platform.data.promotion.PromotionRechargeGuideTracker.markOpened()
        val guide = CallBalanceOfferGuideUiState(
            balance = state.coinBalance,
            remainingSeconds = offer.remainingSeconds,
            vipOffers = offer.toGuideVipOffers(),
            saleOffers = offer.toGuideSaleOffers(),
            fromType = GiftSendRequestDto.FROM_TYPE_CALL,
        )
        if (guide.isCatalogEmpty) return
        _uiState.update {
            it.copy(
                isBalanceOfferGuideVisible = true,
                balanceOfferGuide = guide,
                showBalanceFloat = false,
                autoShownBalanceOfferRoomId = if (fromUser) {
                    it.autoShownBalanceOfferRoomId
                } else {
                    roomKey
                },
                freeCallTriggerConsumed = it.isFreeCall || it.freeCallTriggerConsumed,
            )
        }
    }

    private fun dismissBalanceOfferGuide() {
        if (com.example.demoproject.platform.data.promotion.PromotionRechargeGuideTracker
                .consumeClosedWithoutPurchase()
        ) {
            com.example.demoproject.platform.data.promotion.PromotionTriggerBus.emit(
                com.example.demoproject.platform.data.promotion.PromotionTrigger
                    .RechargeGuideClosedWithoutPurchase,
            )
        }
        _uiState.update {
            it.copy(
                isBalanceOfferGuideVisible = false,
                balanceOfferGuide = null,
            )
        }
        refreshBalanceOfferGates()
    }

    private fun openBalanceOfferMoreOptions() {
        dismissBalanceOfferGuide()
        presentCoinPayGuide(fromCallback = null, fromType = GiftSendRequestDto.FROM_TYPE_CALL)
    }

    private fun purchasePrimaryBalanceOffer() {
        val guide = _uiState.value.balanceOfferGuide ?: return
        val id = guide.primaryOfferId ?: return
        purchaseBalanceOffer(id, isVip = guide.primaryIsVip)
    }

    private fun purchaseBalanceOffer(offerId: Long, isVip: Boolean) {
        val guide = _uiState.value.balanceOfferGuide ?: return
        if (guide.purchasingOfferId != null) return
        val vip = guide.vipOffers.firstOrNull { it.id == offerId }
        val sale = guide.saleOffers.firstOrNull { it.id == offerId }
        val sku: String
        val goodsId: Long
        val productType: BillingProductType
        when {
            isVip && vip != null -> {
                sku = vip.sku
                goodsId = vip.id
                productType = BillingProductType.Vip
            }
            !isVip && sale != null -> {
                sku = sale.sku
                goodsId = sale.id
                productType = BillingProductType.Coins
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
            uiId = "call-balance-offer-$goodsId",
            goodsId = goodsId,
            productId = sku,
            productType = productType,
            paymentType = BillingPaymentType.GooglePlay,
            fromType = guide.fromType ?: GiftSendRequestDto.FROM_TYPE_CALL,
        )
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    balanceOfferGuide = it.balanceOfferGuide?.copy(purchasingOfferId = offerId),
                )
            }
            launcher.launch(activity, request) { result ->
                when (result) {
                    is StorePurchaseResult.Success -> clearBalanceAlert()
                    is StorePurchaseResult.Canceled -> {
                        _uiState.update {
                            it.copy(
                                balanceOfferGuide = it.balanceOfferGuide?.copy(
                                    purchasingOfferId = null,
                                ),
                            )
                        }
                    }
                    is StorePurchaseResult.ExternalCheckoutOpened -> {
                        _uiState.update {
                            it.copy(
                                balanceOfferGuide = it.balanceOfferGuide?.copy(
                                    purchasingOfferId = null,
                                ),
                            )
                        }
                        viewModelScope.launch {
                            _effects.send(
                                CallEffect.ShowMessage(
                                    getApplication<Application>()
                                        .getString(StoreR.string.store_status_external_checkout_opened),
                                ),
                            )
                        }
                    }
                    is StorePurchaseResult.Failed -> {
                        _uiState.update {
                            it.copy(
                                balanceOfferGuide = it.balanceOfferGuide?.copy(
                                    purchasingOfferId = null,
                                ),
                            )
                        }
                        viewModelScope.launch {
                            _effects.send(CallEffect.ShowMessage(result.message))
                        }
                    }
                }
            }
        }
    }

    private fun clearBalanceAlert() {
        _uiState.update {
            it.copy(
                balanceAlertRoomKey = "",
                balanceAlertRawDuration = 0,
                balanceAlertTotalDuration = 0,
                balanceAlertSaleThreshold = 0,
                balanceAlertRechargeThreshold = 0,
                balanceOffer = null,
                showBalanceFloat = false,
                isBalanceOfferGuideVisible = false,
                balanceOfferGuide = null,
                freeCallTriggerConsumed = true,
            )
        }
        viewModelScope.launch {
            _effects.send(
                CallEffect.ShowMessage(
                    getApplication<Application>()
                        .getString(StoreR.string.store_status_purchase_verified),
                ),
            )
        }
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
                    is StorePurchaseResult.ExternalCheckoutOpened -> {
                        _uiState.update {
                            it.copy(coinPayGuide = it.coinPayGuide?.copy(purchasingOfferId = null))
                        }
                        viewModelScope.launch {
                            _effects.send(
                                CallEffect.ShowMessage(
                                    getApplication<Application>()
                                        .getString(StoreR.string.store_status_external_checkout_opened),
                                ),
                            )
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

    private fun openReportPage() {
        val state = _uiState.value
        val peerId = state.peerUserId.trim()
        if (peerId.isEmpty()) return
        viewModelScope.launch {
            _effects.send(CallEffect.OpenReport(userId = peerId, age = state.peerAge))
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
                    content = report.reasons
                        .filter { it.id in report.selectedReasonIds }
                        .joinToString(", ") { it.title },
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
        val state = _uiState.value
        when {
            state.connectingNotice == CallConnectingNotice.PeerLeft -> {
                matchNextJob?.cancel()
                matchNextJob = null
                matchNextTransitionActive = false
                viewModelScope.launch {
                    endAndCloseMatchSession()
                    _effects.send(CallEffect.Exit)
                }
            }
            state.hangupContinue != null -> dismissHangupContinue()
            state.hangupRecharge != null -> dismissHangupRecharge()
            state.isMatchCall &&
                state.phase == CallRingingPhase.InCall &&
                !state.isMatchHangupEnabled -> Unit
            state.isMatchCall -> {
                viewModelScope.launch {
                    val roomId = state.callRoomId
                    if (roomId.isNotBlank()) {
                        runtime.callSessionRepository.endCall(roomId, state.callDurationSec)
                    }
                    CallKitHolder.coordinator?.endBySystem(EndReason.Hangup)
                        ?: run {
                            endAndCloseMatchSession()
                            _effects.send(CallEffect.Exit)
                        }
                }
            }
            else -> CallKitHolder.coordinator?.hangup()
                ?: viewModelScope.launch { _effects.send(CallEffect.Exit) }
        }
    }

    private fun nextMatch() {
        val state = _uiState.value
        if (!state.isMatchNextEnabled || matchNextTransitionActive || matchSessionReleased) return
        matchNextTransitionActive = true
        _uiState.update { it.copy(isMatchNextInProgress = true) }
        matchNextJob?.cancel()
        matchNextJob = viewModelScope.launch {
            performMatchNext(endCurrentCall = true, callConnected = true)
        }
    }

    private suspend fun performMatchNext(endCurrentCall: Boolean, callConnected: Boolean) {
        if (matchSessionReleased) return
        val unjoinableStreak =
            runtime.matchSessionCoordinator.recordRoomOutcome(connected = callConnected)
        if (unjoinableStreak >= UNJOINABLE_ROOM_LIMIT) {
            matchNextTransitionActive = false
            endAndCloseMatchSession()
            runtime.matchSessionCoordinator.resetRoomOutcomes()
            runtime.matchSessionCoordinator.publishContinuation(
                com.example.demoproject.platform.data.match.MatchContinuation.Failure(
                    message = str(R.string.call_status_match_rooms_unavailable),
                ),
            )
            _effects.send(CallEffect.OpenMatch)
            return
        }
        runtime.matchSessionCoordinator.publishContinuation(
            com.example.demoproject.platform.data.match.MatchContinuation.Searching(
                callConnected = callConnected,
            ),
        )
        val snapshot = _uiState.value
        if (endCurrentCall && snapshot.callRoomId.isNotBlank()) {
            runtime.callSessionRepository.endCall(
                roomId = snapshot.callRoomId,
                durationSeconds = snapshot.callDurationSec,
            )
        }
        if (endCurrentCall) {
            CallKitHolder.coordinator?.endBySystem(EndReason.Hangup)
        }
        when (
            val result = runtime.matchRepository.nextMatch(
                source = "match_screen_next",
            )
        ) {
            is MatchStartResult.Success -> {
                runtime.matchSessionCoordinator.publishContinuation(
                    com.example.demoproject.platform.data.match.MatchContinuation.Success(
                        result.info,
                    ),
                )
            }
            is MatchStartResult.Failure -> {
                runtime.matchSessionCoordinator.publishContinuation(
                    com.example.demoproject.platform.data.match.MatchContinuation.Failure(
                        message = result.message,
                        rechargePageData = result.rechargePageData,
                    ),
                )
            }
        }
        matchNextTransitionActive = false
        _effects.send(CallEffect.OpenMatch)
    }

    /**
     * Leaves the match queue for good. Once this runs, a later server `CallEnded` for the same
     * room must not re-enter the queue via [performMatchNext].
     */
    private suspend fun endAndCloseMatchSession() {
        if (matchSessionReleased) return
        matchSessionReleased = true
        matchNextJob?.cancel()
        matchNextJob = null
        val sessionId = _uiState.value.matchSessionId
        if (sessionId > 0L) {
            runtime.matchRepository.endMatch(sessionId, source = "match_call_end")
        }
        runtime.matchRepository.closeMatch(source = "match_call_close")
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
        const val REMATCH_DELAY_MS = 1_000L
        const val UNJOINABLE_ROOM_LIMIT = 3
        const val DEFAULT_RENEW_AHEAD_SEC = 300
        const val IN_CALL_MSG_TYPE_TIP = 4
    }
}

private fun CallCreateResult.Failure.isInsufficientBalance(): Boolean =
    rechargePageData != null ||
        code == INSUFFICIENT_BALANCE_ERROR_CODE ||
        code == MSG_SEND_INSUFFICIENT_BALANCE_ERROR_CODE
