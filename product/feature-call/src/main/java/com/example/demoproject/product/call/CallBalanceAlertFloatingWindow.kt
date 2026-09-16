package com.example.demoproject.product.call

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.unit.dp
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.product.store.R as StoreR

private val CallBalanceFloatTightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

/**
 * In-call balance alert floating card (not system PiP).
 *
 * VIP: Figma `1:4840` — "Upgrade VIP" + benefits product.
 * Coin: Figma `502:3673` — "More Coins" + coin icon/amount product.
 */
@Composable
fun CallBalanceAlertFloatingWindow(
    offer: CallBalanceOffer,
    onClick: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cd = stringResource(R.string.call_balance_offer_cd_float)
    val shape = RoundedCornerShape(ComponentSize.callBalanceFloatRadius)

    Box(
        modifier = modifier
            .width(ComponentSize.callBalanceFloatWidth)
            .height(ComponentSize.callBalanceFloatHeight)
            .semantics { contentDescription = cd }
            .clickable(
                role = Role.Button,
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Spacing.callBalanceFloatBodyTop)
                .width(ComponentSize.callBalanceFloatWidth)
                .height(ComponentSize.callBalanceFloatBodyHeight)
                .shadow(
                    elevation = ComponentSize.callBalanceFloatElevation,
                    shape = shape,
                    ambientColor = DemoColors.storeVipCardShadow,
                    spotColor = DemoColors.storeVipCardShadow,
                )
                .clip(shape)
                .background(DemoGradients.callBalanceFloatCard)
                .border(ComponentSize.meCardStroke, DemoColors.callBalanceFloatBorder, shape),
        )

        CallBalanceFloatTimerChip(
            countdown = offer.formattedCountdown,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        when {
            offer.showVipTitle -> CallBalanceFloatVipTitle(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Spacing.callBalanceFloatTitleTop),
            )
            offer.showCoinTitle -> CallBalanceFloatCoinTitle(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Spacing.callBalanceFloatTitleTop),
            )
        }

        CallBalanceFloatProductCluster(
            offer = offer,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Spacing.callBalanceFloatOffTop),
        )
        CallBalanceFloatPriceButton(
            price = offer.price,
            originalPrice = offer.originalPrice,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Spacing.callBalanceFloatCtaTop),
        )

        Text(
            text = stringResource(StoreR.string.call_balance_offer_more_options),
            color = DemoColors.onPrimaryButton,
            fontSize = TextSize.callBalanceFloatMeta,
            lineHeight = TextSize.callBalanceFloatMetaLine,
            fontStyle = FontStyle.Italic,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = CallBalanceFloatTightLineHeight,
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Spacing.callBalanceFloatMoreTop)
                .clickable(
                    role = Role.Button,
                    onClick = onMoreOptions,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ),
        )
    }
}

/** OFF badge + product card with Figma overlap (badge overhangs product top by 11dp). */
@Composable
private fun CallBalanceFloatProductCluster(
    offer: CallBalanceOffer,
    modifier: Modifier = Modifier,
) {
    val hasOff = !offer.offPercent.isNullOrBlank()
    Box(
        modifier = modifier.width(ComponentSize.callBalanceFloatProductWidth),
        contentAlignment = Alignment.TopCenter,
    ) {
        CallBalanceFloatProductCard(
            offer = offer,
            modifier = Modifier.padding(
                top = if (hasOff) Spacing.callBalanceFloatOffOverhang else 0.dp,
            ),
        )
        offer.offPercent?.takeIf { it.isNotBlank() }?.let { percent ->
            CallBalanceFloatOffBadge(
                percent = percent,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun CallBalanceFloatTimerChip(
    countdown: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(ComponentSize.callBalanceFloatTimerWidth)
            .height(ComponentSize.callBalanceFloatTimerHeight),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.call_balance_float_timer_ribbon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.callBalanceFloatIconGap),
        ) {
            Image(
                painter = painterResource(R.drawable.call_balance_float_ic_timer),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.callBalanceFloatTimerIcon),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = countdown,
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.callBalanceFloatMeta,
                lineHeight = TextSize.callBalanceFloatMetaLine,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                style = TextStyle(
                    shadow = Shadow(
                        color = DemoColors.scrim,
                        blurRadius = 0.9f,
                    ),
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = CallBalanceFloatTightLineHeight,
                ),
            )
        }
    }
}

