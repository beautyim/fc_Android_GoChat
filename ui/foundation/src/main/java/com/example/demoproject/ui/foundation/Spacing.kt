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
    /** Report card / section gaps — Figma 20dp. */
    val reportSectionGap = 20.dp
    /** Report reason list item gap — Figma 8dp. */
    val reportReasonGap = 8.dp
    /** Report reason row icon→label — Figma 8dp. */
    val reportReasonIconGap = 8.dp
    /** Report title→subtitle / section label→hint — Figma 6dp. */
    val reportLabelGap = 6.dp
    /** Report avatar→identity — Figma 13dp. */
    val reportAvatarGap = 13.dp
    /** Report card horizontal inset — Figma 12dp. */
    val reportCardInset = 12.dp
    /** Report card vertical inset — Figma 8dp. */
    val reportCardVertical = 8.dp
    /** Report reason row horizontal padding — Figma 8dp. */
    val reportReasonPadH = 8.dp
    /** Report reason row vertical padding — Figma 12dp. */
    val reportReasonPadV = 12.dp
    /** Report submit bottom inset above home indicator — Figma ~12dp. */
    val reportSubmitBottom = 12.dp
    /** Report screenshot remove chip inset — Figma 4dp. */
    val reportShotRemoveInset = 4.dp
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
    /** Bind/change-email hero top under nav — Figma y 117 − 97 = 20. */
    val emailHeroTop = 20.dp
    /** Bind-email hero → title — Figma 280 − (117+147) = 16. */
    val emailHeroToTitle = 16.dp
    /** Change-email hero → title — Figma 280 − (124+134) = 22. */
    val emailChangeHeroToTitle = 22.dp
    /** Bind/change-email title → subtitle — Figma 307 − (280+19) = 8. */
    val emailTitleToSubtitle = 8.dp
    /** Bind/change-email subtitle → form — Figma 392 − (307+32) = 53. */
    val emailSubtitleToForm = 53.dp
    /** Email form field-group gap — Figma 99 − 71 = 28. */
    val emailFieldGroupGap = 28.dp
    /** Email form label → field — Figma 29 − 17 = 12. */
    val emailLabelGap = 12.dp
    /** Email form field padding — Figma icon inset 13. */
    val emailFieldPadding = 13.dp
    /** Email form icon → text — Figma 41 − (13+16) = 12. */
    val emailFieldIconGap = 12.dp
    /** Email CTA bottom inset — Figma 778 − (701+51) = 26. */
    val emailCtaBottom = 26.dp
    /** Bind-email form → CTA — Figma 701 − (392+269) = 40. */
    val emailFormToCta = 40.dp
    /** Change-email form → CTA — Figma 711 − (590+71) = 50. */
    val emailChangeFormToCta = 50.dp
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
    /** Video show user chip → report min gap — 30dp. */
    val videoShowUserToReport = 30.dp
    /** Call ringing header start inset — Figma 12dp. */
    val callRingingHeaderStart = 12.dp
    /** Call ringing avatar → name — Figma 8dp. */
    val callRingingAvatarGap = 8.dp
    /** Call ringing name → status — Figma ~4dp. */
    val callRingingStatusGap = 4.dp
    /** Call ringing action → label — Figma 8dp. */
    val callRingingActionLabelGap = 8.dp
    /** Call ringing bottom actions inset above nav bar — Figma ~37dp. */
    val callRingingBottomInset = 37.dp
    /** Call ringing report end inset — Figma 15dp (375-324-36). */
    val callRingingReportEnd = 15.dp
    /** Match-call connecting top close inset — Figma x=16, y=60 after status bar. */
    val callConnectingCloseHorizontal = 16.dp
    val callConnectingCloseTop = 16.dp
    /** Match-call title block spacing and avatar separation — Figma 1:1520. */
    val callConnectingHeroTop = 62.dp
    val callConnectingSubtitleTop = 4.dp
    val callConnectingAvatarTop = 18.dp
    val callConnectingStatusTop = 17.dp
    /** Match-call notice sits 29dp above the navigation-bar safe area. */
    val callConnectingNoticeBottom = 29.dp
    val callConnectingNoticeHorizontal = 12.dp
    val callConnectingNoticeVertical = 8.dp
    /** In-call header end controls gap. */
    val callInCallHeaderEndGap = 8.dp
    /** In-call name → Like chip — Figma 8dp. */
    val callInCallLikeGap = 8.dp
    /** In-call bottom bar horizontal — Figma ~(375-353)/2 ≈ 11dp. */
    val callInCallBottomBarHorizontal = 11.dp
    /** In-call composer ↔ side buttons — Figma 8dp. */
    val callInCallBottomBarGap = 8.dp
    /** Match-call Next pill horizontal content — Figma 10dp enabled / 8dp countdown. */
    val callMatchNextHorizontal = 10.dp
    val callMatchNextCountdownHorizontal = 8.dp
    /** In-call chat column start — Figma 8dp. */
    val callInCallChatStart = 8.dp
    /** In-call chat bubble stack gap — Figma 10dp. */
    val callInCallChatGap = 10.dp
    /** In-call gift quick bar above composer — Figma ~4–10dp. */
    val callInCallGiftQuickBottom = 4.dp
    /** In-call gift-sent tip start inset — Figma 10dp. */
    val callInCallGiftSentTipStart = 10.dp
    /** In-call gift-sent tip gap between icon and xN — Figma ~11dp. */
    val callInCallGiftSentTipEndGap = 4.dp
    /** How far the gift icon overhangs the purple bar on the end — Figma ~22dp. */
    val callInCallGiftSentTipIconOverhang = 22.dp
    /** In-call more sheet row vertical — Figma 12dp. */
    val callInCallMoreRowVertical = 12.dp
    /** In-call more sheet top inset before first row — Figma ~28dp. */
    val callInCallMoreTop = 28.dp
    /** Call balance float end inset — Figma right 10dp (375−245−120). */
    val callBalanceFloatEnd = 10.dp
    /** Call balance float above bottom bar — Figma gap above composer. */
    val callBalanceFloatBottom = 12.dp
    /** Call balance float body below timer ribbon — Figma top=8. */
    val callBalanceFloatBodyTop = 8.dp
    /** Call balance float timer content — Figma top=4. */
    val callBalanceFloatTimerContentTop = 4.dp
    /** Call balance float Upgrade VIP — Figma top=28. */
    val callBalanceFloatTitleTop = 28.dp
    /** Call balance float OFF badge — Figma top=57. */
    val callBalanceFloatOffTop = 57.dp
    /** Call balance float product card — Figma top=68. */
    val callBalanceFloatProductTop = 68.dp
    /** OFF overhang above product — Figma 68−57=11dp. */
    val callBalanceFloatOffOverhang = 11.dp
    /** Call balance float CTA — Figma top=128. */
    val callBalanceFloatCtaTop = 128.dp
    /** Product bottom → CTA — Figma 128−(68+54)=6dp. */
    val callBalanceFloatProductToCta = 6.dp
    /** Call balance float More options — Figma text center ≈166 → top≈160. */
    val callBalanceFloatMoreTop = 160.dp
    /** Call balance float horizontal inset for product — Figma 8dp. */
    val callBalanceFloatInset = 8.dp
    /** Call balance float icon ↔ label — Figma 2dp. */
    val callBalanceFloatIconGap = 2.dp
    /** Call balance float CTA price gap — Figma 4dp. */
    val callBalanceFloatPriceGap = 4.dp
    /** Call balance float product coins row center — Figma y=19 → top≈11. */
    val callBalanceFloatCoinsRowTop = 11.dp
    /** Call balance float coin-pack icon top — Figma center ≈19 → top≈9. */
    val callBalanceFloatCoinIconTop = 9.dp
    /** Call balance float coin-pack amount — Figma top=39, raised 10dp for optical balance. */
    val callBalanceFloatCoinAmountTop = 29.dp
    /** Call balance float product match row center — Figma y=40 → top≈32. */
    val callBalanceFloatMatchRowTop = 32.dp
    /** Call balance offer-guide CTA → more options — Figma ~12dp. */
    val callBalanceOfferGuideCtaToMore = 12.dp
    /** Call balance offer-guide more → home indicator — Figma ~16dp. */
    val callBalanceOfferGuideMoreBottom = 16.dp
    /** Call hangup-recharge header top under wavy bg — Figma avatar ≈53dp. */
    val callHangupRechargeHeaderTop = 53.dp
    /** Call hangup avatar → name/speech column — Figma ~19dp (25→119−100). */
    val callHangupAvatarToMeta = 19.dp
    /** Call hangup name → speech bubble — Figma ~8dp. */
    val callHangupNameToSpeech = 8.dp
    /** Call hangup speech horizontal pad — Figma 10dp. */
    val callHangupSpeechHorizontal = 10.dp
    /** Call hangup speech vertical pad — Figma 8dp. */
    val callHangupSpeechVertical = 8.dp
    /** Call hangup header → top-up title — Figma ~20dp. */
    val callHangupHeaderToTitle = 20.dp
    /** Call hangup title → sale carousel — Figma ~12dp. */
    val callHangupTitleToSale = 12.dp
    /** Call hangup continue avatar top under wavy bg — Figma ~23dp. */
    val callHangupContinueAvatarTop = 23.dp
    /** Call hangup continue avatar → name — Figma ~16dp. */
    val callHangupContinueAvatarToName = 16.dp
    /** Call hangup continue name → waiting title — Figma ~12dp. */
    val callHangupContinueNameToTitle = 12.dp
    /** Call hangup continue title → subtitle — Figma ~16dp. */
    val callHangupContinueTitleToSubtitle = 16.dp
    /** Call hangup continue subtitle → primary CTA — Figma ~28dp. */
    val callHangupContinueSubtitleToCta = 28.dp
    /** Call hangup continue primary → secondary CTA — Figma ~12dp. */
    val callHangupContinueCtaGap = 12.dp
    /** Call hangup continue secondary → home indicator — Figma ~16dp. */
    val callHangupContinueBottom = 16.dp
    /** Match hero card horizontal inset — Figma 8dp. */
    val matchHeroInset = 8.dp
    /** Match hero top insets for chips / filter — Figma 12dp. */
    val matchHeroChrome = 12.dp
    /** Match CTA bottom inset above card edge — Figma ~23dp (593−522−48). */
    val matchCtaBottom = 23.dp
    /** Match CTA horizontal inset inside card — Figma ~18dp. */
    val matchCtaHorizontal = 18.dp
    /** Matching radar field side inset — Figma (375−345)/2 = 15dp. */
    val matchSearchingFieldInset = 15.dp
    /** Matching hearts emblem lift above the radar centre — Figma 37.5dp. */
    val matchSearchingEmblemLift = 37.5.dp
    /** Matching caption block drop below the radar centre — Figma 36.5dp. */
    val matchSearchingCaptionDrop = 36.5.dp
    /** Matching title → caption — Figma 209−200 = 9dp. */
    val matchSearchingTitleToCaption = 9.dp
    /** Matching Cancel link above the home indicator — Figma 108.5−34 = 74.5dp. */
    val matchSearchingCancelBottom = 74.5.dp
    /** Treasure SmallCoins glow top — Figma 301−254. */
    val treasureRewardTop = 47.dp
    /** Treasure DualCoins (no bonus) glow top — Figma 311−254. */
    val treasureDualRewardTop = 57.dp
    /** Treasure DualCoinsWithBonus glow top — Figma 282−254. */
    val treasureDualBonusRewardTop = 28.dp
    /** Treasure SmallCoins glow → label — Figma 402−(301+80). */
    val treasureRewardToLabel = 21.dp
    /** Treasure DualCoins glow → label — Figma ≈16dp. */
    val treasureDualRewardToLabel = 16.dp
    /** Treasure content panel horizontal inset — Figma (340−310)/2. */
    val treasureContentInset = 15.dp
    /** Treasure VIP row top inside content — Figma 268−254. */
    val treasureVipRowTop = 14.dp
    /** Treasure VIP title ↔ +coins centers 34 − 24 line = 10dp. */
    val treasureVipTitleGap = 10.dp
    /** Treasure VIP badge row → EXTRA BONUS — Figma 371−348. */
    val treasureVipToBonus = 23.dp
    /** Treasure DualCoinsWithBonus labels → EXTRA BONUS — Figma 420−394. */
    val treasureDualToBonus = 26.dp
    /** Treasure VIP EXTRA BONUS tag lift above panel. */
    val treasureVipExtraTagLift = 5.dp
    /** Prize coin row vertical pad — Figma py ≈4.7. */
    val prizeCoinRowVertical = 5.dp
    /** Prize coin frames gap (after dashed divider) — Figma ≈12. */
    val prizeCoinRowGap = 12.dp
    /** Prize base coin icon → amount — Figma ≈12 (105.8−46−48). */
    val prizeBaseIconToText = 12.dp
    /** Prize bonus coin icon → amount — Figma ≈8 (99.9−48−44). */
    val prizeBonusIconToText = 8.dp
    /** Prize original → sale price — Figma 83−3−57 = 23. */
    val prizePriceGap = 23.dp
    /** Prize bonus row → price strip — Figma 394−391 ≈ 3. */
    val prizeCoinsToPrice = 3.dp
    /** Prize price strip → pink CTA wash — Figma 462.6−458 ≈ 5. */
    val prizePriceToCover = 5.dp
    /** Pink CTA wash lift above its slot — visual QA 25dp. */
    val prizeCtaCoverLift = 25.dp
    /** Pink CTA wash soft rim — 3dp edge fade. */
    val prizeCtaCoverEdgeFade = 3.dp
    /** Real-person verify hero / benefits / steps horizontal inset — Figma 16dp. */
    val verifyContentInset = 16.dp
    /** Real-person verify hero art top under nav — Figma 121 − 97. */
    val verifyHeroArtTop = 24.dp
    /** Real-person verify hero title top under nav — Figma 139 − 97. */
    val verifyHeroTitleTop = 42.dp
    /** Real-person verify hero title → subtitle — Figma 208 − 199. */
    val verifyHeroTitleToSubtitle = 9.dp
    /** Real-person verify subtitle → benefits — Figma 293 − 256. */
    val verifySubtitleToBenefits = 37.dp
    /** Real-person verify benefits → “How it works” — Figma ≈20dp. */
    val verifyBenefitsToHow = 20.dp
    /** Real-person verify how-title → steps — Figma 461 − 449. */
    val verifyHowToSteps = 12.dp
    /** Real-person verify step cards gap — Figma 12dp. */
    val verifyStepGap = 12.dp
    /** Real-person verify benefit / step icon circle pad — Figma 8dp. */
    val verifyIconPad = 8.dp
    /** Real-person verify benefit column / step text gap — Figma 8dp / 4dp. */
    val verifyBenefitTextGap = 8.dp
    val verifyStepTextGap = 4.dp
    /** Real-person verify step row icon → copy — Figma 12dp. */
    val verifyStepIconGap = 12.dp
    /** Real-person verify card padding — Figma 8 / 12. */
    val verifyBenefitCardPadH = 8.dp
    val verifyBenefitCardPadV = 12.dp
    val verifyStepCardPad = 12.dp
    /** Real-person verify CTA bottom inset — Figma 50 − 34 home ≈ 16. */
    val verifyCtaBottom = 16.dp
    /** Real-person verify oval lift above center — Figma top calc(50%−18). */
    val verifyOvalCenterLift = 18.dp
    /** Real-person verify hero text end inset so art can sit on the trailing edge. */
    val verifyHeroTextEndInset = 152.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 28.dp
}
