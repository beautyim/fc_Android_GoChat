package com.example.demoproject.ui.foundation

import androidx.compose.ui.unit.dp

object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    /** Online chip / card horizontal gap — 12dp. */
    val chipGap = 12.dp
    /** Gift card grid gap — Figma ~10dp. */
    val giftCardGap = 10.dp
    /** Settings list row gap — Figma 10dp. */
    val settingsRowGap = 10.dp
    /** Settings "To Bind" vertical padding — Figma 5dp. */
    val settingsBindVertical = 5.dp
    /** Store coin grid column gap — Figma 7dp. */
    val storeCoinColumnGap = 7.dp
    /** Store coin grid row gap — Figma 12dp. */
    val storeCoinRowGap = 12.dp
    /** Store nav → VIP card — Figma 12dp (y 109 − 97). */
    val storeNavToVip = 12.dp
    /** Store VIP card → pager dots — Figma 10dp (y 265 − 255). */
    val storeVipToPager = 10.dp
    /** Store promo carousel page gap while swiping — 10dp. */
    val storePromoPageGap = 10.dp
    /**
     * Extra top padding on "Choose coins" so nav rhythm matches Figma:
     * pager → title ≈ 22dp; LazyVerticalGrid already inserts [storeCoinRowGap].
     */
    val storePagerToSectionExtra = 10.dp
    /** Store balance pill item gap — Figma 1dp. */
    val storeBalanceGap = 1.dp
    /** Chat emoji grid gap / inset — Figma ~18dp. */
    val chatEmojiGap = 18.dp
    /** VIP page content inset — Figma 13dp. */
    val vipContentInset = 13.dp
    /** VIP nav → hero (non-member) — Figma 56dp. */
    val vipHeroTop = 56.dp
    /** VIP nav → hero (member) — Figma 46dp. */
    val vipHeroTopMember = 46.dp
    /** VIP hero → benefits (non-member) — Figma 53dp. */
    val vipHeroToBenefits = 53.dp
    /** VIP expiry → benefits — Figma 35dp. */
    val vipExpiryToBenefits = 35.dp
    /** VIP benefits → "Choose Your Plan" — Figma 20dp. */
    val vipBenefitsToSection = 20.dp
    /** VIP section title → plan cards — Figma 12dp. */
    val vipSectionToPlans = 12.dp
    /** VIP plan card column gap — Figma 16dp. */
    val vipPlanGap = 16.dp
    /** VIP benefit card horizontal inset — Figma 20dp. */
    val vipBenefitCardInset = 20.dp
    /** VIP benefit card top inset — Figma 12dp. */
    val vipBenefitCardTop = 12.dp
    /** VIP benefit card bottom inset — Figma 20dp (136 − 12 − 16 − 16 − 72). */
    val vipBenefitCardBottom = 20.dp
    /** VIP benefit title → icons — Figma 16dp. */
    val vipBenefitTitleToItems = 16.dp
    /** VIP benefit star ↔ title — Figma 5dp. */
    val vipBenefitStarGap = 5.dp
    /** VIP benefit icon → label — Figma 8dp. */
    val vipBenefitIconToLabel = 8.dp
    /** VIP benefit divider top inset within items row — Figma 1dp. */
    val vipBenefitDividerTop = 1.dp
    /** VIP plan title top inset — Figma 34dp. */
    val vipPlanTitleTop = 34.dp
    /** VIP plan title → price — Figma 5dp. */
    val vipPlanTitleToPrice = 5.dp
    /** VIP plan price → original — Figma 7dp. */
    val vipPlanPriceToOriginal = 7.dp
    /** VIP plan original → dayDesc / dayDesc → sale — Figma 8dp. */
    val vipPlanTextGap = 8.dp
    /** VIP plan card bottom (unselected sale) — Figma 20dp. */
    val vipPlanCardBottom = 20.dp
    /** VIP plan card bottom (selected sale) — Figma 16dp. */
    val vipPlanCardBottomSelected = 16.dp
    /** VIP hero start extra beyond content inset — Figma 5dp. */
    val vipHeroStartExtra = 5.dp
    /** VIP CTA → billing note — Figma 14dp. */
    val vipCtaToNote = 14.dp
    /** VIP pay-guide sheet horizontal inset — Figma 20dp / 13dp. */
    val vipGuideContentInset = 20.dp
    /** VIP pay-guide title top — Figma 40dp. */
    val vipGuideTitleTop = 40.dp
    /** VIP pay-guide title → subtitle — Figma 12dp (104 − 40 − 52). */
    val vipGuideTitleToSubtitle = 12.dp
    /** VIP pay-guide subtitle → benefits card — Figma 34dp (162 − 128). */
    val vipGuideSubtitleToBenefits = 34.dp
    /** VIP pay-guide benefits → plan card — Figma 12dp (292 − 280). */
    val vipGuideBenefitsToPlan = 12.dp
    /** VIP pay-guide plan → CTA — Figma 27dp (399 − 372). */
    val vipGuidePlanToCta = 27.dp
    /** VIP pay-guide close chip padding — Figma 4dp. */
    val vipGuideClosePadding = 4.dp
    /** VIP pay-guide close top/end inset — Figma 16dp. */
    val vipGuideCloseInset = 16.dp
    /** VIP pay-guide plan card horizontal inset — Figma 13dp ((375−349)/2). */
    val vipGuidePlanInset = 13.dp
    /** VIP pay-guide plan icon → text — Figma 10dp. */
    val vipGuidePlanIconGap = 10.dp
    /** VIP pay-guide plan title → subtitle — Figma 4dp. */
    val vipGuidePlanTextGap = 4.dp
    /** VIP pay-guide CTA bottom inset — Figma 16dp above home indicator. */
    val vipGuideCtaBottom = 16.dp
    /** Coin pay-guide balance row top — Figma ~52dp under wavy header. */
    val coinGuideBalanceTop = 52.dp
    /** Coin pay-guide balance → low-balance hint — Figma 12dp. */
    val coinGuideBalanceToHint = 12.dp
    /** Coin pay-guide hint → sale carousel — Figma 11dp. */
    val coinGuideHintToSale = 11.dp
    /** Coin pay-guide horizontal inset for title / hint — Figma 20dp. */
    val coinGuideTextInset = 20.dp
    /** Coin pay-guide close chip top/end — Figma 24×16dp. */
    val coinGuideCloseTop = 24.dp
    val coinGuideCloseEnd = 16.dp
    /** Coin pay-guide balance ↔ coin icon — Figma ~4dp. */
    val coinGuideBalanceIconGap = 4.dp
    /** Coin pay-guide bottom content inset. */
    val coinGuideBottom = 16.dp
    /** Blocked users nav → first row — Figma 20dp (y 117 − 97). */
    val blockedListTop = 20.dp
    /** Blocked users row horizontal inset — Figma 10dp. */
    val blockedRowInset = 10.dp
    /** Blocked users name → country line — Figma 3dp. */
    val blockedNameGap = 3.dp
    /** Blocked users "Unlock" vertical padding — Figma 6dp. */
    val blockedActionVertical = 6.dp
    /** Blocked users artwork → "No users" label box — Figma 26dp (y 453 − 427). */
    val blockedEmptyGap = 26.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 28.dp
}
