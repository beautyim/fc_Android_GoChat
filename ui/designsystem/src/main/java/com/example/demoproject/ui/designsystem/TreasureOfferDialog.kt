package com.example.demoproject.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

/**
 * Limited-time treasure / promo offer popup variants from Figma:
 * - [TreasureOfferVariant.SmallCoins] — 1:5756
 * - [TreasureOfferVariant.Vip] — 1:5572
 * - [TreasureOfferVariant.DualCoins] — 1:5811
 * - [TreasureOfferVariant.DualCoinsWithBonus] — 1:5631
 *
 * Coin-bag / coin glyphs load from backend URLs (Coil). Missing URL shows
 * nothing — no local coin-bag fallback. Purple glow is
 * [R.drawable.treasure_ic_coin_glow] (Group 51).
 */
sealed interface TreasureOfferVariant {
    data class SmallCoins(
        val coins: Int,
        val coinIconUrl: String? = null,
    ) : TreasureOfferVariant

    data class Vip(
        val vipTitle: String,
        val bonusCoins: Int,
        val matchCount: Int,
        /** Small coin glyph next to `+829`; backend `coin_icon` when present. */
        val coinIconUrl: String? = null,
    ) : TreasureOfferVariant

    data class DualCoins(
        val leftCoins: Int,
        val rightCoins: Int,
        val leftIconUrl: String? = null,
        val rightIconUrl: String? = null,
    ) : TreasureOfferVariant

    data class DualCoinsWithBonus(
        val leftCoins: Int,
        val rightCoins: Int,
        val matchCount: Int,
        val leftIconUrl: String? = null,
        val rightIconUrl: String? = null,
    ) : TreasureOfferVariant
}

