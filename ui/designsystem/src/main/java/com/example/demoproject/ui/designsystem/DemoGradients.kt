package com.example.demoproject.ui.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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
    /** In-call balance floating window — Figma 1:4841. */
    val callBalanceFloatCard: Brush = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to DemoColors.callBalanceFloatStart,
            0.55563f to DemoColors.callBalanceFloatMid,
            1f to DemoColors.callBalanceFloatEnd,
        ),
    )
    val callBalanceFloatVipTitle: Brush = Brush.linearGradient(
        colors = listOf(
            DemoColors.callBalanceFloatVipGoldStart,
            DemoColors.callBalanceFloatVipGoldEnd,
        ),
    )
    val callBalanceFloatProduct: Brush = Brush.horizontalGradient(
        colors = listOf(
            DemoColors.callBalanceFloatProductStart,
            DemoColors.callBalanceFloatProductMid,
            DemoColors.callBalanceFloatProductEnd,
        ),
    )
    val callBalanceFloatOffPercent: Brush = Brush.verticalGradient(
        colors = listOf(
            DemoColors.callBalanceFloatOffPercentStart,
            DemoColors.callBalanceFloatOffPercentEnd,
        ),
    )
    /** Match-call "Match!" title — Figma 1:1529. */
    val callConnectingTitle: Brush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.32145f to DemoColors.callConnectingTitleStart,
            0.54469f to DemoColors.callConnectingTitleMiddle,
            0.74292f to DemoColors.callConnectingTitleEnd,
        ),
    )
    /** Match-call avatar lower fade — transparent to black over the final 100dp. */
    val callConnectingAvatarScrim: Brush = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black),
    )

    /**
     * Matching-in-progress rotating arc stroke — Figma 172:3630 paints the
     * gradient along the arc's own vertical axis, so it must be applied inside
     * the arc's square bounding box rather than the whole screen.
     */
    val matchSearchingArc: Brush = Brush.verticalGradient(
        colors = listOf(DemoColors.matchSearchingArcStart, DemoColors.matchSearchingArcEnd),
    )
}
