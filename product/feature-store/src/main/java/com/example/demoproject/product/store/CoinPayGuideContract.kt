package com.example.demoproject.product.store

/**
 * Coin pay-guide bottom sheet state (insufficient-balance recharge alert).
 * Catalog rows reuse [StoreSaleOfferUi] / [StoreCoinOfferUi] from the store page.
 */
data class CoinPayGuideUiState(
    val isLoading: Boolean = false,
    val purchasingOfferId: Long? = null,
    val balance: Int = 0,
    val saleOffers: List<StoreSaleOfferUi> = emptyList(),
    val coinOffers: List<StoreCoinOfferUi> = emptyList(),
    /** Billing `from_type` when opened from a callback alert; null for local catalog. */
    val fromType: Int? = null,
) {
    val isCatalogEmpty: Boolean
        get() = saleOffers.isEmpty() && coinOffers.isEmpty()
}

sealed interface CoinPayGuideIntent {
    data object Dismiss : CoinPayGuideIntent
    data class PurchaseCoin(val offerId: Long) : CoinPayGuideIntent
    data class PurchaseSale(val offerId: Long) : CoinPayGuideIntent
}
