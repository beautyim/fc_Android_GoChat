package com.example.demoproject.ui.designsystem

import androidx.compose.ui.graphics.Color

/** Semantic colors shared across branded screens (login, legal links, etc.). */
object DemoColors {
    val link = Color(0xFF9D41FF)
    val textPrimary = Color(0xFF333333)
    val textSecondary = Color(0xFF666666)
    val textTertiary = Color(0xFF8A8A8E)
    val textTitle = Color(0xFF111111)
    val orLabel = Color(0x99666666)
    val divider = Color(0xFFE0E0E5)
    val inputBorder = Color(0xFFE0E0E5)
    /** Bind/change-email field stroke — #999 @ 20% (Figma 1:3191). */
    val emailFieldBorder = Color(0x33999999)
    /** Input placeholder — #999 @ 60% (Figma auth inputs). */
    val inputPlaceholder = Color(0x99999999)
    /** Auxiliary label — #999999 (Figma Online chips / helper). */
    val textAuxiliary = Color(0xFF999999)
    val sheet = Color(0xFFFFFFFF)
    /** Match tab center glow — rgba(183,171,223,0.4) / Figma drop-shadow. */
    val tabMatchGlow = Color(0x66B7ABDF)
    /** Match tab button fill — #FDFDFD. */
    val tabMatchButton = Color(0xFFFDFDFD)
    /** Light page canvas — #F9F9F9 (forget-password, etc.). */
    val page = Color(0xFFF9F9F9)
    /** Top bar title — #15131A. */
    val navTitle = Color(0xFF15131A)
    val onPrimaryButton = Color(0xFFFFFFFF)
    /** Modal scrim — rgba(0,0,0,0.5). */
    val scrim = Color(0x80000000)
    /** Secondary dialog button fill — #F3F0FC. */
    val dialogSecondary = Color(0xFFF3F0FC)
    /** Destructive confirm primary — #FB4F6F (Figma 80:3781). */
    val dialogDestructive = Color(0xFFFB4F6F)
    /** Soft glow behind destructive dialog icon. */
    val dialogDestructiveGlow = Color(0x33FB4F6F)
    /** Chip / coin pill fill — #EFEFEF. */
    val chip = Color(0xFFEFEFEF)
    /** Field / form validation error text — #FF3535. */
    val error = Color(0xFFFF3535)
    val gradientStart = Color(0xFF7722FF)
    val gradientEnd = Color(0xFF2158FF)
    /** Online header wash — #E9E5FF. */
    val onlineHeaderStart = Color(0xFFE9E5FF)
    /** Online card action — #00D670. */
    val onlineAction = Color(0xFF00D670)
    /** FREE badge — #FF6A00. */
    val freeBadge = Color(0xFFFF6A00)
    /** Welfare dialog "Free" title — #FF3292 (Figma 1:5515 / 1:5563). */
    val welfareFreeTitle = Color(0xFFFF3292)
    /** Free-video reward label — #FFDAEB (Figma 1:5516). */
    val welfareFreeVideoReward = Color(0xFFFFDAEB)
    /** Free-match reward label — #FF3493 (Figma 1:5565). */
    val welfareFreeMatchReward = Color(0xFFFF3493)
    /** Free-match reward "x" — #FF3995 (Figma 1:5566). */
    val welfareFreeMatchTimes = Color(0xFFFF3995)
    /** Free-match reward count — #FF429A (Figma 1:5567). */
    val welfareFreeMatchCount = Color(0xFFFF429A)
    /** Free-video CTA gradient start — #F352BD (Figma 1:5513). */
    val welfareFreeVideoCtaStart = Color(0xFFF352BD)
    /** Free-video CTA gradient end — #FC2472 (Figma 1:5513). */
    val welfareFreeVideoCtaEnd = Color(0xFFFC2472)
    /** Free-video CTA underlay — Figma 1:5512 #17081E, matches footer so ghost button art is hidden. */
    val welfareFreeVideoCtaCover = Color(0xFF17081E)
    /** Free-video CTA underlay fade mid stop — #17081E @ ~72%. */
    val welfareFreeVideoCtaCoverMid = Color(0xB817081E)
    /** Free-video CTA underlay top edge (fully transparent) for soft fade. */
    val welfareFreeVideoCtaCoverEdge = Color(0x0017081E)
    /** Treasure reward / price accent — #9D41FF. */
    val treasureAccent = Color(0xFF9D41FF)
    /** Treasure original price — #999999. */
    val treasureOriginalPrice = Color(0xFF999999)
    /** Treasure VIP / Match body — #333333. */
    val treasureBody = Color(0xFF333333)
    /** Treasure EXTRA BONUS panel fill — #FFF0E7. */
    val treasureExtraBonusFill = Color(0xFFFFF0E7)
    /** Treasure price-strip fill. */
    val treasurePriceStrip = Color(0xFFFFFFFF)
    /** Treasure original-price strike — red. */
    val treasureStrike = Color(0xFFFF3535)
    /** Treasure coin-glow disc — purple soft. */
    /** Treasure CTA text cover wash — #C84FFD → #A33BFE. */
    val treasureCtaCoverStart = Color(0xFFC84FFD)
    val treasureCtaCoverEnd = Color(0xFFA33BFE)
    /** Prize bonus / sale accent — #FF2E6E (Figma 1:5749 / 1:5753). */
    val prizeAccent = Color(0xFFFF2E6E)
    /** Prize body text — #333333. */
    val prizeBody = Color(0xFF333333)
    /** Prize original price — #999999. */
    val prizeOriginalPrice = Color(0xFF999999)
    /** Prize price strip — #FFEBF7 (Figma 1:5751). */
    val prizePriceStrip = Color(0xFFFFEBF7)
    /** Prize CTA cover wash — #FBD4FB (Figma 1:5740). */
    val prizeCtaCover = Color(0xFFFBD4FB)
    /** Prize CTA cover edge — transparent for soft rim. */
    val prizeCtaCoverEdge = Color(0x00FBD4FB)
    /** Prize dashed divider — #F978A0 (Figma 1:5743). */
    val prizeDashedDivider = Color(0xFFF978A0)
    /** Prize CTA gradient — #F352BD → #FC2472 (Figma 1:5741). */
    val prizeCtaStart = Color(0xFFF352BD)
    val prizeCtaEnd = Color(0xFFFC2472)
    /** Prize strike — red. */
    val prizeStrike = Color(0xFFFF3535)
    /** Video show CTA gradient start — Figma #00E384. */
    val videoShowCtaStart = Color(0xFF00E384)
    /** Video show CTA gradient end — Figma #00D7E6. */
    val videoShowCtaEnd = Color(0xFF00D7E6)
    /** Video show user chip fill — white @ 10%. */
    val videoShowUserChip = Color(0x1AFFFFFF)
    /** Video show progress track — #999999. */
    val videoShowProgressTrack = Color(0xFF999999)
    /** Status text on cards — white @ 60%. */
    val onCardMuted = Color(0x99FFFFFF)
    /** Card drop shadow — rgba(0,0,0,0.16). */
    val cardShadow = Color(0x29000000)
    /** Skeleton base — #E8E8E8. */
    val skeleton = Color(0xFFE8E8E8)
    /** Skeleton highlight — #F3F3F3. */
    val skeletonHighlight = Color(0xFFF3F3F3)
    /** Gift price / coin accent — #FFC733. */
    val giftPrice = Color(0xFFFFC733)
    /** Figma first-visit gift quick bar price — #333 @ 90%. */
    val chatGiftQuickPrice = Color(0xE6333333)
    /** Gift card default border — black @ 5%. */
    val giftCardBorder = Color(0x0D000000)
    /** Gift card selected fill — #9D41FF @ 10%. */
    val giftCardSelected = Color(0x1A9D41FF)
    /** Gift sheet top glow — soft lavender. */
    val giftSheetGlow = Color(0x66C9B8FF)
    /** Gift pager track — black @ 10%. */
    val giftPagerTrack = Color(0x1A000000)
    /** Gift balance pill — #F1F1F1. */
    val giftBalancePill = Color(0xFFF1F1F1)
    /** Gift card surface — #FEFEFE @ 96%. */
    val giftCardSurface = Color(0xF5FEFEFE)
    /** Primary CTA drop shadow — rgba(10,29,240,0.32). */
    val primaryShadow = Color(0x520A1DF0)
    /** Secondary CTA drop shadow — rgba(10,29,240,0.12). */
    val secondaryShadow = Color(0x1F0A1DF0)
    /** Profile nav circle fill — black @ 30%. */
    val profileNavScrim = Color(0x4D000000)
    /** Profile stats divider — #999 @ 20%. */
    val profileStatsDivider = Color(0x33999999)
    /** Profile video thumb bottom gradient end. */
    val profileVideoScrim = Color(0xFF000000)
    /** Media viewer time labels — #FEFEFE. */
    val profileMediaTime = Color(0xFFFEFEFE)
    /** Media viewer scrubber track — #CFCFDD @ 20%. */
    val profileMediaProgressTrack = Color(0x33CFCFDD)
    /** Media viewer controls bottom fade end — black @ 30%. */
    val profileMediaControlsScrimEnd = Color(0x4D000000)
    /** Profile avatar ring. */
    val profileAvatarRing = Color(0xFFFFFFFF)
    /** iOS-style action sheet action text — #007AFF. */
    val actionSheetAction = Color(0xFF007AFF)
    /** Action sheet row separator — gray @ 55%. */
    val actionSheetSeparator = Color(0x8C808080)
    /** Action sheet frosted surface. */
    val actionSheetSurface = Color(0xF2F2F2F7)
    /** Call records missed / rejected status — #E80000. */
    val callRecordsNegative = Color(0xFFE80000)
    /** Call records connected / match status — #03B300. */
    val callRecordsPositive = Color(0xFF03B300)
    /** Call ringing hang up / cancel fill — Figma red. */
    val callRingingHangup = Color(0xFFFF3B30)
    /** Call ringing answer fill — Figma green. */
    val callRingingAnswer = Color(0xFF34C759)
    /** Call ringing answer outer glow. */
    val callRingingAnswerGlow = Color(0x4034C759)
    /** Call ringing answer mid glow ring. */
    val callRingingAnswerGlowMid = Color(0x6634C759)
    /** Call ringing report circle — black @ 20%. */
    val callRingingReportBg = Color(0x33000000)
    /** Call ringing top scrim start — black @ 50%. */
    val callRingingTopScrim = Color(0x80000000)
    /** Call ringing status line — white @ 80%. */
    val callRingingStatus = Color(0xCCFFFFFF)
    /** Call ringing on-video text / icons. */
    val callRingingOnVideo = Color(0xFFFFFFFF)
    /** Call ringing text shadow. */
    val callRingingTextShadow = Color(0x80000000)
    /** Match-call connecting canvas — Figma 1:1520. */
    val callConnectingBackground = Color(0xFF08040E)
    /** Match-call connecting avatar border — Figma #CB9AE1. */
    val callConnectingAvatarBorder = Color(0xFFCB9AE1)
    /** Match-call connecting avatar glow — Figma #C54CFF. */
    val callConnectingAvatarGlow = Color(0xFFC54CFF)
    /** Match-call connecting subtitle — Figma #EED9FF. */
    val callConnectingSubtitle = Color(0xFFEED9FF)
    /** Match-call connecting status — white @ 70%. */
    val callConnectingStatus = Color(0xB3FFFFFF)
    /** Match-call connecting toast text — Figma semantic black. */
    val callConnectingNoticeText = Color(0xFF333333)
    /** Match-call title gradient stops — Figma 1:1529. */
    val callConnectingTitleStart = Color(0xFFF9ECFF)
    val callConnectingTitleMiddle = Color(0xFFD4A5FE)
    val callConnectingTitleEnd = Color(0xFFE27FF6)
    /** In-call overlay chip / bubble — black @ 20%. */
    val callInCallOverlay = Color(0x33000000)
    /** In-call composer placeholder — white @ 70%. */
    val callInCallPlaceholder = Color(0xB3FFFFFF)
    /** Match-call Next pill — black @ 30%; disabled state dims the whole control to 50%. */
    val callMatchNextEnabled = Color(0x4D000000)
    /** In-call gift quick price — white @ 90%. */
    val callInCallGiftQuickPrice = Color(0xE6FFFFFF)
    /** In-call Like / accent fill — #9D41FF. */
    val callInCallAccent = Color(0xFF9D41FF)
    /** In-call more-sheet icon circle — #9D41FF @ 10%. */
    val callInCallMoreIconBg = Color(0x1A9D41FF)
    /** In-call more row divider — black @ 10%. */
    val callInCallMoreDivider = Color(0x1A000000)
    /** In-call gift-request highlight — #E94381. */
    val callInCallGiftRequestAccent = Color(0xFFE94381)
    /** In-call gift-request subtitle — white @ 80%. */
    val callInCallGiftRequestSub = Color(0xCCFFFFFF)
    /** In-call gift-sent tip bar — #A855F7 @ 50%. */
    val callInCallGiftSentTip = Color(0x80A855F7)
    /** In-call gift-sent tip subtitle — white @ 80%. */
    val callInCallGiftSentTipSub = Color(0xCCFFFFFF)
    /** iOS segmented control track — #767680 @ 12%. */
    val segmentTrack = Color(0x1F767680)
    /** Segmented control selected pill shadow. */
    val segmentSelectedShadow = Color(0x1A000000)
    /** Me page VIP card fill (member) — #FFFAF0 → #FFF7E6 mid. */
    val meVipCardStart = Color(0xFFFFFAF0)
    val meVipCardEnd = Color(0xFFFFF7E6)
    /** Me page VIP card fill (non-member) — #F0F0F0. */
    val meVipCardInactive = Color(0xFFF0F0F0)
    /** Me page VIP card border — #FFEFD2. */
    val meVipCardBorder = Color(0xFFFFEFD2)
    /** Me camera badge fill — #F0E2FF. */
    val meCameraBadge = Color(0xFFF0E2FF)
    /** Me menu icon circle — #9D41FF @ 10%. */
    val meMenuIconBg = Color(0x1A9D41FF)
    /** Report reason icon well — #EEDEFF. */
    val reportReasonIconBg = Color(0xFFEEDEFF)
    /** Report reason row border — #999 @ 30%. */
    val reportReasonBorder = Color(0x4D999999)
    /** Report online status label — #858692. */
    val reportOnlineLabel = Color(0xFF858692)
    /** Report screenshot remove chip — black @ 30%. */
    val reportShotRemoveBg = Color(0x4D000000)
    /** Settings logout icon circle — #FF4144 @ 10%. */
    val settingsLogoutIconBg = Color(0x1AFF4144)
    /** Me tab chat unread badge — #FF3838. */
    val meUnreadBadge = Color(0xFFFF3838)
    /** Chat list timestamp — #939393. */
    val chatTimestamp = Color(0xFF939393)
    /** Chat swipe pin action — #FF9500. */
    val chatSwipePin = Color(0xFFFF9500)
    /** Chat swipe delete action — #FF3B30. */
    val chatSwipeDelete = Color(0xFFFF3B30)
    /** Chat notification banner shadow — rgba(196,170,255,0.25). */
    val chatBannerShadow = Color(0x40C4AAFF)
    /** Chat notification bell circle — soft lavender. */
    val chatNotifBellBg = Color(0xFFEDE6FF)
    /** Chat preview text — #666566. */
    val chatPreview = Color(0xFF666566)
    /** Chat detail page canvas — #F9F9F9. */
    val chatDetailPage = Color(0xFFF9F9F9)
    /** Incoming bubble fill — #F3F0FF. */
    val chatBubbleIncoming = Color(0xFFF3F0FF)
    /** Outgoing bubble fill — #9D41FF. */
    val chatBubbleOutgoing = Color(0xFF9D41FF)
    /** Composer field / secondary circle — #9D41FF @ 7%. */
    val chatComposerField = Color(0x129D41FF)
    /** Translate / action chip — black @ 5%. */
    val chatActionChip = Color(0x0D000000)
    /** Online presence dot. */
    val chatOnlineDot = Color(0xFF00D670)
    /** Gift bubble wash end — #F0E6FA. */
    val chatGiftWash = Color(0xFFF0E6FA)
    /** Gift coin price — #FFA100. */
    val chatGiftCoin = Color(0xFFFFA100)
    /** Locked media scrim — black @ 50%. */
    val chatMediaScrim = Color(0x80000000)
    /** Locked media icon circle — white @ 10%. */
    val chatMediaLockCircle = Color(0x1AFFFFFF)
    /** Call missed label — #FF1010. */
    val chatCallMissed = Color(0xFFFF1010)
    /** Store VIP promo card fill — #FFE6F8 → #EDE1FF → #F5E1FF. */
    val storeVipCardStart = Color(0xFFFFE6F8)
    val storeVipCardMid = Color(0xFFEDE1FF)
    val storeVipCardEnd = Color(0xFFF5E1FF)
    /** Store VIP promo card border — #D5AEFF. */
    val storeVipCardBorder = Color(0xFFD5AEFF)
    /** Store VIP card soft shadow — black @ 10%. */
    val storeVipCardShadow = Color(0x1A000000)
    /** Store sale promo card fill — #FFF2E6 → #FFE3E1 → #FFF5E1. */
    val storeSaleCardStart = Color(0xFFFFF2E6)
    val storeSaleCardMid = Color(0xFFFFE3E1)
    val storeSaleCardEnd = Color(0xFFFFF5E1)
    /** Store sale promo card border — #FFDBAE. */
    val storeSaleCardBorder = Color(0xFFFFDBAE)
    /** Store sale Extra label — #666666. */
    val storeSaleExtraLabel = Color(0xFF666666)
    /** Store coin card surface — white @ 96%. */
    val storeCoinCardSurface = Color(0xF5FFFFFF)
    /** Store coin card border — black @ 8%. */
    val storeCoinCardBorder = Color(0x14000000)
    /** Store coin card shadow — warm brown @ 8%. */
    val storeCoinCardShadow = Color(0x14794600)
    /** Store coin amount — #2B2D34. */
    val storeCoinAmount = Color(0xFF2B2D34)
    /** Store discount chip fill — primary #F31845 @ 7%. */
    val storeDiscountChip = Color(0x12F31845)
    /** Store discount chip text — #F31845. */
    val storeDiscountText = Color(0xFFF31845)
    /** Store VIP benefit muted — #333 @ 70%. */
    val storeVipBenefitMuted = Color(0xB2333333)
    /** Store VIP original price — #333 @ 50%. */
    val storeVipOriginalPrice = Color(0x80333333)
    /** Store VIP original-price diagonal strike — pink. */
    val storeVipPriceStrike = Color(0xFFFF5EA4)
    /** Store VIP match label muted — #333 @ 80%. */
    val storeVipMatchMuted = Color(0xCC333333)
    /** Store price button original — white @ 60%. */
    val storePriceOriginal = Color(0x99FFFFFF)
    /** Store VIP ribbon start/end. */
    val storeVipRibbonStart = Color(0xFFFF5EA4)
    val storeVipRibbonEnd = Color(0xFF2C17E8)
    /** Store super-discount ribbon. */
    val storeSaleRibbonStart = Color(0xFFFF5EAC)
    val storeSaleRibbonEnd = Color(0xFFE81772)
    /** Store pager inactive dot — primary @ 50%. */
    val storePagerInactive = Color(0x809D41FF)
    /** VIP page hero title — #291551. */
    val vipHeroTitle = Color(0xFF291551)
    /** VIP benefit card shadow — #C126FF @ 10%. */
    val vipBenefitCardShadow = Color(0x1AC126FF)
    /** VIP plan card selected fill — #F3E7FF. */
    val vipPlanSelectedFill = Color(0xFFF3E7FF)
    /** VIP plan unselected price / muted — #999999. */
    val vipPlanMuted = Color(0xFF999999)
    /** VIP plan unselected top badge. */
    val vipPlanBadgeStart = Color(0xFFDBDDE2)
    val vipPlanBadgeEnd = Color(0xFFF1F2F4)
    val vipPlanBadgeText = Color(0xFF4D525C)
    /** VIP selected top badge. */
    val vipPlanPopularStart = Color(0xFF8391FF)
    val vipPlanPopularEnd = Color(0xFF8A4FFF)
    /** VIP selected discount pill. */
    val vipPlanDiscountStart = Color(0xFFFD842C)
    val vipPlanDiscountEnd = Color(0xFFFD3435)
    /** VIP unselected discount pill text — #F8F3CB. */
    val vipPlanDiscountMutedText = Color(0xFFF8F3CB)
    /** VIP plan card shadow — #0A0DD4 @ 20%. */
    val vipPlanCardShadow = Color(0x330A0DD4)
    /** VIP pay-guide accent (“free messages”) — #9B4DFB. */
    val vipGuideAccent = Color(0xFF9B4DFB)
    /** VIP pay-guide subtitle muted — #9287A9. */
    val vipGuideSubtitleMuted = Color(0xFF9287A9)
    /** VIP pay-guide close chip — white @ 40%. */
    val vipGuideCloseChip = Color(0x66FFFFFF)
    /** Coin pay-guide close chip — solid white. */
    val coinGuideCloseChip = Color(0xFFFFFFFF)
    /** Coin pay-guide close chip shadow — black @ 4%. */
    val coinGuideCloseShadow = Color(0x0A000000)
    /**
     * Coin pay-guide sheet bottom fill under the system nav bar —
     * matches the lower portion of Figma `弹窗背景 1` (~#F9F0F7).
     */
    val coinGuideSheetBottom = Color(0xFFF9F0F7)
    /** VIP pay-guide plan card gradient. */
    val vipGuidePlanStart = Color(0xFFFFE6F8)
    val vipGuidePlanMid = Color(0xFFEDE1FF)
    val vipGuidePlanEnd = Color(0xFFF5E1FF)
    /** VIP pay-guide plan card shadow — black @ 10%. */
    val vipGuidePlanShadow = Color(0x1A000000)
    /** Call balance float card fill — #DBDCFF → #A3B4F2 → #A181FA. */
    val callBalanceFloatStart = Color(0xFFDBDCFF)
    val callBalanceFloatMid = Color(0xFFA3B4F2)
    val callBalanceFloatEnd = Color(0xFFA181FA)
    /** Call balance float border — #616CFF. */
    val callBalanceFloatBorder = Color(0xFF616CFF)
    /** Call balance float timer ribbon — soft purple placeholder until asset lands. */
    val callBalanceFloatTimerRibbon = Color(0xFF6B3AC9)
    /** Call balance float OFF badge — soft lilac placeholder until asset lands. */
    val callBalanceFloatOffBadge = Color(0xFFB89BFF)
    /** Call balance float VIP title gold gradient. */
    val callBalanceFloatVipGoldStart = Color(0xFFFFF200)
    val callBalanceFloatVipGoldEnd = Color(0xFFFFA17C)
    /** Call balance float product mini-card fill — same family as store VIP card. */
    val callBalanceFloatProductStart = Color(0xFFFFE6F8)
    val callBalanceFloatProductMid = Color(0xFFEDE1FF)
    val callBalanceFloatProductEnd = Color(0xFFF5E1FF)
    /** Call balance float OFF percent gold gradient. */
    val callBalanceFloatOffPercentStart = Color(0xFFFDFDC8)
    val callBalanceFloatOffPercentEnd = Color(0xFFFDEE64)
    /** Call balance offer-guide hangup seconds — #FF3B30. */
    val callBalanceOfferHangupSeconds = Color(0xFFFF3B30)
    /** Call balance offer-guide more-options — #999999. */
    val callBalanceOfferMoreOptions = Color(0xFF999999)
    /**
     * Call hangup-recharge / continue sheet bottom under system nav —
     * Figma `#EADFF2` (node 1:5167 / 193:6512).
     */
    val callHangupSheetBottom = Color(0xFFEADFF2)
    /** Call hangup peer avatar ring — Figma `#7722FF`. */
    val callHangupAvatarBorder = Color(0xFF7722FF)
    /** Call hangup speech-bubble body — Figma `#8D7E86`. */
    val callHangupSpeechText = Color(0xFF8D7E86)
    /** Call hangup speech-bubble shadow — pink @ 20%. */
    val callHangupSpeechShadow = Color(0x33FBA4B4)
    /** Call hangup continue secondary border — Figma `#7722FF`. */
    val callHangupSecondaryBorder = Color(0xFF7722FF)
    /** Match hero card scrim — black @ 30% (Figma 1:1328). */
    val matchHeroScrim = Color(0x4D000000)
    /** Match hero placeholder wash — Figma #FF8181 (1:1326). */
    val matchHeroFallback = Color(0xFFFF8181)
    /** Match price /time chip fill — white @ 10% (Figma 1:1353). */
    val matchPriceChip = Color(0x1AFFFFFF)
    /** Match VIP 50% Off badge fill — #FFEA32 @ 10% (Figma 328:5409). */
    val matchVipOffBadge = Color(0x1AFFEA32)
    /** Match radar ripple stroke — soft lilac. */
    val matchRipple = Color(0xFFC9B8FF)
    /** Match filter option fill — Figma #F5F5F5. */
    val matchFilterOption = Color(0xFFF5F5F5)
    /** Match filter option hairline — black @ 5%. */
    val matchFilterOptionBorder = Color(0x0D000000)
    /** Match filter selected fill — primary purple @ 10%. */
    val matchFilterSelected = Color(0x1A9D41FF)
    /** Matching-in-progress full-bleed scrim — black @ 30% (Figma 1:1463). */
    val matchSearchingScrim = Color(0x4D000000)
    /** Matching ripple wave body — #9046FF @ 10% (Figma Ellipse 22/23). */
    val matchSearchingWaveFill = Color(0x1A9046FF)
    /** Matching ripple wave hairline — #9046FF @ 20%. */
    val matchSearchingWaveStroke = Color(0x339046FF)
    /** Matching core disc, outer band — #140033 @ 60% (Figma Ellipse 24). */
    val matchSearchingCoreOuter = Color(0x99140033)
    /** Matching core disc, middle band — #140033 @ 50% (Figma Ellipse 25). */
    val matchSearchingCoreMiddle = Color(0x80140033)
    /** Matching core disc, inner band — #140033 @ 10% (Figma Ellipse 26). */
    val matchSearchingCoreInner = Color(0x1A140033)
    /** Matching core band hairline — #B98AFF @ 50%. */
    val matchSearchingCoreStroke = Color(0x80B98AFF)
    /** Matching innermost band hairline — #B56CD8. */
    val matchSearchingInnerStroke = Color(0xFFB56CD8)
    /** Matching rotating arc gradient — #F1DCFE → #BD88FC (Figma Ellipse 27). */
    val matchSearchingArcStart = Color(0xFFF1DCFE)
    val matchSearchingArcEnd = Color(0xFFBD88FC)
    /** Matching status title — #E5CEFB. */
    val matchSearchingTitle = Color(0xFFE5CEFB)
    /** Matching status caption — #E2D1F7. */
    val matchSearchingCaption = Color(0xFFE2D1F7)
    /** Matching Cancel link — white @ 40%. */
    val matchSearchingCancel = Color(0x66FFFFFF)
}
