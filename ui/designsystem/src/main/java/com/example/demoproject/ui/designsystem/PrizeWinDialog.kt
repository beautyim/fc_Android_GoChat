package com.example.demoproject.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

private val PrizeTightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private fun prizeTextStyle(
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontWeight: FontWeight,
    color: Color,
): TextStyle = TextStyle(
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    lineHeight = lineHeight,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = PrizeTightLineHeight,
)

/**
 * Prize-win / claim popup — Figma 1:5698「中奖弹窗」.
 *
 * Coin-bag icons load from backend URLs; [R.drawable.treasure_ic_coin] is
 * placeholder / error fallback only.
 *
 * Content under the hero: coins on top; price strip paints above the pink wash;
 * wash is rounded, extended 20dp below the Figma plate, and Claim sits at the bottom.
 */
@Composable
fun PrizeWinDialog(
    baseCoins: Int,
    bonusCoins: Int,
    originalPrice: String,
    salePrice: String,
    onClaim: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    baseCoinIconUrl: String? = null,
    bonusCoinIconUrl: String? = null,
    dismissOnScrim: Boolean = true,
) {
    val closeCd = stringResource(R.string.prize_cd_close)
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
                .widthIn(max = ComponentSize.prizeDialogWidth)
                .fillMaxWidth()
                .aspectRatio(ComponentSize.prizeDialogAspect)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Image(
                painter = painterResource(R.drawable.prize_bg_hero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(Radius.lg)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = ComponentSize.prizeCoinBlockTop),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.width(ComponentSize.prizeCoinRowWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PrizeCoinRow(
                        amount = baseCoins,
                        amountColor = DemoColors.prizeBody,
                        amountSize = TextSize.prizeBaseCoins,
                        amountLineHeight = TextSize.prizeBaseCoinsLine,
                        showPlus = false,
                        iconUrl = baseCoinIconUrl,
                        iconSize = ComponentSize.prizeBaseCoinIcon,
                        iconToText = Spacing.prizeBaseIconToText,
                        showDashedDivider = true,
                    )
                    Spacer(modifier = Modifier.height(Spacing.prizeCoinRowGap))
                    PrizeCoinRow(
                        amount = bonusCoins,
                        amountColor = DemoColors.prizeAccent,
                        amountSize = TextSize.prizeBonusCoins,
                        amountLineHeight = TextSize.prizeBonusCoinsLine,
                        showPlus = true,
                        iconUrl = bonusCoinIconUrl ?: baseCoinIconUrl,
                        iconSize = ComponentSize.prizeBonusCoinIcon,
                        iconToText = Spacing.prizeBonusIconToText,
                        showDashedDivider = false,
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.prizeCoinsToPrice))
                // Price paints above the pink wash; wash sits under and extends below.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = ComponentSize.prizeCtaCoverHorizontal)
                            .offset(
                                y = ComponentSize.prizePriceStripHeight +
                                    Spacing.prizePriceToCover -
                                    Spacing.prizeCtaCoverLift,
                            )
                            .height(ComponentSize.prizeCtaCoverHeight)
                            .clip(RoundedCornerShape(Radius.md))
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithCache {
                                val fadePx = Spacing.prizeCtaCoverEdgeFade.toPx()
                                val solid = DemoColors.prizeCtaCover
                                val edge = DemoColors.prizeCtaCoverEdge
                                val ty = (fadePx / size.height).coerceIn(0f, 0.5f)
                                val tx = (fadePx / size.width).coerceIn(0f, 0.5f)
                                val vertical = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to edge,
                                        ty to solid,
                                        1f - ty to solid,
                                        1f to edge,
                                    ),
                                )
                                val horizontalMask = Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0f to Color.Transparent,
                                        tx to Color.Black,
                                        1f - tx to Color.Black,
                                        1f to Color.Transparent,
                                    ),
                                )
                                onDrawBehind {
                                    drawRect(brush = vertical)
                                    drawRect(
                                        brush = horizontalMask,
                                        blendMode = BlendMode.DstIn,
                                    )
                                }
                            },
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(
                                start = ComponentSize.prizePriceStripStart,
                                end = ComponentSize.prizePriceStripEnd,
                            )
                            .height(ComponentSize.prizePriceStripHeight)
                            .clip(
                                RoundedCornerShape(
                                    bottomStart = Radius.md,
                                    bottomEnd = Radius.md,
                                ),
                            )
                            .background(DemoColors.prizePriceStrip),
                        contentAlignment = Alignment.Center,
                    ) {
                        PrizePriceRow(
                            originalPrice = originalPrice,
                            salePrice = salePrice,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = ComponentSize.prizeCtaHorizontal)
                            .padding(bottom = ComponentSize.prizeCtaBottom)
                            .height(ComponentSize.prizeCtaHeight)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(DemoGradients.prizeCta)
                            .clickable(
                                role = Role.Button,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color.White),
                                onClick = onClaim,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.prize_cta_claim),
                            style = prizeTextStyle(
                                fontSize = TextSize.prizeCta,
                                lineHeight = TextSize.prizeCtaLine,
                                fontWeight = FontWeight.Black,
                                color = DemoColors.onPrimaryButton,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            autoSize = DemoTextAutoSize.label(TextSize.prizeCta),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.sm),
                        )
                    }
                }
            }
            // Close glyph is baked into the hero; hit target only.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(ComponentSize.prizeCloseInset)
                    .size(ComponentSize.prizeClose)
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
}

