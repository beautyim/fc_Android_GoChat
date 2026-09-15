package com.example.demoproject.product.store

data class StoreCoinOfferUi(
    val id: Long,
    val sku: String,
    val coinAmount: Int,
    val price: String,
    val originalPrice: String?,
    val discountLabel: String?,
    val cornerBadge: String?,
    val iconUrl: String?,
)

data class StoreVipOfferUi(
    val id: Long,
    val sku: String,
    /** API `title` segment; compose with [month] + local VIP suffix in UI. */
    val title: String,
    val month: Int,
    val price: String,
    val originalPrice: String?,
    val giveCoins: Int,
    val matchCount: Int,
    val badgeLabel: String?,
)

/** Sale / super-discount carousel card (Figma store sale promo). */
data class StoreSaleOfferUi(
    val id: Long,
    val sku: String,
    val baseCoins: Int,
    val bonusCoins: Int,
    val price: String,
    val originalPrice: String?,
    /** Percent only, e.g. "72%"; UI appends localized OFF. */
    val discountPercent: String?,
    val badgeLabel: String?,
)

data class StoreUiState(
    val isLoading: Boolean = false,
    /** Offer currently in purchase flow; drives button-level loading only. */
    val purchasingOfferId: Long? = null,
    val balance: Int = 0,
    val errorMessage: String? = null,
    val vipOffers: List<StoreVipOfferUi> = emptyList(),
    val saleOffers: List<StoreSaleOfferUi> = emptyList(),
    val coinOffers: List<StoreCoinOfferUi> = emptyList(),
) {
    val isCatalogEmpty: Boolean
        get() = vipOffers.isEmpty() && saleOffers.isEmpty() && coinOffers.isEmpty()
}

sealed interface StoreIntent {
    data object Refresh : StoreIntent
    data class PurchaseCoin(val offerId: Long) : StoreIntent
    data class PurchaseVip(val offerId: Long) : StoreIntent
    data class PurchaseSale(val offerId: Long) : StoreIntent
}

sealed interface StoreEffect {
    data class ShowMessage(val message: String) : StoreEffect
}
