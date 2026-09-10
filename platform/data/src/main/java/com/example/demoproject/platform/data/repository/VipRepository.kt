package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.network.result.AppResult

data class VipPageData(
    val user: VipUser,
    val plans: List<VipPlan>,
    val benefits: List<VipBenefit>,
) {
    val isEmpty: Boolean get() = plans.isEmpty() && benefits.isEmpty()

    fun benefitsForPlan(sku: String?): List<VipBenefit> {
        val selectedPlan = plans.firstOrNull { it.sku == sku } ?: plans.firstOrNull()
        val planBenefits = selectedPlan?.benefits?.takeIf { it.isNotEmpty() } ?: benefits
        return planBenefits.sortedForVipPurchaseDisplay()
    }
}

/**
 * VIP purchase page display order (design):
 * Reward coins → Reward Match → Match 50%OFF → VIP Badge → Hide online status.
 *
 * API privilege ids: 1=Badge, 2=Hide, 3=Coins, 4=Match, 5=Discount.
 */
fun List<VipBenefit>.sortedForVipPurchaseDisplay(): List<VipBenefit> =
    sortedByPrivilegeIds(VipPurchaseBenefitDisplayOrder)

/**
 * VIP payment-guide chip grid (2 rows, first row has ceil(n/2) items):
 * Reward coins → Reward Match → VIP Badge → Match 50%OFF → Hide online status.
 */
fun List<VipBenefit>.sortedForVipGuideDisplay(): List<VipBenefit> =
    sortedByPrivilegeIds(VipGuideBenefitDisplayOrder)

private fun List<VipBenefit>.sortedByPrivilegeIds(order: Map<Int, Int>): List<VipBenefit> =
    sortedWith(
        compareBy<VipBenefit> { order[it.id] ?: Int.MAX_VALUE }
            .thenBy { it.id },
    )

private val VipPurchaseBenefitDisplayOrder: Map<Int, Int> = mapOf(
    3 to 0,
    4 to 1,
    5 to 2,
    1 to 3,
    2 to 4,
)

private val VipGuideBenefitDisplayOrder: Map<Int, Int> = mapOf(
    3 to 0,
    4 to 1,
    1 to 2,
    5 to 3,
    2 to 4,
)

data class VipUser(
    val isVip: Boolean,
    val expiryText: String?,
    val nickname: String,
    val avatarUrl: String?,
    val sex: Int,
)

data class VipPlan(
    val id: Long,
    val sku: String,
    val productType: Int = 2,
    val title: String,
    val days: Int,
    val month: Int,
    val dayDesc: String,
    val price: String,
    val saleText: String,
    val label: String,
    val hidden: Boolean,
    val iconUrl: String?,
    val benefits: List<VipBenefit> = emptyList(),
)

data class VipBenefit(
    val id: Int,
    val title: String,
    val tips: String,
    val iconUrl: String?,
)

interface VipRepository {
    suspend fun getVipPage(): AppResult<VipPageData>
}
