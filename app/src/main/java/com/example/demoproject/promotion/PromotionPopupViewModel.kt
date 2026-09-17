package com.example.demoproject.promotion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.network.mapper.isTreasureCallback
import com.example.demoproject.platform.data.network.mapper.toPromoGoodsOrNull
import com.example.demoproject.platform.data.network.mapper.toWinningOfferOrNull
import com.example.demoproject.platform.data.network.dto.toVipAlertCallbackDtoOrNull
import com.example.demoproject.platform.data.network.dto.VipAlertCallbackDto
import com.example.demoproject.platform.data.promotion.PromotionPopupSelector
import com.example.demoproject.platform.data.promotion.PromotionPopupType
import com.example.demoproject.platform.data.promotion.PromotionPresentGate
import com.example.demoproject.platform.data.promotion.PromotionPurchasePageTracker
import com.example.demoproject.platform.data.promotion.PromotionRechargeGuideTracker
import com.example.demoproject.platform.data.promotion.PromotionScheduleConstants
import com.example.demoproject.platform.data.promotion.PromotionScheduleLogic
import com.example.demoproject.platform.data.promotion.PromotionScheduleState
import com.example.demoproject.platform.data.promotion.PromotionTrigger
import com.example.demoproject.platform.data.promotion.PromotionTriggerBus
import com.example.demoproject.platform.data.promotion.TreasureUserTier
import com.example.demoproject.platform.data.repository.PromoGoods
import com.example.demoproject.platform.data.repository.WinningOffer
import com.example.demoproject.platform.mqtt.MqttRuntime
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.max

class PromotionPopupViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val runtime = NetworkRuntime.get(application)
    private val mqtt = MqttRuntime.get(application)

    private val _uiState = MutableStateFlow(PromotionPopupUiState())
    val uiState: StateFlow<PromotionPopupUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<PromotionPopupEffect>(extraBufferCapacity = 8)
    val effects = _effects.asSharedFlow()

    private var schedule = PromotionScheduleState()
    private var userId: String = ""
    private var gate = PromotionPresentGate(
        hostResumed = false,
        matchImmersive = false,
        excludedRoute = true,
        blockingOverlay = false,
        rechargeGuideBlocking = false,
        currentMainTab = null,
    )
    private var cachedTreasure: PromoGoods? = null
    private var pendingMqttWinning: WinningOffer? = null
    private var isFetching = false
    private var appOpenReady = false
    private var sessionBoundAt = 0L
    private var evaluateJob: Job? = null
    private var countdownJob: Job? = null
    private var networkRetryJob: Job? = null

    init {
        viewModelScope.launch {
            runtime.sessionManager.sessionFlow.collect { session ->
                if (session == null) {
                    countdownJob?.cancel()
                    networkRetryJob?.cancel()
                    evaluateJob?.cancel()
                    userId = ""
                    schedule = PromotionScheduleState()
                    cachedTreasure = null
                    pendingMqttWinning = null
                    appOpenReady = false
                    sessionBoundAt = 0L
                    isFetching = false
                    _uiState.value = PromotionPopupUiState()
                } else if (session.userId != userId) {
                    val boundUser = session.userId
                    userId = boundUser
                    schedule = runtime.promotionPopupStore.load(boundUser)
                    // SessionBound normally seeds after /app/init; if that event is missed
                    // (race / failed emit), still cold-start so treasure entry can show.
                    viewModelScope.launch {
                        delay(PromotionScheduleConstants.APP_OPEN_WAIT_MILLIS + 1_500L)
                        if (userId != boundUser) return@launch
                        if (sessionBoundAt > 0L) return@launch
                        handleTrigger(PromotionTrigger.SessionBound)
                    }
                }
            }
        }
        viewModelScope.launch {
            PromotionTriggerBus.events.collect { event ->
                handleTrigger(event)
            }
        }
        viewModelScope.launch {
            mqtt.manager.messageFlow.collect { inbound ->
                handleMqtt(inbound.payload, System.currentTimeMillis())
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                tick()
            }
        }
    }

    fun updateGate(transform: (PromotionPresentGate) -> PromotionPresentGate) {
        gate = transform(gate)
        updateTreasureEntryVisibility()
        requestEvaluate()
    }

    fun onIntent(intent: PromotionPopupIntent) {
        when (intent) {
            PromotionPopupIntent.Dismiss -> dismissActive(markShown = true)
            PromotionPopupIntent.FreeMatchStart -> {
                dismissActive(markShown = true)
                viewModelScope.launch {
                    _effects.emit(PromotionPopupEffect.StartFreeMatch)
                    _effects.emit(PromotionPopupEffect.NavigateToMatch)
                }
            }
            PromotionPopupIntent.FreeCallStart -> {
                dismissActive(markShown = true)
                viewModelScope.launch { _effects.emit(PromotionPopupEffect.NavigateToHome) }
            }
            PromotionPopupIntent.TreasureEntryClick -> openTreasureFromEntry()
            PromotionPopupIntent.TreasureGetOffer -> purchaseTreasure()
            PromotionPopupIntent.WinningClaim -> purchaseWinning()
            PromotionPopupIntent.ConsumeToast -> _uiState.update { it.copy(toastMessage = null) }
        }
    }

    fun onPurchaseResultSuccess() {
        PromotionPurchasePageTracker.markPurchased()
        PromotionRechargeGuideTracker.markPurchased()
        viewModelScope.launch { handleTrigger(PromotionTrigger.PurchaseVerifiedSuccess) }
    }

    private suspend fun handleTrigger(event: PromotionTrigger) {
        when (event) {
            PromotionTrigger.SessionBound -> onSessionBound()
            PromotionTrigger.PurchaseVerifiedSuccess -> {
                val now = System.currentTimeMillis()
                schedule = PromotionScheduleLogic.onPurchaseVerifiedSuccess(schedule, now)
                runtime.paidStatusStore.markPaidFromPurchase()
                persist()
                dismissActive(markShown = false)
                pendingMqttWinning = null
                requestEvaluate()
            }
            PromotionTrigger.PurchasePageClosedWithoutPurchase -> {
                schedule = PromotionScheduleLogic.enqueueWinning(
                    schedule,
                    PromotionScheduleConstants.SID_WINNING_PURCHASE_PAGE,
                    PromotionScheduleConstants.WINNING_PURCHASE_PAGE_DELAY_MILLIS,
                    System.currentTimeMillis(),
                )
                persist()
                requestEvaluate()
            }
            PromotionTrigger.RechargeGuideClosedWithoutPurchase -> {
                schedule = PromotionScheduleLogic.enqueueWinning(
                    schedule,
                    PromotionScheduleConstants.SID_WINNING_RECHARGE_GUIDE,
                    PromotionScheduleConstants.WINNING_RECHARGE_GUIDE_DELAY_MILLIS,
                    System.currentTimeMillis(),
                )
                persist()
                requestEvaluate()
            }
            is PromotionTrigger.TreasureMqtt -> {
                if (!schedule.registeredFirstEnterSession || runtime.paidStatusStore.isPaidUser) {
                    schedule = PromotionScheduleLogic.enqueueTreasureFromMqtt(schedule)
                    persist()
                }
                requestEvaluate()
            }
            is PromotionTrigger.WinningMqtt -> {
                if (PromotionPopupSelector.winningEligible(
                        runtime.paidStatusStore.isPaidUser,
                        runtime.accountBalanceStore.coins.value,
                    )
                ) {
                    // sid=0 marks MQTT-sourced winning so stash can be consumed.
                    schedule = schedule.copy(
                        winningInPool = true,
                        winningDueAt = System.currentTimeMillis(),
                        winningSid = 0,
                    )
                    persist()
                    requestEvaluate()
                }
            }
            PromotionTrigger.AppOpenReady -> {
                appOpenReady = true
                requestEvaluate()
            }
            PromotionTrigger.EvaluateNow -> requestEvaluate()
        }
    }

    private suspend fun onSessionBound() {
        val uid = runtime.sessionManager.currentUserId ?: return
        userId = uid
        schedule = runtime.promotionPopupStore.load(uid)
        val now = System.currentTimeMillis()
        sessionBoundAt = now
        appOpenReady = false
        schedule = PromotionScheduleLogic.onSessionBound(
            schedule,
            now,
            runtime.paidStatusStore.isPaidUser,
        )
        persist()
        // Wait up to 3s for /app/open quotas before evaluating (doc §3.1).
        viewModelScope.launch {
            val deadline = now + PromotionScheduleConstants.APP_OPEN_WAIT_MILLIS
            while (System.currentTimeMillis() < deadline && !appOpenReady) {
                delay(100L)
            }
            appOpenReady = true
            prefetchTreasureEntry()
            requestEvaluate()
        }
    }

    fun markAppOpenReady() {
        appOpenReady = true
        requestEvaluate()
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        val vip = runtime.vipStatusStore.status.value
        val isVip = vip?.isVip == true
        val vipExpired = !isVip && !vip?.expiryText.isNullOrBlank()
        var next = PromotionScheduleLogic.tickWelfareDue(schedule, now)
        next = PromotionScheduleLogic.tickTreasureRepool(
            next,
            now,
            runtime.paidStatusStore.isPaidUser,
            isVip,
            vipExpired,
        )
        // Clear winning if no longer eligible.
        if (next.winningInPool &&
            !PromotionPopupSelector.winningEligible(
                runtime.paidStatusStore.isPaidUser,
                runtime.accountBalanceStore.coins.value,
            )
        ) {
            next = next.copy(winningInPool = false, winningDueAt = 0L)
            pendingMqttWinning = null
        }
        if (next != schedule) {
            schedule = next
            viewModelScope.launch { persist() }
        }
        updateTreasureEntryVisibility()
        requestEvaluate()
    }

    private fun requestEvaluate() {
        if (evaluateJob?.isActive == true) return
        evaluateJob = viewModelScope.launch { evaluate() }
    }

    private suspend fun evaluate() {
        if (!appOpenReady && sessionBoundAt > 0L &&
            System.currentTimeMillis() - sessionBoundAt < PromotionScheduleConstants.APP_OPEN_WAIT_MILLIS
        ) {
            return
        }
        val selected = PromotionPopupSelector.select(
            PromotionSelectionInput(
                schedule = schedule,
                now = System.currentTimeMillis(),
                gate = gate,
                matchFreeCount = runtime.matchQuotaStore.quota.value.matchFreeCount,
                callFreeMin = runtime.callFreeMinStore.callFreeMin.value,
                coins = runtime.accountBalanceStore.coins.value,
                isPaidUser = runtime.paidStatusStore.isPaidUser,
                hasActivePopup = _uiState.value.activePopup != null,
                isFetching = isFetching,
            ),
        ) ?: return

        when (selected) {
            PromotionPopupType.FreeMatch -> showFreeMatch()
            PromotionPopupType.FreeCall -> showFreeCall()
            PromotionPopupType.TreasureBox -> showTreasure(activeClick = false)
            PromotionPopupType.Winning -> showWinning()
        }
    }

    private fun showFreeMatch() {
        val count = runtime.matchQuotaStore.quota.value.matchFreeCount
        if (count <= 0) return
        schedule = schedule.onWelfareShown(System.currentTimeMillis(), isMatch = true)
        viewModelScope.launch { persist() }
        _uiState.update {
            it.copy(activePopup = PromotionActivePopup.FreeMatch(count))
        }
    }

    private fun showFreeCall() {
        val count = runtime.callFreeMinStore.callFreeMin.value
        if (count <= 0) return
        schedule = schedule.onWelfareShown(System.currentTimeMillis(), isMatch = false)
        viewModelScope.launch { persist() }
        _uiState.update {
            it.copy(activePopup = PromotionActivePopup.FreeCall(count))
        }
    }

    private fun openTreasureFromEntry() {
        viewModelScope.launch { showTreasure(activeClick = true) }
    }

    private suspend fun showTreasure(activeClick: Boolean) {
        if (isFetching) return
        isFetching = true
        try {
            val tier = currentTier()
            when (val result = runtime.coinRepository.getPromoGoods(tier)) {
                is AppResult.Success -> {
                    val offer = result.data
                    if (offer == null) {
                        scheduleNetworkRetry()
                        return
                    }
                    cachedTreasure = offer
                    startEntryCountdown(offer.remainSeconds)
                    if (!activeClick &&
                        !runtime.paidStatusStore.isPaidUser &&
                        !schedule.registeredFirstEnterSession
                    ) {
                        // Entry only; mark first enter so later autos can fire.
                        schedule = schedule.markRegisteredFirstEnter()
                        persist()
                        updateTreasureEntryVisibility()
                        return
                    }
                    schedule = schedule.onTreasureShown(
                        System.currentTimeMillis(),
                        runtime.paidStatusStore.isPaidUser,
                    ).markRegisteredFirstEnter()
                    persist()
                    _uiState.update {
                        it.copy(activePopup = PromotionActivePopup.Treasure(offer))
                    }
                    updateTreasureEntryVisibility()
                }
                else -> scheduleNetworkRetry()
            }
        } finally {
            isFetching = false
        }
    }

    private suspend fun showWinning() {
        if (isFetching) return
        val sid = schedule.winningSid.takeIf { it > 0 }
            ?: PromotionScheduleConstants.DEFAULT_WINNING_SID
        // Scene triggers (10/11/12) must hit live vip/event — never reuse MQTT stash.
        val isSceneSid = sid == PromotionScheduleConstants.SID_WINNING_COLD ||
            sid == PromotionScheduleConstants.SID_WINNING_PURCHASE_PAGE ||
            sid == PromotionScheduleConstants.SID_WINNING_RECHARGE_GUIDE
        val mqttStash = pendingMqttWinning
        val useMqttStash = !isSceneSid && mqttStash != null

        isFetching = true
        try {
            val resolved = if (useMqttStash) {
                pendingMqttWinning = null
                mqttStash
            } else {
                pendingMqttWinning = null
                when (
                    val result = runtime.coinRepository.reportVipEvent(
                        sid = sid,
                        needPrice = 0,
                        fromId = 0L,
                        forceWinningSkin = true,
                        tier = currentTier(),
                    )
                ) {
                    is AppResult.Success -> result.data
                    else -> {
                        scheduleNetworkRetry()
                        null
                    }
                }
            } ?: return

            schedule = schedule.onWinningShown(System.currentTimeMillis())
            persist()
            _uiState.update {
                it.copy(activePopup = PromotionActivePopup.Winning(resolved))
            }
        } finally {
            isFetching = false
        }
    }

    private fun purchaseTreasure() {
        val offer = (_uiState.value.activePopup as? PromotionActivePopup.Treasure)?.offer
            ?: cachedTreasure
            ?: return
        if (offer.sku.isBlank() || offer.goodsId <= 0L) {
            viewModelScope.launch { _effects.emit(PromotionPopupEffect.NavigateToStore) }
            dismissActive(markShown = true)
            return
        }
        _uiState.update { it.copy(isPurchasing = true) }
        viewModelScope.launch {
            _effects.emit(
                PromotionPopupEffect.LaunchPurchase(
                    goodsId = offer.goodsId,
                    sku = offer.sku,
                    productTypeApi = offer.productType.apiValue,
                    fromType = offer.fromType.takeIf { it > 0 }
                        ?: PromotionScheduleConstants.DEFAULT_TREASURE_FROM_TYPE,
                    fromId = offer.fromId,
                    orderFrom = offer.orderFrom,
                ),
            )
        }
    }

    private fun purchaseWinning() {
        val offer = (_uiState.value.activePopup as? PromotionActivePopup.Winning)?.offer ?: return
        if (!offer.hasPurchasableSku) {
            viewModelScope.launch {
                refillWinningAndPurchase(offer)
            }
            return
        }
        launchWinningPurchase(offer)
    }

    private suspend fun refillWinningAndPurchase(offer: WinningOffer) {
        isFetching = true
        _uiState.update { it.copy(isPurchasing = true) }
        try {
            when (
                val result = runtime.coinRepository.reportVipEvent(
                    sid = offer.sid,
                    needPrice = 1,
                    fromId = offer.fromId,
                    forceWinningSkin = true,
                    tier = currentTier(),
                )
            ) {
                is AppResult.Success -> {
                    val filled = result.data
                    if (filled == null || !filled.hasPurchasableSku) {
                        dismissActive(markShown = true)
                        _effects.emit(PromotionPopupEffect.NavigateToStore)
                        return
                    }
                    _uiState.update {
                        it.copy(activePopup = PromotionActivePopup.Winning(filled))
                    }
                    launchWinningPurchase(filled)
                }
                else -> {
                    dismissActive(markShown = true)
                    _effects.emit(PromotionPopupEffect.NavigateToStore)
                }
            }
        } finally {
            isFetching = false
            _uiState.update { it.copy(isPurchasing = false) }
        }
    }

    private fun launchWinningPurchase(offer: WinningOffer) {
        _uiState.update { it.copy(isPurchasing = true) }
        viewModelScope.launch {
            _effects.emit(
                PromotionPopupEffect.LaunchPurchase(
                    goodsId = offer.goodsId,
                    sku = offer.sku,
                    productTypeApi = offer.productType.apiValue,
                    fromType = PromotionScheduleConstants.WINNING_CREATE_FROM_TYPE,
                    fromId = offer.fromId,
                    orderFrom = offer.orderFrom,
                ),
            )
        }
    }

    fun onPurchaseFinished(success: Boolean, failedMessage: String?) {
        _uiState.update { it.copy(isPurchasing = false) }
        if (success) {
            dismissActive(markShown = false)
            viewModelScope.launch { handleTrigger(PromotionTrigger.PurchaseVerifiedSuccess) }
        } else if (failedMessage != null) {
            _uiState.update { it.copy(toastMessage = failedMessage) }
        }
    }

    private fun dismissActive(markShown: Boolean) {
        val active = _uiState.value.activePopup
        _uiState.update { it.copy(activePopup = null, isPurchasing = false) }
        if (!markShown || active == null) return
        // Welfare already marked on show; treasure/winning too.
    }

    private suspend fun prefetchTreasureEntry() {
        val tier = currentTier()
        when (val result = runtime.coinRepository.getPromoGoods(tier)) {
            is AppResult.Success -> {
                result.data?.let { offer ->
                    cachedTreasure = offer
                    startEntryCountdown(offer.remainSeconds)
                }
            }
            else -> Unit
        }
        updateTreasureEntryVisibility()
    }

    private fun startEntryCountdown(remainSeconds: Long) {
        countdownJob?.cancel()
        val isPaid = runtime.paidStatusStore.isPaidUser
        if (isPaid) {
            _uiState.update {
                it.copy(
                    treasureRemainSeconds = 0L,
                    treasureEntryShowSpecialOffer = true,
                )
            }
            return
        }
        var remaining = if (remainSeconds > 0L) {
            remainSeconds
        } else {
            PromotionScheduleConstants.UNPAID_ENTRY_COUNTDOWN_SECONDS
        }
        _uiState.update {
            it.copy(
                treasureRemainSeconds = remaining,
                treasureEntryShowSpecialOffer = false,
            )
        }
        countdownJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                remaining = max(0L, remaining - 1L)
                _uiState.update { it.copy(treasureRemainSeconds = remaining) }
                if (remaining <= 0L) {
                    // Unpaid: restart local 3600s loop without auto-popup.
                    remaining = PromotionScheduleConstants.UNPAID_ENTRY_COUNTDOWN_SECONDS
                    _uiState.update { it.copy(treasureRemainSeconds = remaining) }
                }
            }
        }
    }

    private fun updateTreasureEntryVisibility() {
        val onMain = gate.currentMainTab != null &&
            !gate.matchImmersive &&
            !gate.excludedRoute
        val hasOffer = cachedTreasure != null || schedule.treasureInPool
        val visible = onMain && hasOffer && gate.hostResumed
        _uiState.update {
            it.copy(
                treasureEntryVisible = visible,
                treasureEntryShowSpecialOffer = runtime.paidStatusStore.isPaidUser,
            )
        }
    }

    private fun scheduleNetworkRetry() {
        networkRetryJob?.cancel()
        networkRetryJob = viewModelScope.launch {
            delay(PromotionScheduleConstants.NETWORK_RETRY_MILLIS)
            requestEvaluate()
        }
    }

    private fun currentTier(): TreasureUserTier {
        val paid = runtime.paidStatusStore.isPaidUser
        val vip = runtime.vipStatusStore.status.value
        val isVip = vip?.isVip == true
        return when {
            !paid -> TreasureUserTier.Unpaid
            isVip -> TreasureUserTier.PaidVip
            !vip?.expiryText.isNullOrBlank() -> TreasureUserTier.VipExpired
            else -> TreasureUserTier.PaidNonVip
        }
    }

    private suspend fun persist() {
        val uid = userId
        if (uid.isBlank()) return
        val snapshot = schedule
        // Drop stale writes after logout / user switch so delete+reregister cannot
        // resurrect a consumed schedule for a reused guest userId.
        if (runtime.sessionManager.currentUserId != uid) return
        runtime.promotionPopupStore.save(uid, snapshot)
    }

    private fun handleMqtt(payload: String, receivedAt: Long) {
        // Age gate: no receivedAt on inbound — approximate with parse time (immediate).
        val root = runCatching {
            runtime.json.parseToJsonElement(payload)
        }.getOrNull() ?: return
        val callbackElement = findFuncNameObject(root) ?: return
        val callback = callbackElement.toVipAlertCallbackDtoOrNull() ?: return
        when {
            callback.isTreasureCallback() -> {
                val offer = callback.toPromoGoodsOrNull(currentTier())
                if (offer != null) {
                    cachedTreasure = offer
                    startEntryCountdown(offer.remainSeconds)
                }
                PromotionTriggerBus.emit(PromotionTrigger.TreasureMqtt(offer?.remainSeconds ?: 0L))
            }
            callback.funcName.equals("winning_recharge_alert", ignoreCase = true) -> {
                val offer = callback.toWinningOfferOrNull(forceWinningSkin = false, currentTier())
                if (offer != null) {
                    pendingMqttWinning = offer
                    PromotionTriggerBus.emit(PromotionTrigger.WinningMqtt(offer.fromId))
                }
            }
        }
        @Suppress("UNUSED_VARIABLE")
        val age = receivedAt
    }

    private fun findFuncNameObject(element: JsonElement): JsonElement? {
        when (element) {
            is JsonObject -> {
                if (element.containsKey("func_name")) return element
                element.values.forEach { child ->
                    findFuncNameObject(child)?.let { return it }
                }
            }
            else -> Unit
        }
        return null
    }

    companion object {
        private const val TAG = "PromotionPopupVM"
    }
}

// Local alias to avoid importing SelectionInput name clash in evaluate.
private typealias PromotionSelectionInput =
    com.example.demoproject.platform.data.promotion.PromotionSelectionInput
