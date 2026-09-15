package com.example.demoproject.platform.data.repository

/**
 * VIP payment-guide payload from `msg/send` `ok = 3` (free messages used up).
 *
 * Benefit chips come from the selected plan's [VipGuidePlan.privileges]
 * (`vip_list[].privilege_infos`), not from `banner_new`.
 */
data class VipGuidePageData(
    val plans: List<VipGuidePlan>,
    val peerNickname: String = "",
    val peerAvatarUrl: String? = null,
    val fromType: Int = 0,
) {
    val isEmpty: Boolean get() = plans.isEmpty()

    fun defaultPlan(): VipGuidePlan? {
        if (plans.isEmpty()) return null
        plans.firstOrNull { plan ->
            plan.label.contains("popular", ignoreCase = true)
        }?.let { return it }
        return plans.getOrNull(plans.size / 2) ?: plans.first()
    }
}

data class VipGuidePlan(
    val id: Long,
    val sku: String,
    val productType: Int = 2,
    val title: String,
    val month: Int,
    val days: Int,
    val price: String,
    val originalPrice: String?,
    val label: String,
    val giveCoins: Int = 0,
    val matchCount: Int = 0,
    val privileges: List<VipGuidePrivilege> = emptyList(),
)

data class VipGuidePrivilege(
    val id: Int,
    val title: String,
    val iconUrl: String?,
)