@Composable
fun TreasureOfferDialog(
    variant: TreasureOfferVariant,
    originalPrice: String,
    salePrice: String,
    onGetOffer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnScrim: Boolean = true,
) {
    val tall = variant is TreasureOfferVariant.DualCoinsWithBonus
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(20f)
            .background(DemoColors.scrim)
            .pointerInput(dismissOnScrim) {
                detectTapGestures(
                    onTap = {
                        if (dismissOnScrim) onDismiss()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = Spacing.md)
                .widthIn(max = ComponentSize.treasureDialogWidth)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            TreasureOfferCard(
                variant = variant,
                originalPrice = originalPrice,
                salePrice = salePrice,
                tall = tall,
                onGetOffer = onGetOffer,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun TreasureOfferCard(
    variant: TreasureOfferVariant,
    originalPrice: String,
    salePrice: String,
    tall: Boolean,
    onGetOffer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeCd = stringResource(R.string.treasure_cd_close)
    val aspect = if (tall) {
        ComponentSize.treasureDialogTallAspect
    } else {
        ComponentSize.treasureDialogAspect
    }
    val ctaBottom = if (tall) {
        ComponentSize.treasureTallCtaBottom
    } else {
        ComponentSize.treasureCtaBottom
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspect),
    ) {
        Image(
            painter = painterResource(
                if (tall) R.drawable.treasure_bg_hero_tall else R.drawable.treasure_bg_hero,
            ),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(Radius.lg)),
            contentScale = ContentScale.Crop,
        )
        if (tall) {
            Image(
                painter = painterResource(R.drawable.treasure_bg_footer),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .aspectRatio(1020f / 624f)
                    .clip(
                        RoundedCornerShape(
                            bottomStart = Radius.lg,
                            bottomEnd = Radius.lg,
                        ),
                    ),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = ComponentSize.treasureContentTop)
                .padding(horizontal = Spacing.treasureContentInset)
                .then(
                    if (tall) {
                        Modifier.padding(
                            bottom = ComponentSize.treasureCtaHeight + ctaBottom + Spacing.sm,
                        )
                    } else {
                        Modifier.height(ComponentSize.treasureContentHeight)
                    },
                ),
        ) {
            if (!tall) {
                Image(
                    painter = painterResource(R.drawable.treasure_bg_content),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(Radius.lg)),
                    contentScale = ContentScale.Crop,
                )
            }
            when (variant) {
                is TreasureOfferVariant.SmallCoins -> {
                    SmallCoinsBody(
                        coins = variant.coins,
                        coinIconUrl = variant.coinIconUrl,
                        originalPrice = originalPrice,
                        salePrice = salePrice,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                is TreasureOfferVariant.Vip -> {
                    VipBody(
                        vipTitle = variant.vipTitle,
                        bonusCoins = variant.bonusCoins,
                        matchCount = variant.matchCount,
                        coinIconUrl = variant.coinIconUrl,
                        originalPrice = originalPrice,
                        salePrice = salePrice,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                is TreasureOfferVariant.DualCoins -> {
                    DualCoinsBody(
                        leftCoins = variant.leftCoins,
                        rightCoins = variant.rightCoins,
                        leftIconUrl = variant.leftIconUrl,
                        rightIconUrl = variant.rightIconUrl,
                        originalPrice = originalPrice,
                        salePrice = salePrice,
                        matchCount = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                is TreasureOfferVariant.DualCoinsWithBonus -> {
                    DualCoinsBody(
                        leftCoins = variant.leftCoins,
                        rightCoins = variant.rightCoins,
                        leftIconUrl = variant.leftIconUrl,
                        rightIconUrl = variant.rightIconUrl,
                        originalPrice = originalPrice,
                        salePrice = salePrice,
                        matchCount = variant.matchCount,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        TreasureCtaButton(
            onClick = onGetOffer,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = ctaBottom)
                .width(ComponentSize.treasureCtaWidth)
                .height(ComponentSize.treasureCtaHeight),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(ComponentSize.treasureCloseInset)
                .size(ComponentSize.treasureClose)
                .clickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onDismiss,
                )
                .semantics { contentDescription = closeCd },
        )
    }
}

@Composable
private fun SmallCoinsBody(
    coins: Int,
    coinIconUrl: String?,
    originalPrice: String,
    salePrice: String,
    modifier: Modifier = Modifier,
) {
    // Figma 199:6592 content panel: glow@y47, label@y148, price strip flush bottom@y193.
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Spacing.treasureRewardTop),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CoinRewardIcon(iconUrl = coinIconUrl)
            Spacer(modifier = Modifier.height(Spacing.treasureRewardToLabel))
            Text(
                text = stringResource(R.string.treasure_coins_fmt, coins),
                color = DemoColors.treasureAccent,
                fontSize = TextSize.treasureRewardCoins,
                lineHeight = TextSize.treasureRewardCoinsLine,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = DemoTextAutoSize.price(TextSize.treasureRewardCoins),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
            )
        }
        TreasurePriceStrip(
            originalPrice = originalPrice,
            salePrice = salePrice,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun VipBody(
    vipTitle: String,
    bonusCoins: Int,
    matchCount: Int,
    coinIconUrl: String?,
    originalPrice: String,
    salePrice: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(Spacing.treasureVipRowTop))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(R.drawable.treasure_ic_vip),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.treasureVipBadge),
                contentScale = ContentScale.Fit,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.treasureVipTitleGap),
            ) {
                Text(
                    text = vipTitle,
                    color = DemoColors.treasureBody,
                    fontSize = TextSize.treasureVipTitle,
                    lineHeight = TextSize.treasureVipTitle,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = DemoTextAutoSize.label(TextSize.treasureVipTitle),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = stringResource(R.string.treasure_bonus_coins_fmt, bonusCoins),
                        color = DemoColors.treasureAccent,
                        fontSize = TextSize.treasureVipTitle,
                        lineHeight = TextSize.treasureVipTitle,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = DemoTextAutoSize.price(TextSize.treasureVipTitle),
                    )
                    TreasureRemoteCoinIcon(
                        iconUrl = coinIconUrl,
                        size = ComponentSize.treasureVipCoin,
                        // VIP bonus row uses a coin glyph (not coin-bag); show local when CDN absent.
                        fallbackToLocalCoin = true,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.treasureVipToBonus))
        ExtraBonusMatchRow(
            matchCount = matchCount,
            tagLift = Spacing.treasureVipExtraTagLift,
        )
        Spacer(modifier = Modifier.weight(1f))
        TreasurePriceRow(
            originalPrice = originalPrice,
            salePrice = salePrice,
            modifier = Modifier.padding(bottom = Spacing.md),
        )
    }
}

@Composable
private fun DualCoinsBody(
    leftCoins: Int,
    rightCoins: Int,
    leftIconUrl: String?,
    rightIconUrl: String?,
    originalPrice: String,
    salePrice: String,
    matchCount: Int?,
    modifier: Modifier = Modifier,
) {
    val rewardTop = if (matchCount != null) {
        Spacing.treasureDualBonusRewardTop
    } else {
        Spacing.treasureDualRewardTop
    }
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(rewardTop))
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            DualCoinColumn(coins = leftCoins, iconUrl = leftIconUrl)
            Box(
                modifier = Modifier
                    .height(ComponentSize.treasureRewardGlow)
                    .padding(horizontal = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.treasure_plus),
                    color = DemoColors.treasureAccent,
                    fontSize = TextSize.treasureDualCoins,
                    lineHeight = TextSize.treasureDualCoins,
                    fontWeight = FontWeight.Black,
                )
            }
            DualCoinColumn(coins = rightCoins, iconUrl = rightIconUrl)
        }
        if (matchCount != null) {
            Spacer(modifier = Modifier.height(Spacing.treasureDualToBonus))
            ExtraBonusMatchRow(matchCount = matchCount)
            Spacer(modifier = Modifier.weight(1f))
            TreasurePriceRow(
                originalPrice = originalPrice,
                salePrice = salePrice,
                modifier = Modifier.padding(bottom = Spacing.md),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
            TreasurePriceStrip(
                originalPrice = originalPrice,
                salePrice = salePrice,
            )
        }
    }
}

@Composable
private fun DualCoinColumn(
    coins: Int,
    iconUrl: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoinRewardIcon(iconUrl = iconUrl)
        Spacer(modifier = Modifier.height(Spacing.treasureDualRewardToLabel))
        Text(
            text = stringResource(R.string.treasure_coins_fmt, coins),
            color = DemoColors.treasureAccent,
            fontSize = TextSize.treasureDualCoins,
            lineHeight = TextSize.treasureDualCoins,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = DemoTextAutoSize.price(TextSize.treasureDualCoins),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Coin-bag slot with Group 51 purple glow. Backend icon only when [iconUrl]
 * is present — no local coin-bag fallback.
 */
@Composable
private fun CoinRewardIcon(
    iconUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(ComponentSize.treasureRewardGlow),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.treasure_ic_coin_glow),
            contentDescription = null,
            modifier = Modifier.size(ComponentSize.treasureRewardGlowExtent),
            contentScale = ContentScale.Fit,
        )
        TreasureRemoteCoinIcon(
            iconUrl = iconUrl,
            size = ComponentSize.treasureRewardIcon,
        )
    }
}

