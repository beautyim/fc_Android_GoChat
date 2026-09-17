package com.example.demoproject.promotion

import com.example.demoproject.platform.data.repository.PromoGoods
import com.example.demoproject.platform.data.repository.PromoGoodsContent
import com.example.demoproject.platform.data.repository.WinningOffer
import com.example.demoproject.ui.designsystem.TreasureOfferVariant

data class PromotionPopupUiState(
    val activePopup: PromotionActivePopup? = null,
    val treasureEntryVisible: Boolean = false,
    val treasureRemainSeconds: Long = 0L,
    /** Paid users show SPECIAL OFFER instead of countdown. */
    val treasureEntryShowSpecialOffer: Boolean = false,
    val isPurchasing: Boolean = false,
    val toastMessage: String? = null,
)

sealed interface PromotionActivePopup {
    data class FreeMatch(val count: Int) : PromotionActivePopup
    data class FreeCall(val count: Int) : PromotionActivePopup
    data class Treasure(val offer: PromoGoods) : PromotionActivePopup
    data class Winning(val offer: WinningOffer) : PromotionActivePopup
}

sealed interface PromotionPopupIntent {
    data object Dismiss : PromotionPopupIntent
    data object FreeMatchStart : PromotionPopupIntent
    data object FreeCallStart : PromotionPopupIntent
    data object TreasureEntryClick : PromotionPopupIntent
    data object TreasureGetOffer : PromotionPopupIntent
    data object WinningClaim : PromotionPopupIntent
    data object ConsumeToast : PromotionPopupIntent
}

sealed interface PromotionPopupEffect {
    data object NavigateToMatch : PromotionPopupEffect
    data object NavigateToHome : PromotionPopupEffect
    data object NavigateToStore : PromotionPopupEffect
    data object StartFreeMatch : PromotionPopupEffect
    data class LaunchPurchase(
        val goodsId: Long,
        val sku: String,
        val productTypeApi: Int,
        val fromType: Int,
        val fromId: Long,
        val orderFrom: Int,
    ) : PromotionPopupEffect
}

fun PromoGoodsContent.toTreasureVariant(): TreasureOfferVariant = when (this) {
    is PromoGoodsContent.SmallCoins -> TreasureOfferVariant.SmallCoins(
        coins = coins,
        coinIconUrl = coinIconUrl,
    )
    is PromoGoodsContent.Vip -> TreasureOfferVariant.Vip(
        vipTitle = vipTitle,
        bonusCoins = bonusCoins,
        matchCount = matchCount,
        coinIconUrl = coinIconUrl,
    )
    is PromoGoodsContent.DualCoins -> {
        if (matchCount > 0) {
            TreasureOfferVariant.DualCoinsWithBonus(
                leftCoins = leftCoins,
                rightCoins = rightCoins,
                matchCount = matchCount,
                leftIconUrl = leftIconUrl,
                rightIconUrl = rightIconUrl,
            )
        } else {
            TreasureOfferVariant.DualCoins(
                leftCoins = leftCoins,
                rightCoins = rightCoins,
                leftIconUrl = leftIconUrl,
                rightIconUrl = rightIconUrl,
            )
        }
    }
}
