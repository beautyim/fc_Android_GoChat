package com.example.demoproject.product.store

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

/**
 * Call hangup-risk offer guide. Background reuses `store_guide_bg`;
 * promo cards reuse store VIP / sale carousel cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallBalanceOfferGuideSheet(
    state: CallBalanceOfferGuideUiState,
    onDismiss: () -> Unit,
    onContinueCall: () -> Unit,
    onMoreOptions: () -> Unit,
    onPurchaseVip: (Long) -> Unit,
    onPurchaseSale: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RectangleShape,
        containerColor = Color.Transparent,
        scrimColor = DemoColors.scrim,
        dragHandle = null,
        sheetGesturesEnabled = false,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CallBalanceOfferGuideContent(
                state = state,
                onDismiss = onDismiss,
                onContinueCall = onContinueCall,
                onMoreOptions = onMoreOptions,
                onPurchaseVip = onPurchaseVip,
                onPurchaseSale = onPurchaseSale,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(DemoColors.coinGuideSheetBottom),
            )
        }
    }
}

@Composable
internal fun CallBalanceOfferGuideContent(
    state: CallBalanceOfferGuideUiState,
    onDismiss: () -> Unit,
    onContinueCall: () -> Unit,
    onMoreOptions: () -> Unit,
    onPurchaseVip: (Long) -> Unit,
    onPurchaseSale: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeCd = stringResource(R.string.store_guide_cd_close)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = ComponentSize.callBalanceOfferGuideMaxHeight),
    ) {
        Image(
            painter = painterResource(R.drawable.store_guide_bg),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.coinGuideBalanceTop)
                    .padding(horizontal = Spacing.coinGuideTextInset)
                    .padding(end = ComponentSize.coinGuideCloseChip),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.coinGuideBalanceIconGap),
                ) {
                    Text(
                        text = stringResource(R.string.store_guide_my_coins, state.balance),
                        color = DemoColors.textPrimary,
                        fontSize = TextSize.md,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Image(
                        painter = painterResource(R.drawable.store_ic_coin),
                        contentDescription = null,
                        modifier = Modifier.size(ComponentSize.coinGuideBalanceCoin),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.coinGuideBalanceToHint))
                CallBalanceOfferHangupHint(remainingSeconds = state.remainingSeconds)
                Spacer(modifier = Modifier.height(Spacing.coinGuideHintToSale))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md)
                    .padding(bottom = Spacing.coinGuideBottom),
            ) {
                if (!state.isCatalogEmpty) {
                    StorePromoCarousel(
                        vipOffers = state.vipOffers,
                        saleOffers = state.saleOffers,
                        purchasingOfferId = state.purchasingOfferId,
                        onPurchaseVip = onPurchaseVip,
                        onPurchaseSale = onPurchaseSale,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.md))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComponentSize.callBalanceOfferGuideCtaHeight)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(DemoGradients.primaryButton)
                        .clickable(
                            role = Role.Button,
                            enabled = state.purchasingOfferId == null && !state.isCatalogEmpty,
                            onClick = onContinueCall,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.call_balance_offer_continue_cta),
                        color = DemoColors.onPrimaryButton,
                        fontSize = TextSize.md,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.callBalanceOfferGuideCtaToMore))
                Text(
                    text = stringResource(R.string.call_balance_offer_more_options),
                    color = DemoColors.callBalanceOfferMoreOptions,
                    fontSize = TextSize.sm,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.Button,
                            onClick = onMoreOptions,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        )
                        .padding(bottom = Spacing.callBalanceOfferGuideMoreBottom),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = Spacing.coinGuideCloseTop, end = Spacing.coinGuideCloseEnd)
                .size(ComponentSize.coinGuideCloseChip)
                .shadow(
                    elevation = ComponentSize.coinGuideCloseElevation,
                    shape = CircleShape,
                    ambientColor = DemoColors.coinGuideCloseShadow,
                    spotColor = DemoColors.coinGuideCloseShadow,
                )
                .clip(CircleShape)
                .background(DemoColors.coinGuideCloseChip)
                .clickable(
                    role = Role.Button,
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                )
                .semantics { contentDescription = closeCd },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.store_guide_ic_close),
                contentDescription = null,
                modifier = Modifier
                    .width(ComponentSize.coinGuideCloseIconWidth)
                    .height(ComponentSize.coinGuideCloseIconHeight),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun CallBalanceOfferHangupHint(
    remainingSeconds: Int,
    modifier: Modifier = Modifier,
) {
    // Figma 1:4198 — 14sp / leading normal; spaces are inserted in code so XML trim
    // cannot drop the gap before the red seconds span.
    val secondsLabel = stringResource(
        R.string.call_balance_offer_seconds,
        remainingSeconds.coerceAtLeast(0),
    ).trim()
    val prefix = stringResource(R.string.call_balance_offer_will_end_prefix).trimEnd()
    val suffix = stringResource(R.string.call_balance_offer_will_end_suffix).trimStart()
    val annotated = buildAnnotatedString {
        append(prefix)
        append('\u0020')
        withStyle(SpanStyle(color = DemoColors.callBalanceOfferHangupSeconds)) {
            append(secondsLabel)
        }
        if (suffix.isNotEmpty() && suffix.first() != '.') {
            append('\u0020')
        }
        append(suffix)
    }
    Text(
        text = annotated,
        color = DemoColors.textSecondary,
        fontSize = TextSize.sm,
        lineHeight = TextSize.smLine,
        fontWeight = FontWeight.Normal,
        style = TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@PreviewScreenSizes
@PreviewFontScale
@Preview
@Composable
private fun CallBalanceOfferGuidePreview() {
    DemoTheme {
        CallBalanceOfferGuideContent(
            state = CallBalanceOfferGuideUiState(
                balance = 3,
                remainingSeconds = 15,
                vipOffers = listOf(
                    StoreVipOfferUi(
                        id = 1,
                        sku = "vip.1",
                        title = "Month",
                        month = 1,
                        price = "$3.33",
                        originalPrice = "$13.33",
                        giveCoins = 123,
                        matchCount = 1,
                        badgeLabel = "Try Now",
                    ),
                ),
            ),
            onDismiss = {},
            onContinueCall = {},
            onMoreOptions = {},
            onPurchaseVip = {},
            onPurchaseSale = {},
        )
    }
}