@Composable
private fun CallBalanceFloatVipTitle(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(ComponentSize.callBalanceFloatTitleRowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.callBalanceFloatIconGap),
    ) {
        Text(
            text = stringResource(R.string.call_balance_offer_upgrade),
            color = DemoColors.textPrimary,
            fontSize = TextSize.callBalanceFloatTitle,
            lineHeight = TextSize.callBalanceFloatTitleLine,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = CallBalanceFloatTightLineHeight,
            ),
        )
        Text(
            text = stringResource(R.string.call_balance_offer_vip),
            fontSize = TextSize.callBalanceFloatVip,
            lineHeight = TextSize.callBalanceFloatVipLine,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            maxLines = 1,
            style = TextStyle(
                brush = DemoGradients.callBalanceFloatVipTitle,
                shadow = Shadow(
                    color = DemoColors.storeVipCardShadow,
                    blurRadius = 2f,
                ),
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = CallBalanceFloatTightLineHeight,
            ),
        )
    }
}

/** Figma `502:3734` — "More" + gold italic "Coins". */
@Composable
private fun CallBalanceFloatCoinTitle(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(ComponentSize.callBalanceFloatTitleRowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.callBalanceFloatIconGap),
    ) {
        Text(
            text = stringResource(R.string.call_balance_offer_more),
            color = DemoColors.textPrimary,
            fontSize = TextSize.callBalanceFloatTitle,
            lineHeight = TextSize.callBalanceFloatTitleLine,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = CallBalanceFloatTightLineHeight,
            ),
        )
        Text(
            text = stringResource(R.string.call_balance_offer_coins),
            fontSize = TextSize.callBalanceFloatVip,
            lineHeight = TextSize.callBalanceFloatVipLine,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = TextSize.callBalanceFloatCoinsTracking,
            maxLines = 1,
            style = TextStyle(
                brush = DemoGradients.callBalanceFloatVipTitle,
                shadow = Shadow(
                    color = DemoColors.storeVipCardShadow,
                    blurRadius = 2f,
                ),
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = CallBalanceFloatTightLineHeight,
            ),
        )
    }
}

@Composable
private fun CallBalanceFloatOffBadge(
    percent: String,
    modifier: Modifier = Modifier,
) {
    val labeled = buildAnnotatedString {
        withStyle(
            SpanStyle(
                brush = DemoGradients.callBalanceFloatOffPercent,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSize = TextSize.callBalanceFloatMeta,
            ),
        ) {
            append(percent)
        }
        withStyle(
            SpanStyle(
                color = DemoColors.onPrimaryButton,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSize = TextSize.callBalanceFloatMeta,
            ),
        ) {
            append(" ")
            append(stringResource(R.string.call_balance_offer_off_label))
        }
    }
    Box(
        modifier = modifier
            .width(ComponentSize.callBalanceFloatOffWidth)
            .height(ComponentSize.callBalanceFloatOffHeight),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.call_balance_float_off_badge),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Text(
            text = labeled,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = DemoColors.storeVipCardShadow,
                    blurRadius = 1.7f,
                ),
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = CallBalanceFloatTightLineHeight,
            ),
        )
    }
}

@Composable
private fun CallBalanceFloatProductCard(
    offer: CallBalanceOffer,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(ComponentSize.callBalanceFloatProductRadius)
    Box(
        modifier = modifier
            .width(ComponentSize.callBalanceFloatProductWidth)
            .height(ComponentSize.callBalanceFloatProductHeight)
            .shadow(
                elevation = ComponentSize.callBalanceFloatElevation,
                shape = shape,
                ambientColor = DemoColors.storeVipCardShadow,
                spotColor = DemoColors.storeVipCardShadow,
            )
            .clip(shape)
            .background(DemoGradients.callBalanceFloatProduct)
            .border(ComponentSize.meCardStroke, DemoColors.storeVipCardBorder, shape),
    ) {
        if (offer.showVipTitle) {
            CallBalanceFloatVipProductContent(offer = offer)
        } else {
            CallBalanceFloatCoinProductContent(amount = offer.displayCoinAmount)
        }
    }
}

