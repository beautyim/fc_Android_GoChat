package com.example.demoproject.product.match

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.billing.StorePurchaseLauncherHolder
import com.example.demoproject.platform.data.match.MatchCallEntry
import com.example.demoproject.platform.data.match.MatchContinuation
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.network.dto.MATCH_SEX_FEMALE
import com.example.demoproject.platform.data.network.dto.MATCH_SEX_MALE
import com.example.demoproject.platform.data.notification.MatchImmersiveStore
import com.example.demoproject.platform.data.repository.BillingPaymentType
import com.example.demoproject.platform.data.repository.BillingProductType
import com.example.demoproject.platform.data.repository.MatchStartAction
import com.example.demoproject.platform.data.repository.MatchStartInfo
import com.example.demoproject.platform.data.repository.MatchStartResult
import com.example.demoproject.platform.data.repository.RechargePageData
import com.example.demoproject.platform.data.repository.StorePurchaseRequest
import com.example.demoproject.platform.data.repository.StorePurchaseResult
import com.example.demoproject.platform.mqtt.MatchSignal
import com.example.demoproject.platform.mqtt.MatchSignalParser
import com.example.demoproject.platform.mqtt.MatchSignalPeer
import com.example.demoproject.platform.mqtt.MatchSignalRoom
import com.example.demoproject.platform.mqtt.MqttRuntime
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.product.store.CoinPayGuideUiState
import com.example.demoproject.product.store.toCoinPayGuideUiState
import com.example.demoproject.product.store.R as StoreR
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
import kotlinx.coroutines.yield

class MatchViewModel(application: Application) : AndroidViewModel(application) {
    private val runtime = NetworkRuntime.get(application)
    private val coordinator = runtime.matchSessionCoordinator
    private val _uiState = MutableStateFlow(MatchUiState())
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()
    private val _effects = Channel<MatchEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var session: ActiveSession? = null
    private var requestJob: Job? = null
    private var heartJob: Job? = null
    private var roomWaitJob: Job? = null
    private var resetJob: Job? = null
    private var matchingStartedAtMs = 0L
    private var hostActivity: Activity? = null

    init {
        collectGlobalState()
        collectMqtt()
        collectContinuation()
        loadInfo()
        viewModelScope.launch {
            _uiState.collect { MatchImmersiveStore.setImmersive(it.isSearching) }
        }
    }

    override fun onCleared() {
        MatchImmersiveStore.setImmersive(false)
        super.onCleared()
    }

    fun bindActivity(activity: Activity?) {
        hostActivity = activity
    }

    fun onIntent(intent: MatchIntent) {
        when (intent) {
            MatchIntent.Refresh -> loadInfo()
            MatchIntent.OpenCoins -> send(MatchEffect.OpenStore)
            MatchIntent.OpenFilter -> _uiState.update {
                it.copy(isFilterSheetVisible = true, draftMatchSex = it.matchSex)
            }
            MatchIntent.DismissFilter ->
                _uiState.update { it.copy(isFilterSheetVisible = false) }
            is MatchIntent.SelectMatchSex ->
                _uiState.update { it.copy(draftMatchSex = intent.matchSex) }
            MatchIntent.ApplyFilters -> _uiState.update {
                it.copy(
                    matchSex = it.draftMatchSex,
                    isFilterSheetVisible = false,
                )
            }
            MatchIntent.ResetFilters ->
                _uiState.update { it.copy(draftMatchSex = MATCH_SEX_ALL) }
            MatchIntent.StartVideoMatch -> onStartVideoMatch()
            MatchIntent.CancelVideoMatch -> cancel()
            MatchIntent.DismissGenderGuide ->
                _uiState.update { it.copy(isGenderGuideVisible = false) }
            is MatchIntent.SelectGenderGuideSex ->
                _uiState.update { it.copy(guideMatchSex = intent.matchSex) }
            MatchIntent.ConfirmGenderGuide -> confirmGenderGuide()
            MatchIntent.DismissCoinPayGuide ->
                _uiState.update { it.copy(coinPayGuide = null) }
            is MatchIntent.PurchaseCoinPayGuideCoin ->
                purchaseCoinPayGuide(intent.offerId, isSale = false)
            is MatchIntent.PurchaseCoinPayGuideSale ->
                purchaseCoinPayGuide(intent.offerId, isSale = true)
        }
    }

