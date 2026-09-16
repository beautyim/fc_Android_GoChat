package com.example.demoproject.product.store

/**
 * Call balance offer guide (MQTT a_type=7 auto / float tap).
 * Carousel cards reuse [StoreVipOfferUi] / [StoreSaleOfferUi] from the store page.
 */
data class CallBalanceOfferGuideUiState(
    val isLoading: Boolean = false,
    val purchasingOfferId: Long? = null,
    val balance: Int = 0,
    val remainingSeconds: Int = 0,
    val vipOffers: List<StoreVipOfferUi> = emptyList(),
    val saleOffers: List<StoreSaleOfferUi> = emptyList(),
    val fromType: Int? = null,
) {
    val isCatalogEmpty: Boolean
        get() = vipOffers.isEmpty() && saleOffers.isEmpty()

    val primaryOfferId: Long?
        get() = vipOffers.firstOrNull()?.id ?: saleOffers.firstOrNull()?.id

    val primaryIsVip: Boolean
        get() = vipOffers.isNotEmpty()
}
