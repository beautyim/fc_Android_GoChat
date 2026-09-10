package com.example.demoproject.product.store

data class StoreUiState(
    val isLoading: Boolean = false,
    val status: String = "",
    val products: List<String> = emptyList(),
    val lastOrder: String = "",
)

sealed interface StoreIntent {
    data object LoadCatalog : StoreIntent
    /** HTTP create-only (debug/smoke). */
    data object CreateOrder : StoreIntent
    /** Full Google Play BillingClient purchase flow. */
    data object LaunchGooglePlayPurchase : StoreIntent
}

sealed interface StoreEffect {
    data class ShowMessage(val message: String) : StoreEffect
}
