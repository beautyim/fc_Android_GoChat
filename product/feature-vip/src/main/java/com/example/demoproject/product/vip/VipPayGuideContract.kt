package com.example.demoproject.product.vip

data class VipPayGuideBenefitUi(
    val id: Int,
    val title: String,
    val iconUrl: String?,
)

data class VipPayGuidePlanUi(
    val id: Long,
    val sku: String,
    val productType: Int,
    val title: String,
    val price: String,
    val originalPrice: String?,
)

data class VipPayGuideUiState(
    val peerNickname: String = "",
    val benefits: List<VipPayGuideBenefitUi> = emptyList(),
    val plan: VipPayGuidePlanUi? = null,
    val fromType: Int = 2,
    val isLoading: Boolean = false,
    val isPurchasing: Boolean = false,
)