    private fun onStartVideoMatch() {
        if (requestJob?.isActive == true || _uiState.value.isSearching) return
        if (_uiState.value.isGenderGuideVisible) return
        val sex = _uiState.value.matchSex
        if (sex == MATCH_SEX_ALL || sex == MATCH_SEX_MALE) {
            viewModelScope.launch {
                if (!shouldShowGenderGuide()) {
                    start()
                    return@launch
                }
                markGenderGuideShown()
                _uiState.update {
                    it.copy(
                        isGenderGuideVisible = true,
                        guideMatchSex = MATCH_SEX_FEMALE,
                    )
                }
            }
            return
        }
        start()
    }

    private fun confirmGenderGuide() {
        val selected = _uiState.value.guideMatchSex
            .takeIf { it == MATCH_SEX_MALE || it == MATCH_SEX_FEMALE }
            ?: MATCH_SEX_FEMALE
        _uiState.update {
            it.copy(
                isGenderGuideVisible = false,
                matchSex = selected,
                draftMatchSex = selected,
            )
        }
        start()
    }

    private suspend fun shouldShowGenderGuide(): Boolean {
        val userId = runtime.sessionManager.currentUserId.orEmpty()
        if (userId.isBlank()) return true
        val lastShown = runtime.appPrefs.matchGenderGuideShownAtMs(userId)
        if (lastShown <= 0L) return true
        return System.currentTimeMillis() - lastShown >= GENDER_GUIDE_INTERVAL_MS
    }

    private suspend fun markGenderGuideShown() {
        val userId = runtime.sessionManager.currentUserId.orEmpty()
        if (userId.isBlank()) return
        runtime.appPrefs.markMatchGenderGuideShown(userId)
    }

    private fun collectGlobalState() {
        viewModelScope.launch {
            runtime.accountBalanceStore.coins.collect { coins ->
                _uiState.update {
                    it.copy(
                        coinBalance = coins,
                        coinPayGuide = it.coinPayGuide?.copy(balance = coins),
                    )
                }
            }
        }
        viewModelScope.launch {
            runtime.vipStatusStore.status.collect { vip ->
                vip ?: return@collect
                _uiState.update {
                    it.copy(
                        isVip = vip.isVip,
                        matchPrice = if (vip.isVip) it.vipMatchPrice else it.payUserMatchPrice,
                    )
                }
            }
        }
        viewModelScope.launch {
            runtime.matchQuotaStore.quota.collect { quota ->
                _uiState.update {
                    it.copy(
                        matchFreeCount = quota.matchFreeCount,
                        flashCount = quota.flashCount,
                    )
                }
            }
        }
        viewModelScope.launch {
            runtime.chatUnreadStore.total.collect { unread ->
                _uiState.update { it.copy(chatUnreadCount = unread) }
            }
        }
    }

    private fun collectMqtt() {
        viewModelScope.launch {
            MqttRuntime.get(getApplication()).manager.messageFlow.collect { message ->
                MatchSignalParser.parse(message.payload, runtime.json)?.let { consumeSignal(it) }
            }
        }
    }

    private fun collectContinuation() {
        viewModelScope.launch {
            coordinator.continuation.collect { continuation ->
                continuation ?: return@collect
                coordinator.consumeContinuation(continuation)
                when (continuation) {
                    is MatchContinuation.Searching -> enterMatching()
                    is MatchContinuation.Success -> {
                        enterMatching()
                        applyStartInfo(continuation.info, allowProfile = false)
                    }
                    is MatchContinuation.Failure -> {
                        clearJobs()
                        session = null
                        _uiState.update {
                            it.copy(
                                phase = MatchPhase.Ready,
                                statusMessage = continuation.message,
                            )
                        }
                        // Insufficient balance opens the in-place coin guide; other failures toast.
                        if (continuation.rechargePageData != null) {
                            presentCoinPayGuide(continuation.rechargePageData)
                        } else if (continuation.message.isNotBlank()) {
                            send(MatchEffect.ShowMessage(continuation.message))
                        }
                    }
                }
            }
        }
    }

