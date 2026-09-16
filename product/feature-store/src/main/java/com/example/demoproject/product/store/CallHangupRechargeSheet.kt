package com.example.demoproject.product.store

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

/**
 * Call ended for insufficient balance — peer header + store sale/coin catalog.
 *
 * Background: `store_hangup_recharge_bg` (Figma `1 1113`).
 * Cards reuse [StoreSaleCard] / [StoreCoinCard].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHangupRechargeSheet(
    state: CallHangupRechargeUiState,
    onDismiss: () -> Unit,
    onPurchaseCoin: (Long) -> Unit,
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
            CallHangupRechargeContent(
                state = state,
                onDismiss = onDismiss,
                onPurchaseCoin = onPurchaseCoin,
                onPurchaseSale = onPurchaseSale,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(DemoColors.callHangupSheetBottom),
            )
        }
    }
}

@Composable
internal fun CallHangupRechargeContent(
    state: CallHangupRechargeUiState,
    onDismiss: () -> Unit,
    onPurchaseCoin: (Long) -> Unit,
    onPurchaseSale: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeCd = stringResource(R.string.store_guide_cd_close)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = ComponentSize.callHangupRechargeMaxHeight),
    ) {
        // Figma 1:5167 — #EADFF2 only under the opaque lower sheet, not the wavy top.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(ComponentSize.callHangupSheetBottomFill)
                .background(DemoColors.callHangupSheetBottom),
        )
        Image(
            painter = painterResource(R.drawable.store_hangup_recharge_bg),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        Column(modifier = Modifier.fillMaxSize()) {
            CallHangupRechargeHeader(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.callHangupRechargeHeaderTop)
                    .padding(horizontal = Spacing.md)
                    .padding(end = ComponentSize.coinGuideCloseChip),
            )
            Spacer(modifier = Modifier.height(Spacing.callHangupHeaderToTitle))
            Text(
                text = stringResource(R.string.call_hangup_recharge_title),
                color = DemoColors.textPrimary,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
            )
            Spacer(modifier = Modifier.height(Spacing.callHangupTitleToSale))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = Spacing.coinGuideBottom),
            ) {
                CallHangupRechargeCatalog(
                    state = state,
                    onPurchaseCoin = onPurchaseCoin,
                    onPurchaseSale = onPurchaseSale,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        CallHangupCloseChip(
            contentDescription = closeCd,
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = Spacing.coinGuideCloseTop, end = Spacing.coinGuideCloseEnd),
        )
    }
}

@Composable
private fun CallHangupRechargeHeader(
    state: CallHangupRechargeUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.callHangupAvatarToMeta),
    ) {
        CallHangupPeerAvatar(
            avatarUrl = state.peerAvatarUrl,
            modifier = Modifier.size(ComponentSize.callHangupAvatar),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.callHangupNameToSpeech),
        ) {
            Text(
                text = state.displayName,
                color = DemoColors.textPrimary,
                fontSize = TextSize.callHangupPeerName,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            CallHangupSpeechBubble(
                text = stringResource(R.string.call_hangup_recharge_speech),
                modifier = Modifier.widthIn(max = 220.dp),
            )
        }
    }
}

@Composable
internal fun CallHangupPeerAvatar(
    avatarUrl: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .border(
                width = ComponentSize.callHangupAvatarBorder,
                color = DemoColors.callHangupAvatarBorder,
                shape = CircleShape,
            )
            .padding(ComponentSize.callHangupAvatarBorder)
            .clip(CircleShape)
            .background(DemoColors.coinGuideSheetBottom),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl.takeIf { it.isNotBlank() })
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun CallHangupSpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = Spacing.xs)
                .size(ComponentSize.callHangupSpeechTail)
                .rotate(45f)
                .background(DemoColors.coinGuideCloseChip),
        )
        Text(
            text = text,
            color = DemoColors.callHangupSpeechText,
            fontSize = TextSize.callHangupSpeech,
            lineHeight = TextSize.callHangupSpeechLine,
            letterSpacing = TextSize.callHangupSpeechTracking,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .padding(start = Spacing.sm)
                .shadow(
                    elevation = Spacing.xs,
                    shape = RoundedCornerShape(Radius.callHangupSpeech),
                    ambientColor = DemoColors.callHangupSpeechShadow,
                    spotColor = DemoColors.callHangupSpeechShadow,
                )
                .clip(RoundedCornerShape(Radius.callHangupSpeech))
                .background(DemoColors.coinGuideCloseChip)
                .padding(
                    horizontal = Spacing.callHangupSpeechHorizontal,
                    vertical = Spacing.callHangupSpeechVertical,
                ),
        )
    }
}

@Composable
private fun CallHangupRechargeCatalog(
    state: CallHangupRechargeUiState,
    onPurchaseCoin: (Long) -> Unit,
    onPurchaseSale: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSale = state.saleOffers.isNotEmpty()
    val tallCards = state.coinOffers.any { !it.cornerBadge.isNullOrBlank() }
    val coinRows = remember(state.coinOffers) {
        state.coinOffers.chunked(ComponentSize.storeCoinColumns)
    }
    Column(modifier = modifier) {
        if (hasSale) {
            CallHangupSaleCarousel(
                saleOffers = state.saleOffers,
                purchasingOfferId = state.purchasingOfferId,
                onPurchaseSale = onPurchaseSale,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
            )
        }
        if (state.coinOffers.isEmpty()) {
            if (!hasSale && !state.isLoading) {
                Text(
                    text = stringResource(R.string.store_empty),
                    color = DemoColors.textSecondary,
                    fontSize = TextSize.sm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.lg),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md)
                    .padding(
                        top = if (hasSale) {
                            Spacing.storeVipToPager + Spacing.storePagerToSectionExtra
                        } else {
                            0.dp
                        },
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.storeCoinRowGap),
            ) {
                coinRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.storeCoinColumnGap),
                    ) {
                        row.forEach { offer ->
                            StoreCoinCard(
                                offer = offer,
                                enabled = state.purchasingOfferId == null,
                                isLoading = state.purchasingOfferId == offer.id,
                                tall = tallCards,
                                onPurchase = { onPurchaseCoin(offer.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(ComponentSize.storeCoinColumns - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallHangupSaleCarousel(
    saleOffers: List<StoreSaleOfferUi>,
    purchasingOfferId: Long?,
    onPurchaseSale: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { saleOffers.size.coerceAtLeast(1) })
    val hasDots = saleOffers.size > 1
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = Spacing.storePromoPageGap,
            userScrollEnabled = hasDots,
        ) { page ->
            val offer = saleOffers[page]
            StoreSaleCard(
                offer = offer,
                enabled = purchasingOfferId == null,
                isLoading = purchasingOfferId == offer.id,
                onClick = { onPurchaseSale(offer.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (hasDots) {
            Spacer(modifier = Modifier.height(Spacing.storeVipToPager))
            StorePagerDots(
                pageCount = saleOffers.size,
                currentPage = pagerState.currentPage,
            )
        }
    }
}

@Composable
internal fun CallHangupCloseChip(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            )
            .semantics { this.contentDescription = contentDescription },
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

@PreviewScreenSizes
@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun CallHangupRechargePreview() {
    DemoTheme {
        CallHangupRechargeContent(
            state = CallHangupRechargeUiState(
                peerNickname = "Isabella",
                peerAge = 23,
                saleOffers = listOf(
                    StoreSaleOfferUi(
                        id = 1,
                        sku = "sale.1",
                        baseCoins = 3333,
                        bonusCoins = 123,
                        price = "$3.33",
                        originalPrice = "$13.33",
                        discountPercent = "72%",
                        badgeLabel = "Super Discounts",
                    ),
                ),
                coinOffers = listOf(
                    StoreCoinOfferUi(
                        id = 2,
                        sku = "coin.1",
                        coinAmount = 98,
                        price = "$1.89",
                        originalPrice = "$3.04",
                        discountLabel = "38% OFF",
                        cornerBadge = "Most Popular",
                        iconUrl = null,
                    ),
                    StoreCoinOfferUi(
                        id = 3,
                        sku = "coin.2",
                        coinAmount = 98,
                        price = "$1.89",
                        originalPrice = "$3.04",
                        discountLabel = "38% OFF",
                        cornerBadge = "Most Popular",
                        iconUrl = null,
                    ),
                ),
            ),
            onDismiss = {},
            onPurchaseCoin = {},
            onPurchaseSale = {},
        )
    }
}
