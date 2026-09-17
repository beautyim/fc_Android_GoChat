package com.example.demoproject.platform.data.promotion

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Cross-cutting promotion triggers (session bind, purchase success, page close, MQTT).
 * Collected by the app-layer [PromotionPopupViewModel].
 */
object PromotionTriggerBus {
    private val _events = MutableSharedFlow<PromotionTrigger>(
        extraBufferCapacity = 32,
    )
    val events: SharedFlow<PromotionTrigger> = _events.asSharedFlow()

    fun emit(event: PromotionTrigger) {
        _events.tryEmit(event)
    }
}

sealed interface PromotionTrigger {
    data object SessionBound : PromotionTrigger
    data object PurchaseVerifiedSuccess : PromotionTrigger
    /** Store / VIP purchase page closed without a successful purchase. */
    data object PurchasePageClosedWithoutPurchase : PromotionTrigger
    /** Call balance / recharge guide closed without purchase. */
    data object RechargeGuideClosedWithoutPurchase : PromotionTrigger
    data class TreasureMqtt(val remainSeconds: Long) : PromotionTrigger
    data class WinningMqtt(val fromId: Long) : PromotionTrigger
    data object EvaluateNow : PromotionTrigger
    data object AppOpenReady : PromotionTrigger
}

/**
 * Tracks whether the user successfully purchased while on Store/VIP so close handlers
 * can decide whether to enqueue winning sid=11.
 */
object PromotionPurchasePageTracker {
    @Volatile
    var purchasedOnPage: Boolean = false

    fun markOpened() {
        purchasedOnPage = false
    }

    fun markPurchased() {
        purchasedOnPage = true
    }

    fun consumeClosedWithoutPurchase(): Boolean {
        val without = !purchasedOnPage
        purchasedOnPage = false
        return without
    }
}

/**
 * Tracks recharge-guide (call balance offer) dismiss without purchase → winning sid=12.
 */
object PromotionRechargeGuideTracker {
    @Volatile
    var purchased: Boolean = false

    fun markOpened() {
        purchased = false
    }

    fun markPurchased() {
        purchased = true
    }

    fun consumeClosedWithoutPurchase(): Boolean {
        val without = !purchased
        purchased = false
        return without
    }
}
