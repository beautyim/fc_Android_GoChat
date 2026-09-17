package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.network.result.AppResult

data class RechargePageData(
    val balance: Int,
    val hotProducts: List<RechargeProduct>,
    val products: List<RechargeProduct>,
    val saleItems: List<RechargeSaleItem>,
    val vipPayItem: RechargeVipPayItem? = null,
    /** VIP offers for the recharge-page carousel (may contain [vipPayItem]). */
    val vipPayItems: List<RechargeVipPayItem> = emptyList(),
) {
    val isEmpty: Boolean
        get() = hotProducts.isEmpty() &&
            products.isEmpty() &&
            saleItems.isEmpty() &&
            vipPayItem == null &&
            vipPayItems.isEmpty()

    /** Fills missing catalog data from the full coin index when alert payloads are partial. */
    fun withCarouselFrom(fullPage: RechargePageData): RechargePageData = copy(
        balance = fullPage.balance,
        hotProducts = hotProducts.ifEmpty { fullPage.hotProducts },
        products = products.ifEmpty { fullPage.products },
        saleItems = saleItems.ifEmpty { fullPage.saleItems },
        vipPayItem = vipPayItem ?: fullPage.vipPayItem,
        vipPayItems = vipPayItems.ifEmpty { fullPage.vipPayItems },
    )
}

data class RechargeProduct(
    val id: Long,
    val sku: String,
    val productType: Int = 1,
    val coinAmount: Int,
    val price: String,
    val originalPrice: String?,
    val saleLabel: String?,
    val labelType: Int,
    val iconUrl: String?,
    val coinIndex: Int,
)

data class RechargeSaleItem(
    val id: Long,
    val sku: String,
    val productType: Int = 1,
    val baseCoins: Int,
    val bonusCoins: Int,
    val totalCoins: Int,
    val price: String,
    val originalPrice: String?,
    val discountRate: String?,
    val styleIndex: Int,
    val iconUrl: String?,
    val coinIndex: Int,
    val showBonusAsMatch: Boolean,
    val matchCount: Int,
    // Null means the server didn't send a localized label; the UI layer falls back to the
    // localized `recharge_super_discount` string resource instead of a hardcoded English literal.
    val superDiscountLabel: String?,
)

data class RechargeVipPayItem(
    val id: Long,
    val sku: String,
    val productType: Int = 2,
    /** API `title` segment (e.g. "Month"); UI composes with [month] + local "VIP". */
    val title: String,
    val month: Int = 0,
    val price: String,
    val originalPrice: String? = null,
    val giveCoins: Int,
    val matchCount: Int,
    /** Corner ribbon from API `label`; UI falls back to localized Try Now when null/blank. */
    val badgeLabel: String? = null,
)

/**
 * Treasure / promo offer from `promo/goods` (or MQTT treasure callback).
 */
data class PromoGoods(
    val remainSeconds: Long,
    val originalPrice: String,
    val salePrice: String,
    val content: PromoGoodsContent,
    val goodsId: Long,
    val sku: String,
    val productType: BillingProductType,
    val fromType: Int,
    val fromId: Long,
    val orderFrom: Int,
    val sid: Int,
    val saveDesc: String = "",
)

sealed interface PromoGoodsContent {
    data class SmallCoins(
        val coins: Int,
        val coinIconUrl: String? = null,
    ) : PromoGoodsContent

    data class Vip(
        val vipTitle: String,
        val bonusCoins: Int,
        val matchCount: Int,
        val coinIconUrl: String? = null,
    ) : PromoGoodsContent

    data class DualCoins(
        val leftCoins: Int,
        val rightCoins: Int,
        val leftIconUrl: String? = null,
        val rightIconUrl: String? = null,
        val matchCount: Int = 0,
    ) : PromoGoodsContent
}

/**
 * Winning / prize-claim offer from `vip/event` or MQTT `winning_recharge_alert`.
 */
data class WinningOffer(
    val remainSeconds: Long,
    val originalPrice: String,
    val salePrice: String,
    val baseCoins: Int,
    val bonusCoins: Int,
    val baseCoinIconUrl: String? = null,
    val bonusCoinIconUrl: String? = null,
    val goodsId: Long,
    val sku: String,
    val productType: BillingProductType,
    val fromType: Int,
    val fromId: Long,
    val orderFrom: Int,
    val sid: Int,
    val hasPurchasableSku: Boolean,
)

interface CoinRepository {
    suspend fun getRechargePage(): AppResult<RechargePageData>

    /** `promo/goods` — treasure-box eligibility and offer payload; null when ineligible. */
    suspend fun getPromoGoods(
        tier: com.example.demoproject.platform.data.promotion.TreasureUserTier =
            com.example.demoproject.platform.data.promotion.TreasureUserTier.Unpaid,
    ): AppResult<PromoGoods?>

    /**
     * `vip/event` — winning offer for [sid].
     * [forceWinningSkin] maps any func_name as winning (HTTP path).
     * [needPrice] `1` when CTA must refill goods/sku.
     */
    suspend fun reportVipEvent(
        sid: Int,
        needPrice: Int = 0,
        fromId: Long = 0L,
        forceWinningSkin: Boolean = true,
        tier: com.example.demoproject.platform.data.promotion.TreasureUserTier =
            com.example.demoproject.platform.data.promotion.TreasureUserTier.Unpaid,
    ): AppResult<WinningOffer?>
}
