package com.example.demoproject.product.call

import com.example.demoproject.platform.data.repository.CallBalanceAlertTiming
import com.example.demoproject.product.store.StoreSaleOfferUi
import com.example.demoproject.product.store.StoreVipOfferUi

/** Visual variant for the in-call balance floating window. */
enum class CallBalanceOfferVariant {
    /** MQTT carries a VIP SKU — float shows "Upgrade VIP". */
    NonVip,
    /** Coin SKU with bonus coins — float without VIP title. */
    Vip,
    /** Free call or coin pack without bonus — float without VIP title. */
    Free,
}

/**
 * In-call balance offer driven by MQTT `a_type=7`.
 * Primary CTA SKU comes only from the alert payload — never replaced by `/coin/index`.
 */
data class CallBalanceOffer(
    val roomKey: String,
    val variant: CallBalanceOfferVariant,
    val remainingSeconds: Int,
    val price: String,
    val originalPrice: String?,
    /** Percent token for OFF badge, e.g. "50%". */
    val offPercent: String?,
    val giveCoins: Int,
    val matchCount: Int,
    val baseCoins: Int,
    val vipOffer: StoreVipOfferUi? = null,
    val saleOffer: StoreSaleOfferUi? = null,
    val coinSku: String = "",
    val coinGoodsId: Long = 0L,
    val isVipPurchase: Boolean = false,
) {
    val showVipTitle: Boolean get() = variant == CallBalanceOfferVariant.NonVip

    /** Coin / free packs use the "More Coins" title (Figma `502:3673`). */
    val showCoinTitle: Boolean
        get() = variant == CallBalanceOfferVariant.Vip ||
            variant == CallBalanceOfferVariant.Free

    /** Displayed coin amount on the float product card. */
    val displayCoinAmount: Int
        get() = (baseCoins + giveCoins).takeIf { it > 0 }
            ?: baseCoins.takeIf { it > 0 }
            ?: giveCoins.coerceAtLeast(0)

    val formattedCountdown: String
        get() {
            val total = remainingSeconds.coerceAtLeast(0)
            val hours = total / 3600
            val minutes = (total % 3600) / 60
            val seconds = total % 60
            return "%02d:%02d:%02d".format(hours, minutes, seconds)
        }
}

fun shouldShowBalanceAlertFloatingWindow(
    remainingSeconds: Int,
    elapsedSeconds: Int,
    isFreeCall: Boolean,
    freeCallTriggerConsumed: Boolean,
): Boolean {
    // Free-call 10s is a one-shot arm: once elapsed crosses 10s, keep eligible until consumed.
    // Do not require near-hangup remaining for this branch.
    val freeCallForce = isFreeCall &&
        !freeCallTriggerConsumed &&
        elapsedSeconds >= CallBalanceAlertTiming.FreeCallForceShowSeconds
    if (freeCallForce) return true

    if (remainingSeconds <= 0) return false
    return if (elapsedSeconds < CallBalanceAlertTiming.LongCallElapsedSeconds) {
        remainingSeconds in 1..CallBalanceAlertTiming.ShortCallNearHangupSeconds
    } else {
        remainingSeconds in 1..CallBalanceAlertTiming.LongCallNearHangupSeconds
    }
}

fun shouldShowMatchBalanceAlertFloatingWindow(
    remainingSeconds: Int,
    saleRechargeAlertTimeSeconds: Int,
): Boolean {
    if (saleRechargeAlertTimeSeconds <= 0) return false
    return remainingSeconds in 1..saleRechargeAlertTimeSeconds
}

fun shouldAutoShowBalanceAlertOffer(
    remainingSeconds: Int,
    rechargeAlertTimeSeconds: Int,
): Boolean {
    val threshold = rechargeAlertTimeSeconds.takeIf { it > 0 }
        ?: CallBalanceAlertTiming.AutoOfferSeconds
    return remainingSeconds in 1..threshold
}

fun CallUiState.shouldShowBalanceFloat(
    imeVisible: Boolean,
    hasOtherOverlay: Boolean,
): Boolean {
    if (phase != CallRingingPhase.InCall) return false
    if (balanceOffer == null || balanceAlertRoomKey.isBlank()) return false
    if (isBalanceOfferGuideVisible || coinPayGuide != null) return false
    if (imeVisible || hasOtherOverlay) return false
    return showBalanceFloat
}
