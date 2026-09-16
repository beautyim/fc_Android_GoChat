package com.example.demoproject.ui.designsystem

import androidx.compose.ui.graphics.Brush

object DemoGradients {
    val primaryButton: Brush = Brush.horizontalGradient(
        colors = listOf(DemoColors.gradientStart, DemoColors.gradientEnd),
    )
    /** Video show Video Chat CTA — Figma #00E384 → #00D7E6. */
    val videoShowCta: Brush = Brush.horizontalGradient(
        colors = listOf(DemoColors.videoShowCtaStart, DemoColors.videoShowCtaEnd),
    )
    val storeVipCard: Brush = Brush.horizontalGradient(
        colors = listOf(
            DemoColors.storeVipCardStart,
            DemoColors.storeVipCardMid,
            DemoColors.storeVipCardEnd,
        ),
    )
    val storeSaleCard: Brush = Brush.horizontalGradient(
        colors = listOf(
            DemoColors.storeSaleCardStart,
            DemoColors.storeSaleCardMid,
            DemoColors.storeSaleCardEnd,
        ),
    )
    val storeVipRibbon: Brush = Brush.linearGradient(
        colors = listOf(DemoColors.storeVipRibbonStart, DemoColors.storeVipRibbonEnd),
    )
    val storeSaleRibbon: Brush = Brush.linearGradient(
        colors = listOf(DemoColors.storeSaleRibbonStart, DemoColors.storeSaleRibbonEnd),
    )
    val vipPlanBadgeMuted: Brush = Brush.horizontalGradient(
        colors = listOf(DemoColors.vipPlanBadgeStart, DemoColors.vipPlanBadgeEnd),
    )
    val vipPlanBadgePopular: Brush = Brush.horizontalGradient(
        colors = listOf(DemoColors.vipPlanPopularStart, DemoColors.vipPlanPopularEnd),
    )
    val vipPlanDiscountHot: Brush = Brush.horizontalGradient(
        colors = listOf(DemoColors.vipPlanDiscountStart, DemoColors.vipPlanDiscountEnd),
    )
    val vipGuidePlanCard: Brush = Brush.horizontalGradient(
        colors = listOf(
            DemoColors.vipGuidePlanStart,
            DemoColors.vipGuidePlanMid,
            DemoColors.vipGuidePlanEnd,
        ),
    )
}
