package com.example.demoproject.product.store

/**
 * Post-call hangup recharge guide (MQTT / heartbeat insufficient-balance end).
 * Catalog rows reuse [StoreSaleOfferUi] / [StoreCoinOfferUi] from the store page.
 */
data class CallHangupRechargeUiState(
    val isLoading: Boolean = false,
    val purchasingOfferId: Long? = null,
    val peerNickname: String = "",
    val peerAge: Int = 0,
    val peerAvatarUrl: String = "",
    val saleOffers: List<StoreSaleOfferUi> = emptyList(),
    val coinOffers: List<StoreCoinOfferUi> = emptyList(),
    val fromType: Int? = null,
) {
    val isCatalogEmpty: Boolean
        get() = saleOffers.isEmpty() && coinOffers.isEmpty()

    val displayName: String
        get() = if (peerAge > 0 && peerNickname.isNotBlank()) {
            "$peerNickname, $peerAge"
        } else {
            peerNickname
        }
}

/**
 * After a successful hangup-recharge purchase — continue video or open chat.
 */
data class CallHangupContinueUiState(
    val peerNickname: String = "",
    val peerAge: Int = 0,
    val peerAvatarUrl: String = "",
    val peerUserId: String = "",
    val videoUrl: String = "",
    val coverUrl: String = "",
) {
    val displayName: String
        get() = if (peerAge > 0 && peerNickname.isNotBlank()) {
            "$peerNickname, $peerAge"
        } else {
            peerNickname
        }
}