    private fun loadInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = runtime.matchRepository.getMatchInfo(SOURCE_INFO)) {
                is AppResult.Success -> _uiState.update {
                    val info = result.data
                    it.copy(
                        isLoading = false,
                        matchFreeCount = info.matchFreeCount,
                        payUserMatchPrice = info.payUserMatchPrice,
                        vipMatchPrice = info.vipMatchPrice,
                        matchPrice = if (it.isVip) {
                            info.vipMatchPrice
                        } else {
                            info.payUserMatchPrice
                        },
                        statusMessage = "",
                    )
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(isLoading = false, statusMessage = result.message)
                    }
                    send(MatchEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun start() {
        if (requestJob?.isActive == true || _uiState.value.isSearching) return
        coordinator.resetRoomOutcomes()
        enterMatching()
        requestJob = viewModelScope.launch {
            when (
                val result = runtime.matchRepository.startMatch(
                    matchSex = selectedSex(),
                    source = SOURCE_START,
                )
            ) {
                is MatchStartResult.Success -> applyStartInfo(result.info, allowProfile = true)
                is MatchStartResult.Failure -> consumeFailure(result)
            }
        }
    }

    private fun enterMatching() {
        clearJobs()
        matchingStartedAtMs = System.currentTimeMillis()
        _uiState.update { it.copy(phase = MatchPhase.Matching, statusMessage = "") }
    }

    private suspend fun applyStartInfo(info: MatchStartInfo, allowProfile: Boolean) {
        val old = session
        session = ActiveSession(
            sessionId = info.sessionId ?: old?.sessionId,
            matchId = info.matchId ?: old?.matchId,
            heartbeatSeconds = info.heartIntervalSeconds ?: 0,
            peer = null,
        )
        startHeartbeat()
        val profileId = info.matchedUser?.profileUserId
        if (allowProfile &&
            info.nextAction == MatchStartAction.UserProfile &&
            !profileId.isNullOrBlank()
        ) {
            _uiState.update { it.copy(phase = MatchPhase.Matched) }
            endAndClose()
            clearJobs()
            session = null
            _uiState.update { it.copy(phase = MatchPhase.Ready) }
            send(MatchEffect.OpenProfile(profileId))
        }
    }

    private suspend fun consumeFailure(failure: MatchStartResult.Failure) {
        clearJobs()
        session = null
        if (failure.rechargePageData != null) {
            _uiState.update {
                it.copy(phase = MatchPhase.Ready, statusMessage = failure.message)
            }
            presentCoinPayGuide(failure.rechargePageData)
        } else {
            fail(failure.message.ifBlank { text(R.string.match_status_started) })
        }
    }

    private fun presentCoinPayGuide(fromCallback: RechargePageData?) {
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
            val fallbackLabel = text(StoreR.string.store_super_discount)
            val ui = page?.toCoinPayGuideUiState(
                fallbackSuperDiscountLabel = fallbackLabel,
                fromType = MATCH_COIN_GUIDE_FROM_TYPE,
            )
            if (ui == null || ui.isCatalogEmpty) {
                _uiState.update { state ->
                    if (state.coinPayGuide?.isLoading == true) {
                        state.copy(coinPayGuide = null)
                    } else {
                        state
                    }
                }
                send(MatchEffect.OpenStore)
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
            send(MatchEffect.ShowMessage(text(StoreR.string.store_status_no_activity)))
            return
        }
        val launcher = StorePurchaseLauncherHolder.launcher
        if (launcher == null) {
            send(MatchEffect.ShowMessage(text(StoreR.string.store_status_launcher_missing)))
            return
        }
        val request = StorePurchaseRequest(
            uiId = "match-coin-guide-$goodsId",
            goodsId = goodsId,
            productId = sku,
            productType = BillingProductType.Coins,
            paymentType = BillingPaymentType.GooglePlay,
            fromType = guide.fromType ?: MATCH_COIN_GUIDE_FROM_TYPE,
        )
        viewModelScope.launch {
            _uiState.update {
                it.copy(coinPayGuide = it.coinPayGuide?.copy(purchasingOfferId = offerId))
            }
            launcher.launch(activity, request) { result ->
                when (result) {
                    is StorePurchaseResult.Success -> {
                        _uiState.update { it.copy(coinPayGuide = null) }
                        send(
                            MatchEffect.ShowMessage(
                                text(StoreR.string.store_status_purchase_verified),
                            ),
                        )
                    }
                    is StorePurchaseResult.Canceled -> {
                        _uiState.update {
                            it.copy(
                                coinPayGuide = it.coinPayGuide?.copy(purchasingOfferId = null),
                            )
                        }
                        send(
                            MatchEffect.ShowMessage(
                                text(StoreR.string.store_status_purchase_canceled),
                            ),
                        )
                    }
                    is StorePurchaseResult.ExternalCheckoutOpened -> {
                        _uiState.update {
                            it.copy(
                                coinPayGuide = it.coinPayGuide?.copy(purchasingOfferId = null),
                            )
                        }
                        send(
                            MatchEffect.ShowMessage(
                                text(StoreR.string.store_status_external_checkout_opened),
                            ),
                        )
                    }
                    is StorePurchaseResult.Failed -> {
                        _uiState.update {
                            it.copy(
                                coinPayGuide = it.coinPayGuide?.copy(purchasingOfferId = null),
                            )
                        }
                        send(
                            MatchEffect.ShowMessage(
                                getApplication<Application>().getString(
                                    StoreR.string.store_status_purchase_failed_fmt,
                                    result.message,
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }

    private suspend fun consumeSignal(signal: MatchSignal) {
        if (!_uiState.value.isSearching) return
        when (signal) {
            is MatchSignal.Success -> consumeSuccess(signal)
            is MatchSignal.RoomReady -> {
                val current = session ?: return
                val peer = signal.peer ?: current.peer ?: return
                if (signal.room.canJoinDirectly) openCall(peer, signal.room)
            }
            is MatchSignal.PreJoinEnded -> {
                if (roomWaitJob?.isActive == true) requestNext(SOURCE_PREJOIN_END)
            }
            is MatchSignal.Notice -> {
                if (signal.message.isNotBlank()) send(MatchEffect.ShowMessage(signal.message))
            }
            is MatchSignal.Terminal -> {
                awaitIntro()
                endAndClose()
                fail(
                    text(
                        if (signal.kind == MatchSignal.Terminal.Kind.Empty) {
                            R.string.match_status_no_result
                        } else {
                            R.string.match_status_ended
                        },
                    ),
                )
            }
        }
    }

    private suspend fun consumeSuccess(signal: MatchSignal.Success) {
        awaitIntro()
        val old = session
        val peer = signal.peer ?: old?.peer
        session = ActiveSession(
            sessionId = signal.matchSessionId ?: old?.sessionId,
            matchId = signal.matchId ?: old?.matchId,
            heartbeatSeconds = old?.heartbeatSeconds ?: 0,
            peer = peer,
        )
        val room = signal.room
        if (peer != null && room?.canJoinDirectly == true) {
            openCall(peer, room)
        } else {
            _uiState.update { it.copy(phase = MatchPhase.Matched) }
            waitForRoom()
        }
    }

    private suspend fun openCall(peer: MatchSignalPeer, room: MatchSignalRoom) {
        val current = session ?: return
        val sessionId = current.sessionId?.takeIf { it > 0L } ?: return
        roomWaitJob?.cancel()
        heartJob?.cancel()
        val entryId = coordinator.putCallEntry(
            MatchCallEntry(sessionId, current.matchId, peer, room),
        )
        _uiState.update { it.copy(phase = MatchPhase.Matched) }
        send(
            MatchEffect.StartVideoCall(
                entryId = entryId,
                userId = peer.userId,
                nickname = peer.nickname,
                avatarUrl = peer.avatarUrl,
                age = peer.age,
            ),
        )
    }

    private fun waitForRoom() {
        roomWaitJob?.cancel()
        roomWaitJob = viewModelScope.launch {
            delay(ROOM_WAIT_MS)
            if (_uiState.value.isSearching) {
                roomWaitJob = null
                requestNext(SOURCE_ROOM_TIMEOUT)
            }
        }
    }

    private suspend fun requestNext(source: String) {
        roomWaitJob?.cancel()
        when (
            val result = runtime.matchRepository.nextMatch(
                matchSex = selectedSex(),
                source = source,
            )
        ) {
            is MatchStartResult.Success -> {
                enterMatching()
                applyStartInfo(result.info, allowProfile = false)
            }
            is MatchStartResult.Failure -> consumeFailure(result)
        }
    }

    private fun startHeartbeat() {
        heartJob?.cancel()
        val current = session ?: return
        val id = current.sessionId?.takeIf { it > 0L } ?: return
        val interval = current.heartbeatSeconds.takeIf { it > 0 } ?: return
        heartJob = viewModelScope.launch {
            while (isActive && _uiState.value.phase == MatchPhase.Matching) {
                delay(interval * 1_000L)
                val result = runtime.matchRepository.heartMatch(id, SOURCE_HEART)
                if (result is AppResult.Failure) {
                    AppLogger.d(TAG, "match heart failed: ${result.message}")
                }
            }
        }
    }

    private fun cancel() {
        if (!_uiState.value.isSearching) return
        requestJob?.cancel()
        viewModelScope.launch {
            endAndClose()
            clearJobs()
            session = null
            _uiState.update { it.copy(phase = MatchPhase.Ready, statusMessage = "") }
        }
    }

    private suspend fun endAndClose() {
        session?.sessionId?.takeIf { it > 0L }?.let {
            runtime.matchRepository.endMatch(it, SOURCE_END)
        }
        runtime.matchRepository.closeMatch(SOURCE_CLOSE)
    }

    private suspend fun fail(message: String) {
        clearJobs()
        session = null
        _uiState.update { it.copy(phase = MatchPhase.Failed, statusMessage = message) }
        if (message.isNotBlank()) send(MatchEffect.ShowMessage(message))
        resetJob?.cancel()
        resetJob = viewModelScope.launch {
            delay(FAILED_MS)
            _uiState.update {
                if (it.phase == MatchPhase.Failed) it.copy(phase = MatchPhase.Ready) else it
            }
        }
    }

    private suspend fun awaitIntro() {
        val remaining = INTRO_MS - (System.currentTimeMillis() - matchingStartedAtMs)
        if (remaining > 0L) delay(remaining)
    }

    private fun clearJobs() {
        heartJob?.cancel()
        roomWaitJob?.cancel()
        requestJob = null
        heartJob = null
        roomWaitJob = null
    }

    private fun send(effect: MatchEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun selectedSex(): Int =
        _uiState.value.matchSex.takeIf { it > 0 } ?: MATCH_SEX_FEMALE

    private fun text(id: Int): String = getApplication<Application>().getString(id)

    private data class ActiveSession(
        val sessionId: Long?,
        val matchId: Long?,
        val heartbeatSeconds: Int,
        val peer: MatchSignalPeer?,
    )

    private companion object {
        const val TAG = "MatchViewModel"
        const val ROOM_WAIT_MS = 12_000L
        const val INTRO_MS = 1_200L
        const val FAILED_MS = 1_500L
        const val SOURCE_INFO = "feature_match"
        const val SOURCE_START = "feature_match"
        const val SOURCE_HEART = "feature_match_heart"
        const val SOURCE_END = "feature_match_end"
        const val SOURCE_CLOSE = "feature_match_close"
        const val SOURCE_ROOM_TIMEOUT = "match_room_timeout"
        const val SOURCE_PREJOIN_END = "match_prejoin_end"
        const val GENDER_GUIDE_INTERVAL_MS = 10 * 60_000L
        /** Billing `from_type` for match-tab coin pay-guide purchases. */
        const val MATCH_COIN_GUIDE_FROM_TYPE = 1
    }
}
