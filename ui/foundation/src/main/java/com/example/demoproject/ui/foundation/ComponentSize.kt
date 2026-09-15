package com.example.demoproject.ui.foundation

import androidx.compose.ui.unit.dp

object ComponentSize {
    val pillButton = 60.dp
    val pillButtonCompact = 51.dp
    val authInput = 53.dp
    val dialogAction = 39.dp
    /** Figma confirm dialog — 315×178. */
    val confirmDialogWidth = 315.dp
    val confirmDialogMinHeight = 178.dp
    /** Figma delete-conversation confirm — 315×264 (80:3770). */
    val confirmDialogDestructiveMinHeight = 264.dp
    /** Figma delete-conversation illustration — 62dp. */
    val confirmDialogIllustration = 62.dp
    val confirmDialogContentWidth = 275.dp
    val confirmDialogTextWidth = 237.dp
    /** About Us account-delete dialog copy spans the full content column. */
    val confirmDialogDeleteTextWidth = 275.dp
    val confirmDialogActionWidth = 128.dp
    val confirmDialogHorizontalInset = 20.dp
    val confirmDialogActionsGap = 19.dp
    val onlineCardMinWidth = 160.dp
    /** Figma card 170×282. */
    val onlineCardAspect = 170f / 282f
    val onlineAction = 58.dp
    /** Figma: action sits 8dp above card bottom (top 216 in 282). */
    val onlineActionBottomInset = 8.dp
    /** Figma top scrim height. */
    val onlineTopGradient = 78.dp
    /** Figma Online card header avatar — 34dp circle. */
    val onlineCardAvatar = 34.dp
    /**
     * FREE badge lift from card bottom so it overlaps the action top
     * (Figma FREE top ≈ 13dp above the 58dp button).
     */
    val onlineFreeBadgeLift = 49.dp
    val onlineCardShadow = 6.dp
    val onlineCoinIcon = 20.dp
    val onlineChipHeight = 32.dp
    val onlineSkeletonNameWidth = 96.dp
    val onlineSkeletonNameHeight = 14.dp
    val onlineSkeletonStatusWidth = 56.dp
    val onlineSkeletonStatusHeight = 10.dp
    /**
     * Figma Rectangle 2 / 1:495 export with baked purple top shadow —
     * 1125×266 @3x ≈ 375×88.67dp; outside the arc is transparent.
     */
    val onlineTabBarAspect = 1125f / 266f
    /** Shadow pad below white bar in Rectangle 2 export (~9dp). */
    val onlineTabBarShadowBottom = 9.dp
    /** Usable white body below flat top (76 - 20.64). */
    val onlineTabBarBodyHeight = 55.36.dp
    /** Figma Frame 14 icon row height. */
    val onlineTabRowHeight = 68.dp
    /** Figma Frame 13 — match tab outer circle (4+44+4). */
    val onlineMatchTab = 52.dp
    /** Figma match glow blur radius — 7.4px. */
    val onlineMatchGlowBlur = 7.4.dp
    /** Figma profile banner — 195dp. */
    val profileBanner = 195.dp
    /** Figma banner→page fade — 50dp. */
    val profileBannerFade = 50.dp
    /** Figma profile top chrome — 53dp. */
    val profileTopBar = 53.dp
    /** Figma nav circle (back / more) — 28dp. */
    val profileNavButton = 28.dp
    /** Figma avatar — 100dp. */
    val profileAvatar = 100.dp
    /** Figma avatar white ring. */
    val profileAvatarBorder = 2.dp
    /** Figma photo cell 109×150. */
    val profilePhotoAspect = 109f / 150f
    /** Figma bottom gift / message circle — 58dp. */
    val profileActionCircle = 58.dp
    /** Figma Video Chat / Follow compact height. */
    val profileFollowButton = 36.dp
    /** Follow sits 20dp below the nickname top (requested vs name row). */
    val profileFollowTopInset = 20.dp
    /** Identity block (name / country / stats) drop below avatar top. */
    val profileIdentityTopInset = 20.dp
    /** Reserve for Follow / Following pill so nickname does not draw underneath. */
    val profileFollowReserve = 96.dp
    val profileVideoChatButton = 58.dp
    /** Figma media viewer close hit — 44dp. */
    val profileMediaClose = 44.dp
    /** Figma media viewer close glyph — ~30dp. */
    val profileMediaCloseIcon = 30.dp
    /** Figma media viewer CTA — 52dp. */
    val profileMediaCta = 52.dp
    /** Figma media frame — square 376 on 375 artboard. */
    val profileMediaAspect = 1f
    /** Figma video scrubber track — 4dp. */
    val profileMediaProgressTrack = 4.dp
    /** Figma video scrubber knob — 12dp. */
    val profileMediaProgressKnob = 12.dp
    /** Figma video play/pause glyph — 16×21. */
    val profileMediaPlayWidth = 16.dp
    val profileMediaPlayHeight = 21.dp
    /** Figma video bottom controls fade — 62dp. */
    val profileMediaControlsScrim = 62.dp
    /** Figma avatar hangs below banner — (137+100)-195. */
    val profileAvatarHang = 42.dp
    /**
     * Figma identity line gaps (centers 170→199→222 with 22/16/12 line boxes):
     * name→country 10dp, country→stats 9dp.
     */
    val profileIdentityGapNameToCountry = 10.dp
    val profileIdentityGapCountryToStats = 9.dp
    /** Bio → Translate / Recovery control. Figma 1:913: 4dp after last bio line. */
    val profileBioToTranslate = 4.dp
    /** Figma 1:915 Recovery / Translate chevron — 14dp. */
    val profileBioToggleIcon = 14.dp
    /** Gap between original bio and translated paragraph (Figma 1:906→1:907). */
    val profileBioParagraphGap = 4.dp
    /** Space above bio after identity block (16 - 8 raise). */
    val profileBioTopInset = 8.dp
    /** Bottom action row side inset. */
    val profileBottomBarSide = 16.dp
    /** Gift / message circle stroke. */
    val profileActionStroke = 1.5.dp
    val profileProgressStroke = 2.dp
    /** Profile skeleton identity bars. */
    val profileSkeletonNameWidth = 140.dp
    val profileSkeletonNameHeight = 22.dp
    val profileSkeletonCountryWidth = 88.dp
    val profileSkeletonCountryHeight = 12.dp
    val profileSkeletonStatWidth = 120.dp
    val profileSkeletonStatHeight = 12.dp
    val profileSkeletonBioLineHeight = 12.dp
    val profileSkeletonPhotosTitleWidth = 72.dp
    val profileSkeletonPhotosTitleHeight = 14.dp
    /** Figma gift card image — 72dp. */
    val giftCardImage = 72.dp
    /** Figma gift card coin next to price. */
    val giftCardCoin = 12.dp
    /** Figma gift balance pill coin / chevron. */
    val giftBalanceIcon = 16.dp
    /** Figma gift header inline avatar. */
    val giftHeaderAvatar = 16.dp
    /** Figma gift pager track. */
    val giftPagerTrackWidth = 100.dp
    val giftPagerTrackHeight = 4.dp
    val giftPagerThumbWidth = 13.dp
    /** Figma gift bottom actions height. */
    val giftActionHeight = 51.dp
    /** Gifts per pager page (2 rows × 4 cols). */
    const val giftPageSize = 8
    const val giftPageColumns = 4
    /** iOS action sheet row height. */
    val actionSheetRow = 52.dp
    /** Figma Call Records list avatar — 60dp. */
    val callRecordsAvatar = 60.dp
    /** Figma Call Records row video action — 28dp. */
    val callRecordsAction = 28.dp
    /** Figma Call Records status camera icon — 12dp. */
    val callRecordsStatusIcon = 12.dp
    /** Figma Call Records empty illustration — 220×176.063. */
    val callRecordsEmptyWidth = 220.dp
    val callRecordsEmptyHeight = 176.dp
    /** Figma profile blocked-by-peer illustration — 200×171.766. */
    val profileBlockedEmptyWidth = 200.dp
    val profileBlockedEmptyHeight = 172.dp
    /** Space above blocked empty block after profile header (Figma ~70). */
    val profileBlockedEmptyTopInset = 70.dp
    /** Figma segmented control height. */
    val callRecordsSegmentHeight = 32.dp
    /** Call Records list row min height (avatar + vertical padding). */
    val callRecordsRowMin = 76.dp
    /** Call Records skeleton name / status bars. */
    val callRecordsSkeletonNameWidth = 120.dp
    val callRecordsSkeletonNameHeight = 14.dp
    val callRecordsSkeletonStatusWidth = 140.dp
    val callRecordsSkeletonStatusHeight = 11.dp
    /** Empty-state primary CTA height. */
    val callRecordsCtaHeight = 52.dp
    /** Figma Me avatar — 90dp. */
    val meAvatar = 90.dp
    /** Figma Me avatar ring stroke. */
    val meAvatarBorder = 1.8.dp
    /** Figma Me camera badge outer — 5.6+16.8+5.6. */
    val meCameraBadge = 28.dp
    /** Figma Me camera glyph — 16.8dp. */
    val meCameraIcon = 16.8.dp
    /** Figma Me gender mark — 14dp. */
    val meGenderIcon = 14.dp
    /** Figma Me VIP badge — 60dp. */
    val meVipBadge = 60.dp
    /** Figma Me VIP card height — 78dp. */
    val meVipCardHeight = 78.dp
    /** Figma Me balance coin — 22dp. */
    val meBalanceCoin = 22.dp
    /** Figma Me Add Coins button — 124×37. */
    val meAddCoinsWidth = 124.dp
    val meAddCoinsHeight = 37.dp
    /** Figma Me Add Coins plus — 16dp. */
    val meAddCoinsPlus = 16.dp
    /** Figma Me menu leading icon circle — 8+20+8. */
    val meMenuIconCircle = 36.dp
    /** Settings row icon circle — Figma 7+16+7. */
    val settingsIconCircle = 30.dp
    /** Following / Followers list top bar — Figma 53dp. */
    val relationshipTopBar = 53.dp
    /** Following / Followers card — 60dp avatar + 8dp vertical padding. */
    val relationshipRowMinHeight = 76.dp
    /** Following / Followers avatar — Figma 60dp. */
    val relationshipAvatar = 60.dp
    /** Following / Followers action gradient circle — Figma 40dp. */
    val relationshipAction = 40.dp
    /** Downloaded chat/video leaf geometry: 58px @3x. */
    val relationshipActionIcon = 19.33.dp
    /** Following / Followers gender glyph — Figma 14dp. */
    val relationshipGenderIcon = 14.dp
    /** Following / Followers status dot — Figma 6dp. */
    val relationshipStatusDot = 6.dp
    /** Following / Followers back icon — Figma 24dp. */
    val relationshipBackIcon = 24.dp
    /** Blocked users row avatar — Figma 50dp. */
    val blockedAvatar = 50.dp
    /** Blocked users card — 50dp avatar + 8dp vertical padding. */
    val blockedRowMinHeight = 66.dp
    /**
     * Blocked users empty artwork. The exported 250dp frame is mostly transparent;
     * these are the artwork bounds so the gap to the label stays on-spec.
     */
    val blockedEmptyIllustrationWidth = 200.dp
    val blockedEmptyIllustrationHeight = 160.dp
    /** Blocked users Unlock pill skeleton — Figma 12+~48+12 × 14+6+6. */
    val blockedSkeletonUnlockWidth = 72.dp
    val blockedSkeletonUnlockHeight = 26.dp
    const val blockedSkeletonCount = 8
    /**
     * Figma tab title section height — 53dp (Online / Messages / Call / Me).
     * Title is vertically centered in this band below the status bar.
     */
    val navHeaderHeight = 53.dp
    /** Alias for [navHeaderHeight] (Me top bar). */
    val meTopBar = navHeaderHeight
    /** Chat unread badge min size. */
    val meUnreadBadgeMin = 20.dp
    /** Me card hairline stroke. */
    val meCardStroke = 1.dp
    /** Me identity: nickname / gender nudge down from prior layout. */
    val meIdentityNameDown = 10.dp
    /** Me identity: followers / following nudge up from prior layout. */
    val meIdentityStatsUp = 10.dp
    /** Me skeleton identity bars. */
    val meSkeletonNameWidth = 140.dp
    val meSkeletonNameHeight = 17.dp
    val meSkeletonCountryWidth = 96.dp
    val meSkeletonCountryHeight = 11.dp
    val meSkeletonStatWidth = 160.dp
    val meSkeletonStatHeight = 13.dp
    val meSkeletonBalanceLabelWidth = 72.dp
    val meSkeletonBalanceLabelHeight = 12.dp
    val meSkeletonBalanceValueWidth = 88.dp
    val meSkeletonBalanceValueHeight = 20.dp
    val meSkeletonMenuTitleWidth = 120.dp
    val meSkeletonMenuTitleHeight = 18.dp
    /** Figma Messages empty illustration — 258×258 (1:1). */
    val chatEmptyIll = 258.dp
    /** Figma Messages notification bell circle — 28dp. */
    val chatNotifBell = 28.dp
    /** Figma Messages swipe action column — 48dp each. */
    val chatSwipeAction = 48.dp
    /** Figma Messages pin indicator next to timestamp — 12dp. */
    val chatPinnedIcon = 12.dp
    /** Figma Messages list row height. */
    val chatRowHeight = 68.dp
    /** Figma Messages list horizontal inset — 12dp. */
    val chatListHorizontalInset = 12.dp
    /** Gap between header bottom and first row. */
    val chatListTopInset = 12.dp
    /** Figma Messages name→avatar gap (72-60). */
    val chatRowTextStartGap = 12.dp
    /** Figma name box top — y=11 (1:1875). */
    val chatRowTextTopInset = 11.dp
    /** Figma name→preview gap — 36 − (11+17) = 8 (1:1875/1:1876). */
    val chatRowNamePreviewGap = 8.dp
    /** Figma timestamp box top — y=9 (1:1877). */
    val chatRowMetaTopInset = 9.dp
    /** Figma timestamp→badge gap — 32 − (9+13) = 10 (1:1877/1:1878). */
    val chatRowMetaGap = 10.dp
    /** Figma unread badge horizontal padding — 6dp. */
    val chatUnreadBadgeHorizontal = 6.dp
    /** Figma unread badge vertical padding — 3dp. */
    val chatUnreadBadgeVertical = 3.dp
    /** Figma Messages unread badge min — 19×19 (1:1878). */
    val chatUnreadBadgeMin = 19.dp
    /**
     * Figma unread badge trailing inset vs row end —
     * frame 351 − (badge x 320 + w 19) = 12 (1:1878).
     * Timestamp stays flush end; badge sits 12dp inset.
     */
    val chatUnreadBadgeEndInset = 12.dp
    /** Chat skeleton name / preview bars (match Figma text box heights). */
    val chatSkeletonNameWidth = 120.dp
    val chatSkeletonNameHeight = 17.dp
    val chatSkeletonPreviewWidth = 180.dp
    val chatSkeletonPreviewHeight = 14.dp
    /** Chat detail top bar — 56dp. */
    val chatDetailTopBar = 56.dp
    /** Figma chat detail top bar: back → avatar gap — 12dp. */
    val chatDetailTopBarBackGap = 12.dp
    /** Figma chat detail top bar: avatar → title gap — 8dp. */
    val chatDetailTopBarAvatarGap = 8.dp
    /** Figma chat detail top bar: nickname ↔ online row gap — 4dp. */
    val chatDetailTopBarTitleGap = 4.dp
    /** Figma chat detail top bar: online dot → label gap — 2dp. */
    val chatDetailTopBarOnlineGap = 2.dp
    /** Figma chat detail top bar: follow → more gap — 20dp. */
    val chatDetailTopBarActionGap = 20.dp
    /** Chat detail header avatar — 36dp. */
    val chatDetailHeaderAvatar = 36.dp
    /** Chat detail bubble avatar — 28dp. */
    val chatDetailBubbleAvatar = 28.dp
    /** Chat detail composer control — 32dp. */
    val chatDetailComposerControl = 32.dp
    /** Chat detail composer field height — 32dp. */
    val chatDetailComposerField = 32.dp
    /** Chat detail composer bar height — 46dp. */
    val chatDetailComposerBar = 46.dp
    /** Chat detail media thumb — 146×196. */
    val chatDetailMediaWidth = 146.dp
    val chatDetailMediaHeight = 196.dp
    /** Chat detail gift card — 240×110. */
    val chatDetailGiftWidth = 240.dp
    val chatDetailGiftHeight = 110.dp
    /** Chat detail gift icon — 60dp. */
    val chatDetailGiftIcon = 60.dp
    /** Figma first-visit gift quick bar icon — 30dp. */
    val chatGiftQuickIcon = 30.dp
    /** Figma first-visit gift quick bar coin — 12dp. */
    val chatGiftQuickCoin = 12.dp
    /** Figma first-visit greeting wave — 120×116. */
    val chatGreetingWaveWidth = 120.dp
    val chatGreetingWaveHeight = 116.dp
    /** Gap from gift quick bar top to greeting wave bottom — 24dp. */
    val chatGreetingAboveGiftBar = 24.dp
    /** Chat detail profile card avatar — 42dp. */
    val chatDetailProfileAvatar = 42.dp
    /** Chat detail profile photo cell — 72×97. */
    val chatDetailProfilePhotoWidth = 72.dp
    val chatDetailProfilePhotoHeight = 97.dp
    /** Chat detail translate chip — 12+8. */
    val chatDetailTranslateIcon = 12.dp
    /** Chat detail online status dot — 4dp. */
    val chatDetailOnlineDot = 4.dp
    /** Figma chat detail message list item gap — 12dp (1:1958). */
    val chatDetailMessageGap = 12.dp
    /** Figma bubble → send-time gap — 4dp (1:1959). */
    val chatDetailBubbleTimeGap = 4.dp
    /**
     * Figma time-separator → first message gap is 16dp (172:3649), while list
     * item gap stays 12dp; extra bottom padding on the separator makes up 4dp.
     */
    val chatDetailTimeSeparatorExtraGap = 4.dp
    /** Figma incoming send-time leading inset under avatar+gap — 42dp (1:1964). */
    val chatDetailTimeStampIndent = 42.dp
    /** Chat emoji panel height — Figma 176:5014 ~257dp. */
    val chatEmojiPanelHeight = 257.dp
    /** Chat emoji glyph cell — taller than glyph so system emoji is not clipped. */
    val chatEmojiCell = 32.dp
    /** Chat emoji panel top hairline. */
    val chatEmojiPanelHairline = 1.dp
    /** Chat emoji grid columns — Figma 8. */
    const val chatEmojiColumns = 8
    /** Server page size for chat history. */
    const val chatDetailPageSize = 30
    /** Store back / nav icon — 24dp. */
    val storeNavIcon = 24.dp
    /** Store balance pill coin — 20dp. */
    val storeBalanceCoin = 20.dp
    /** Store balance pill chevron — 16dp. */
    val storeBalanceChevron = 16.dp
    /** Store VIP promo card height — 146dp. */
    val storeVipCardHeight = 146.dp
    /** Store sale promo card height — same as VIP, Figma 146dp. */
    val storeSaleCardHeight = 146.dp
    /** Store VIP badge art — 100dp. */
    val storeVipBadge = 100.dp
    /** Store sale gift art — Figma 100dp. */
    val storeSaleGift = 100.dp
    /** Store VIP badge start inset — Figma 4dp. */
    val storeVipBadgeStart = 4.dp
    /** Store sale gift start inset — Figma 4dp. */
    val storeSaleGiftStart = 4.dp
    /** Store VIP badge top inset — Figma 33dp. */
    val storeVipBadgeTop = 33.dp
    /** Store sale gift top inset — Figma 33dp. */
    val storeSaleGiftTop = 33.dp
    /** Store ribbon inset from card edge so it sits inside the 1dp border — Figma x/y = 1. */
    val storeRibbonInset = 1.dp
    /** Store VIP title top inset — Figma y=20. */
    val storeVipContentTop = 20.dp
    /** Store sale title top inset — Figma title center ≈48.5 → top ≈39. */
    val storeSaleContentTop = 39.dp
    /**
     * Benefits/price cluster start — Figma x=121
     * (badge 4+100, then 17dp gap to coin icon / dashed line).
     */
    val storeVipContentStart = 121.dp
    /** Store sale content start — Figma title/Extra x≈119. */
    val storeSaleContentStart = 119.dp
    /** Store VIP content end inset — keep room for match label on narrow widths. */
    val storeVipContentEnd = 16.dp
    /** Store sale content end inset — leave room for OFF ribbon. */
    val storeSaleContentEnd = 40.dp
    /** Store VIP title → benefits — Figma 54−(20+21) = 13dp. */
    val storeVipTitleToBenefits = 13.dp
    /** Store sale title → Extra row — Figma ≈12dp. */
    val storeSaleTitleToExtra = 12.dp
    /** Store VIP benefits → dashed divider — Figma 99−(54+34) = 11dp. */
    val storeVipBenefitsToDivider = 11.dp
    /** Store sale Extra → dashed divider — Figma ≈11dp. */
    val storeSaleExtraToDivider = 11.dp
    /** Store VIP dashed → price — Figma 110−99 = 11dp. */
    val storeVipDividerToPrice = 11.dp
    /** Store sale dashed → price — Figma ≈11dp. */
    val storeSaleDividerToPrice = 11.dp
    /** Store VIP benefit icon — 32dp. */
    val storeVipBenefitIcon = 32.dp
    /** Store sale Extra coin icon — Figma 24dp. */
    val storeSaleExtraCoin = 24.dp
    /** Store VIP benefit icon → label — Figma ≈6dp. */
    val storeVipIconToLabel = 6.dp
    /** Store sale Extra label → coin — Figma ≈6dp. */
    val storeSaleExtraLabelToCoin = 6.dp
    /** Store sale coin → bonus text — Figma ≈4dp. */
    val storeSaleExtraCoinToText = 4.dp
    /** Gap around benefits vertical divider — Figma ≈12dp. */
    val storeVipBenefitSectionGap = 12.dp
    /** Current ↔ original price gap — Figma 216−(140+56) = 20dp. */
    val storeVipPriceGap = 20.dp
    /** Store sale current ↔ original price gap — Figma ≈20dp. */
    val storeSalePriceGap = 20.dp
    /** Store VIP benefits vertical divider height — Figma 33dp. */
    val storeVipBenefitDividerHeight = 33.dp
    /** Store VIP original-price strike width — Figma ≈43dp. */
    val storeVipStrikeWidth = 43.dp
    /** Store sale original-price strike width — Figma ≈43dp. */
    val storeSaleStrikeWidth = 43.dp
    /** Store VIP ribbon height — 29dp. */
    val storeRibbonHeight = 29.dp
    /** Store sale % OFF ribbon — Figma 33×45. */
    val storeSaleOffRibbonWidth = 33.dp
    val storeSaleOffRibbonHeight = 45.dp
    /** Store sale % OFF ribbon end inset — Figma 343−(289+33) = 21dp. */
    val storeSaleOffRibbonEnd = 21.dp
    /** Store sale OFF label top inset inside ribbon — Figma center y=30 → top ≈24. */
    val storeSaleOffLabelTop = 24.dp
    /** Store VIP dashed divider stroke. */
    val storeDashedStroke = 1.dp
    /** Store pager active pill — 24×8. */
    val storePagerActiveWidth = 24.dp
    val storePagerDot = 8.dp
    /** Store coin package icon — 40dp. */
    val storeCoinIcon = 40.dp
    /** Store coin card with Super Discounts ribbon — Figma 197dp. */
    val storeCoinCardHeightWithBadge = 197.dp
    /** Store coin card without ribbon — Figma 171dp. */
    val storeCoinCardHeight = 171.dp
    /** Icon top inset with ribbon — Figma y=40. */
    val storeCoinIconTopWithBadge = 40.dp
    /** Icon top inset without ribbon — Figma y=14. */
    val storeCoinIconTop = 14.dp
    /** Icon → amount gap — Figma 4dp. */
    val storeCoinIconToAmount = 4.dp
    /** Amount → discount chip gap — Figma 9dp. */
    val storeCoinAmountToDiscount = 9.dp
    /** Discount chip → price button gap — Figma 8dp. */
    val storeCoinDiscountToButton = 8.dp
    /** Card bottom inset under price button — Figma 14dp. */
    val storeCoinCardBottom = 14.dp
    /** Discount chip min height — Figma 24dp. */
    val storeDiscountChipMinHeight = 24.dp
    /** Discount chip horizontal padding — Figma 9dp. */
    val storeDiscountChipHorizontal = 9.dp
    /** Discount chip vertical padding — Figma 6dp. */
    val storeDiscountChipVertical = 6.dp
    /** Store ribbon horizontal padding — Figma 18dp. */
    val storeRibbonHorizontal = 18.dp
    /** Store coin price button height — 36dp. */
    val storePriceButtonHeight = 36.dp
    /** Store coin price button min width — 92dp. */
    val storePriceButtonMinWidth = 92.dp
    /** Price button horizontal padding — Figma 13dp. */
    val storePriceButtonHorizontal = 13.dp
    /** Original/current price gap inside button — Figma 7dp. */
    val storePriceButtonGap = 7.dp
    /** Store coin card elevation blur approximation. */
    val storeCoinCardElevation = 8.dp
    /** Store VIP card elevation. */
    val storeVipCardElevation = 2.dp
    /** Store coin grid columns. */
    const val storeCoinColumns = 2
    /** Store first-load skeleton coin cards. */
    const val storeSkeletonCoinCount = 4
    /** Store skeleton section title bar. */
    val storeSkeletonSectionWidth = 108.dp
    val storeSkeletonSectionHeight = 14.dp
    /** Store skeleton coin amount bar. */
    val storeSkeletonAmountWidth = 56.dp
    val storeSkeletonAmountHeight = 22.dp
    /** Store skeleton discount chip width. */
    val storeSkeletonDiscountWidth = 72.dp
    /** VIP back icon — 24dp. */
    val vipNavIcon = 24.dp
    /** VIP benefit card height — 136dp. */
    val vipBenefitCardHeight = 136.dp
    /** VIP benefit icon — 32dp. */
    val vipBenefitIcon = 32.dp
    /** VIP benefit star — 12dp. */
    val vipBenefitStar = 12.dp
    /** VIP benefit divider height — Figma 62dp. */
    val vipBenefitDividerHeight = 62.dp
    /** VIP benefit item column — Figma 72dp. */
    val vipBenefitItemHeight = 72.dp
    /** VIP benefit label block — Figma 32dp. */
    val vipBenefitLabelHeight = 32.dp
    /** VIP plan card height — 167dp. */
    val vipPlanCardHeight = 167.dp
    /** VIP plan top badge height — Figma 23dp. */
    val vipPlanBadgeHeight = 23.dp
    /** VIP plan top badge min width — Figma 86dp. */
    val vipPlanBadgeMinWidth = 86.dp
    /** VIP CTA button height — 51dp. */
    val vipCtaHeight = 51.dp
    /** VIP disclaimer shield — 16dp. */
    val vipDisclaimerIcon = 16.dp
    /** VIP plan card elevation. */
    val vipPlanCardElevation = 4.dp
    /** VIP benefit card elevation. */
    val vipBenefitCardElevation = 2.dp
    /** VIP unselected sale pill vertical pad — Figma 1dp. */
    val vipPlanSaleMutedVertical = 1.dp
    /** VIP nav back top inset within header — Figma 16dp. */
    val vipNavIconTop = 16.dp
    /** VIP pay-guide sheet close icon leaf — Figma 20dp. */
    val vipGuideCloseIcon = 20.dp
    /** Coin pay-guide close chip — Figma 36dp. */
    val coinGuideCloseChip = 36.dp
    /** Coin pay-guide close glyph — Figma ~22dp. */
    val coinGuideCloseIcon = 22.dp
    /** Coin pay-guide balance coin — Figma 16dp. */
    val coinGuideBalanceCoin = 16.dp
    /** Coin pay-guide close chip elevation — Figma 0 1 2 @ 4%. */
    val coinGuideCloseElevation = 2.dp
    /**
     * Coin pay-guide sheet content max height — Figma frame 468dp.
     * Keeps sticky header + scroll body measurable for [Modifier.weight].
     */
    val coinGuideSheetMaxHeight = 468.dp
    /**
     * Figma `弹窗背景 1` image scale inside the 468dp frame: `height: 142.37%`.
     */
    const val coinGuideBgHeightRatio = 1.4237f
    /**
     * Figma `弹窗背景 1` image offset: `top: -31.44%` (crops lower portion of the asset).
     */
    const val coinGuideBgTopRatio = -0.3144f
    /** VIP pay-guide plan badge icon — Figma 60dp. */
    val vipGuidePlanIcon = 60.dp
    /** VIP pay-guide plan card height — Figma 80dp. */
    val vipGuidePlanCardHeight = 80.dp
    /** VIP pay-guide plan card elevation. */
    val vipGuidePlanCardElevation = 2.dp
    /** VIP pay-guide benefits card elevation — Figma soft purple shadow. */
    val vipGuideBenefitCardElevation = 4.dp
    /** VIP pay-guide CTA height — Figma py 16 → ~51dp with 16sp text. */
    val vipGuideCtaHeight = 51.dp
    /** VIP first-load skeleton plan cards. */
    const val vipSkeletonPlanCount = 3
    const val vipSkeletonBenefitCount = 5
    val vipSkeletonHeroWidth = 168.dp
    val vipSkeletonHeroHeight = 28.dp
    val vipSkeletonSectionWidth = 140.dp
    val vipSkeletonSectionHeight = 14.dp
    val vipSkeletonBenefitTitleWidth = 88.dp
    val vipSkeletonBenefitTitleHeight = 16.dp
    val vipSkeletonBenefitLabelWidth = 48.dp
    val vipSkeletonBenefitLabelHeight = 10.dp
    val vipSkeletonPlanTitleWidth = 64.dp
    val vipSkeletonPlanTitleHeight = 14.dp
    val vipSkeletonPlanPriceWidth = 72.dp
    val vipSkeletonPlanPriceHeight = 24.dp
    val vipSkeletonDisclaimerWidth = 200.dp
    val vipSkeletonDisclaimerHeight = 10.dp
}
