package com.example.demoproject.product.vip

data class VipBenefitUi(
    val id: Int,
    val title: String,
    val iconUrl: String?,
)

data class VipPlanUi(
    val id: Long,
    val sku: String,
    val title: String,
    val price: String,
    val originalPrice: String?,
    val dayDesc: String,
    val saleText: String,
    val label: String,
)

data class VipPurchaseUiState(
    val isLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val isVip: Boolean = false,
    val expiryText: String? = null,
    val errorMessage: String? = null,
    val benefits: List<VipBenefitUi> = emptyList(),
    val plans: List<VipPlanUi> = emptyList(),
    val selectedPlanId: Long? = null,
)

sealed interface VipPurchaseIntent {
    data object Refresh : VipPurchaseIntent
    data class SelectPlan(val planId: Long) : VipPurchaseIntent
    data object PurchaseSelected : VipPurchaseIntent
}

sealed interface VipPurchaseEffect {
    data class ShowMessage(val message: String) : VipPurchaseEffect
}
