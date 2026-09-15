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

interface CoinRepository {
    suspend fun getRechargePage(): AppResult<RechargePageData>
}
