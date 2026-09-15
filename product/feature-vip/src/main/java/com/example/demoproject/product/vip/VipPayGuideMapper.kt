package com.example.demoproject.product.vip

import android.content.Context
import com.example.demoproject.platform.data.repository.VipGuidePageData
import com.example.demoproject.platform.data.repository.VipGuidePlan

fun VipGuidePageData.toPayGuideUiState(
    context: Context,
    fallbackNickname: String,
): VipPayGuideUiState {
    val plan = defaultPlan()
    return VipPayGuideUiState(
        peerNickname = peerNickname.ifBlank { fallbackNickname },
        benefits = plan?.privileges.orEmpty().map { privilege ->
            VipPayGuideBenefitUi(
                id = privilege.id,
                title = privilege.title,
                iconUrl = privilege.iconUrl,
            )
        },
        plan = plan?.toPayGuidePlanUi(context),
        fromType = fromType,
    )
}

private fun VipGuidePlan.toPayGuidePlanUi(context: Context): VipPayGuidePlanUi {
    val baseTitle = composeGuidePlanBaseTitle(month = month, title = title)
    val displayTitle = if (baseTitle.contains("VIP", ignoreCase = true)) {
        baseTitle
    } else {
        context.getString(R.string.vip_guide_plan_title_fmt, baseTitle)
    }
    return VipPayGuidePlanUi(
        id = id,
        sku = sku,
        productType = productType,
        title = displayTitle,
        price = price,
        originalPrice = originalPrice,
    )
}

/** `"$month $title"` (e.g. `1` + `Month` → `1 Month`). */
private fun composeGuidePlanBaseTitle(month: Int, title: String): String {
    val titlePart = title.trim()
    return when {
        month > 0 && titlePart.isNotBlank() -> {
            val prefix = month.toString()
            if (titlePart == prefix || titlePart.startsWith("$prefix ")) {
                titlePart
            } else {
                "$prefix $titlePart"
            }
        }
        titlePart.isNotBlank() -> titlePart
        month > 0 -> month.toString()
        else -> ""
    }
}