@Composable
private fun TreasureRemoteCoinIcon(
    iconUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    fallbackToLocalCoin: Boolean = false,
) {
    val context = LocalContext.current
    val localCoin = painterResource(R.drawable.treasure_ic_coin)
    val empty = ColorPainter(Color.Transparent)
    if (iconUrl.isNullOrBlank()) {
        if (!fallbackToLocalCoin) return
        Image(
            painter = localCoin,
            contentDescription = null,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
        )
        return
    }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(iconUrl)
            .crossfade(true)
            .build(),
        contentDescription = null,
        placeholder = if (fallbackToLocalCoin) localCoin else empty,
        error = if (fallbackToLocalCoin) localCoin else empty,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun ExtraBonusMatchRow(
    matchCount: Int,
    modifier: Modifier = Modifier,
    tagLift: Dp = 0.dp,
) {
    Box(
        modifier = modifier
            .width(ComponentSize.treasureExtraBonusWidth)
            .height(ComponentSize.treasureExtraBonusHeight + tagLift),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(ComponentSize.treasureExtraBonusHeight - Spacing.xs)
                .clip(RoundedCornerShape(Radius.sm))
                .background(DemoColors.treasureExtraBonusFill)
                .border(1.dp, DemoColors.sheet, RoundedCornerShape(Radius.sm)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(ComponentSize.treasureExtraTagWidth)
                .height(ComponentSize.treasureExtraTagHeight),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.treasure_bg_extra_tag),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            Text(
                text = stringResource(R.string.treasure_extra_bonus),
                color = DemoColors.treasureAccent,
                fontSize = TextSize.treasureExtraTag,
                lineHeight = TextSize.treasureExtraTag,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = DemoTextAutoSize.label(TextSize.treasureExtraTag),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xxs),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs)
                .padding(bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
        ) {
            Image(
                painter = painterResource(R.drawable.treasure_ic_match),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.treasureMatchIcon),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(R.string.treasure_match_label),
                color = DemoColors.treasureBody,
                fontSize = TextSize.treasureMatchLabel,
                lineHeight = TextSize.treasureMatchLabel,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = DemoTextAutoSize.label(TextSize.treasureMatchLabel),
            )
            Text(
                text = stringResource(R.string.treasure_match_count_fmt, matchCount),
                color = DemoColors.treasureBody,
                fontSize = TextSize.treasureMatchCount,
                lineHeight = TextSize.treasureMatchCount,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = DemoTextAutoSize.price(TextSize.treasureMatchCount),
            )
        }
    }
}

