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
    /** VIP pay-guide plan card gradient. */
    val vipGuidePlanStart = Color(0xFFFFE6F8)
    val vipGuidePlanMid = Color(0xFFEDE1FF)
    val vipGuidePlanEnd = Color(0xFFF5E1FF)
    /** VIP pay-guide plan card shadow — black @ 10%. */
    val vipGuidePlanShadow = Color(0x1A000000)
}