@Composable
private fun PrizeCoinRow(
    amount: Int,
    amountColor: Color,
    amountSize: TextUnit,
    amountLineHeight: TextUnit,
    showPlus: Boolean,
    iconUrl: String?,
    iconSize: Dp,
    iconToText: Dp,
    showDashedDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    // Dashed divider is drawn on the bottom edge (Figma border-bottom) and must
    // not add layout height, or the bonus row collides with the price strip.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.prizeCoinFrameHeight),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = Spacing.prizeCoinRowVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            PrizeRemoteCoinIcon(
                iconUrl = iconUrl,
                size = iconSize,
            )
            Spacer(modifier = Modifier.width(iconToText))
            Text(
                text = buildAnnotatedString {
                    if (showPlus) {
                        withStyle(
                            SpanStyle(
                                color = amountColor,
                                fontSize = amountSize,
                                fontWeight = FontWeight.Black,
                            ),
                        ) {
                            append("+")
                            append(amount.coerceAtLeast(0).toString())
                        }
                    } else {
                        withStyle(
                            SpanStyle(
                                color = amountColor,
                                fontSize = amountSize,
                                fontWeight = FontWeight.Black,
                            ),
                        ) {
                            append(amount.coerceAtLeast(0).toString())
                        }
                    }
                    append(" ")
                    withStyle(
                        SpanStyle(
                            color = DemoColors.prizeBody,
                            fontSize = TextSize.prizeCoinsUnit,
                            fontWeight = FontWeight.Black,
                        ),
                    ) {
                        append(stringResource(R.string.prize_coins_unit))
                    }
                },
                style = TextStyle(
                    lineHeight = amountLineHeight,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = PrizeTightLineHeight,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = DemoTextAutoSize.price(amountSize),
            )
        }
        if (showDashedDivider) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(ComponentSize.prizeStrikeStroke),
            ) {
                drawLine(
                    color = DemoColors.prizeDashedDivider,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(8f, 6f),
                        0f,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PrizePriceRow(
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
                style = prizeTextStyle(
                    fontSize = TextSize.prizeOriginalPrice,
                    lineHeight = TextSize.prizeOriginalPriceLine,
                    fontWeight = FontWeight.SemiBold,
                    color = DemoColors.prizeOriginalPrice,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = DemoTextAutoSize.price(TextSize.prizeOriginalPrice),
                textAlign = TextAlign.Center,
            )
            Canvas(
                modifier = Modifier
                    .width(ComponentSize.prizeStrikeWidth)
                    .height(ComponentSize.prizeStrikeStroke)
                    .rotate(-9.12f),
            ) {
                drawRect(color = DemoColors.prizeStrike)
            }
        }
        Spacer(modifier = Modifier.width(Spacing.prizePriceGap))
        Text(
            text = salePrice,
            style = prizeTextStyle(
                fontSize = TextSize.prizeSalePrice,
                lineHeight = TextSize.prizeSalePriceLine,
                fontWeight = FontWeight.Black,
                color = DemoColors.prizeAccent,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = DemoTextAutoSize.price(TextSize.prizeSalePrice),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PrizeRemoteCoinIcon(
    iconUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val fallback = painterResource(R.drawable.treasure_ic_coin)
    val iconModifier = modifier.size(size)
    if (iconUrl.isNullOrBlank()) {
        Image(
            painter = fallback,
            contentDescription = null,
            modifier = iconModifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(iconUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            placeholder = fallback,
            error = fallback,
            modifier = iconModifier,
            contentScale = ContentScale.Fit,
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun PrizeWinDialogPreview() {
    DemoTheme {
        PrizeWinDialog(
            baseCoins = 124,
            bonusCoins = 37,
            originalPrice = "$63.95",
            salePrice = "$15.99",
            onClaim = {},
            onDismiss = {},
        )
    }
}