@Composable
private fun TreasurePriceStrip(
    originalPrice: String,
    salePrice: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.treasurePriceStripHeight)
            .clip(
                RoundedCornerShape(
                    bottomStart = Radius.md,
                    bottomEnd = Radius.md,
                ),
            )
            .background(DemoColors.treasurePriceStrip),
        contentAlignment = Alignment.Center,
    ) {
        TreasurePriceRow(
            originalPrice = originalPrice,
            salePrice = salePrice,
        )
    }
}

@Composable
private fun TreasurePriceRow(
    originalPrice: String,
    salePrice: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = originalPrice,
                color = DemoColors.treasureOriginalPrice,
                fontSize = TextSize.treasureOriginalPrice,
                lineHeight = TextSize.treasureOriginalPrice,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = DemoTextAutoSize.price(TextSize.treasureOriginalPrice),
                textAlign = TextAlign.Center,
            )
            Canvas(
                modifier = Modifier
                    .width(ComponentSize.treasureStrikeWidth)
                    .height(ComponentSize.treasureStrikeStroke)
                    .rotate(-9.12f),
            ) {
                drawRect(color = DemoColors.treasureStrike)
            }
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = salePrice,
            color = DemoColors.treasureAccent,
            fontSize = TextSize.treasureSalePrice,
            lineHeight = TextSize.treasureSalePrice,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = DemoTextAutoSize.price(TextSize.treasureSalePrice),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TreasureCtaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.pill))
            .clickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.treasure_btn_cta),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        // Cover baked "Get VIP Now" copy with Figma "Get Offer".
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(18.dp)
                .background(DemoGradients.treasureCtaCover),
        )
        Text(
            text = stringResource(R.string.treasure_cta_get_offer),
            color = DemoColors.onPrimaryButton,
            fontSize = TextSize.treasureCta,
            lineHeight = TextSize.treasureCta,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = DemoTextAutoSize.label(TextSize.treasureCta),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun TreasureSmallCoinsPreview() {
    DemoTheme {
        TreasureOfferDialog(
            variant = TreasureOfferVariant.SmallCoins(coins = 103),
            originalPrice = "$63.95",
            salePrice = "$15.99",
            onGetOffer = {},
            onDismiss = {},
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Composable
private fun TreasureVipPreview() {
    DemoTheme {
        TreasureOfferDialog(
            variant = TreasureOfferVariant.Vip(
                vipTitle = "1 Month VIP",
                bonusCoins = 829,
                matchCount = 2,
            ),
            originalPrice = "$63.95",
            salePrice = "$15.99",
            onGetOffer = {},
            onDismiss = {},
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "Treasure DualCoins", locale = "en")
@Preview(name = "Treasure DualCoins AR", locale = "ar")
@Composable
private fun TreasureDualCoinsPreview() {
    DemoTheme {
        val coinUri = previewTreasureCoinUri()
        TreasureOfferDialog(
            variant = TreasureOfferVariant.DualCoins(
                leftCoins = 103,
                rightCoins = 103,
                leftIconUrl = coinUri,
                rightIconUrl = coinUri,
            ),
            originalPrice = "$63.95",
            salePrice = "$15.99",
            onGetOffer = {},
            onDismiss = {},
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "Treasure DualCoins+Bonus", locale = "en")
@Preview(name = "Treasure DualCoins+Bonus AR", locale = "ar")
@Composable
private fun TreasureDualCoinsWithBonusPreview() {
    DemoTheme {
        val coinUri = previewTreasureCoinUri()
        TreasureOfferDialog(
            variant = TreasureOfferVariant.DualCoinsWithBonus(
                leftCoins = 103,
                rightCoins = 103,
                matchCount = 2,
                leftIconUrl = coinUri,
                rightIconUrl = coinUri,
            ),
            originalPrice = "$63.95",
            salePrice = "$15.99",
            onGetOffer = {},
            onDismiss = {},
        )
    }
}

@Composable
private fun previewTreasureCoinUri(): String {
    val context = LocalContext.current
    return "android.resource://${context.packageName}/${R.drawable.treasure_ic_coin}"
}