@Composable
private fun CallBalanceFloatVipProductContent(offer: CallBalanceOffer) {
    val coins = offer.giveCoins.takeIf { it > 0 } ?: offer.baseCoins.takeIf { it > 0 } ?: 0
    Box(modifier = Modifier.fillMaxSize()) {
        if (coins > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Spacing.callBalanceFloatCoinsRowTop),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.callBalanceFloatIconGap),
            ) {
                Text(
                    text = stringResource(R.string.call_balance_offer_plus_coins, coins),
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.callBalanceFloatBody,
                    lineHeight = TextSize.callBalanceFloatBodyLine,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = CallBalanceFloatTightLineHeight,
                    ),
                )
                Image(
                    painter = painterResource(StoreR.drawable.store_ic_coin),
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.callBalanceFloatBenefitIcon),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        if (offer.matchCount > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Spacing.callBalanceFloatMatchRowTop),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.callBalanceFloatIconGap),
            ) {
                Text(
                    text = stringResource(R.string.call_balance_offer_plus),
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.callBalanceFloatBody,
                    lineHeight = TextSize.callBalanceFloatBodyLine,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = CallBalanceFloatTightLineHeight,
                    ),
                )
                Image(
                    painter = painterResource(StoreR.drawable.store_ic_vip_match),
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.callBalanceFloatBenefitIcon),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    text = stringResource(
                        R.string.call_balance_offer_match_x,
                        offer.matchCount,
                    ),
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.callBalanceFloatBody,
                    lineHeight = TextSize.callBalanceFloatBodyLine,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = CallBalanceFloatTightLineHeight,
                    ),
                )
            }
        }
    }
}

/** Figma `502:3737` — coin glyph above amount. */
@Composable
private fun CallBalanceFloatCoinProductContent(amount: Int) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(StoreR.drawable.store_ic_coin),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Spacing.callBalanceFloatCoinIconTop)
                .size(ComponentSize.callBalanceFloatCoinIcon),
            contentScale = ContentScale.Fit,
        )
        if (amount > 0) {
            Text(
                text = amount.toString(),
                color = DemoColors.textPrimary,
                fontSize = TextSize.callBalanceFloatBody,
                lineHeight = TextSize.callBalanceFloatBodyLine,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = CallBalanceFloatTightLineHeight,
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Spacing.callBalanceFloatCoinAmountTop),
            )
        }
    }
}

@Composable
private fun CallBalanceFloatPriceButton(
    price: String,
    originalPrice: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(ComponentSize.callBalanceFloatCtaWidth)
            .height(ComponentSize.callBalanceFloatCtaHeight)
            .shadow(
                elevation = ComponentSize.callBalanceFloatElevation,
                shape = RoundedCornerShape(Radius.pill),
                ambientColor = DemoColors.storeVipCardShadow,
                spotColor = DemoColors.storeVipCardShadow,
            )
            .clip(RoundedCornerShape(Radius.pill))
            .background(DemoGradients.primaryButton)
            .padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        originalPrice?.takeIf { it.isNotBlank() }?.let { original ->
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = original,
                    color = DemoColors.onPrimaryButton.copy(alpha = 0.7f),
                    fontSize = TextSize.callBalanceFloatMeta,
                    lineHeight = TextSize.callBalanceFloatMetaLine,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = CallBalanceFloatTightLineHeight,
                    ),
                )
                Spacer(
                    modifier = Modifier
                        .width(ComponentSize.callBalanceFloatStrikeWidth)
                        .height(ComponentSize.meCardStroke)
                        .background(DemoColors.onPrimaryButton.copy(alpha = 0.7f)),
                )
            }
            Spacer(modifier = Modifier.width(Spacing.callBalanceFloatPriceGap))
        }
        Text(
            text = price,
            color = DemoColors.onPrimaryButton,
            fontSize = TextSize.callBalanceFloatBody,
            lineHeight = TextSize.callBalanceFloatBodyLine,
            fontStyle = FontStyle.Italic,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = CallBalanceFloatTightLineHeight,
            ),
        )
    }
}

@Preview
@PreviewFontScale
@Composable
private fun CallBalanceAlertFloatingWindowVipPreview() {
    DemoTheme {
        CallBalanceAlertFloatingWindow(
            offer = CallBalanceOffer(
                roomKey = "1",
                variant = CallBalanceOfferVariant.NonVip,
                remainingSeconds = 12 * 3600 + 12 * 60 + 43,
                price = "$2.99",
                originalPrice = "$5.99",
                offPercent = "50%",
                giveCoins = 123,
                matchCount = 1,
                baseCoins = 0,
            ),
            onClick = {},
            onMoreOptions = {},
        )
    }
}

@Preview
@Composable
private fun CallBalanceAlertFloatingWindowCoinPreview() {
    DemoTheme {
        CallBalanceAlertFloatingWindow(
            offer = CallBalanceOffer(
                roomKey = "1",
                variant = CallBalanceOfferVariant.Vip,
                remainingSeconds = 12 * 3600 + 12 * 60 + 43,
                price = "$2.99",
                originalPrice = "$5.99",
                offPercent = "50%",
                giveCoins = 0,
                matchCount = 0,
                baseCoins = 123,
            ),
            onClick = {},
            onMoreOptions = {},
        )
    }
}
