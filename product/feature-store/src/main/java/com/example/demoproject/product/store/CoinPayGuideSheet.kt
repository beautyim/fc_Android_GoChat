package com.example.demoproject.product.store

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

private val CoinPayGuideSheetShape = RectangleShape

/**
 * Insufficient-balance coin recharge guide.
 *
 * Dismiss only via the close chip or scrim tap — sheet drag gestures are disabled.
 * Sale / coin cards reuse the store page composables.
 * Silhouette comes from `store_guide_bg` (Figma `弹窗背景 1`) alpha, not a rounded clip.
 * Draws edge-to-edge under the system nav bar and paints that strip with the sheet
 * bottom color so it is not transparent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinPayGuideSheet(
    state: CoinPayGuideUiState,
    onDismiss: () -> Unit,
    onPurchaseCoin: (Long) -> Unit,
    onPurchaseSale: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = CoinPayGuideSheetShape,
        containerColor = Color.Transparent,
        scrimColor = DemoColors.scrim,
        dragHandle = null,
        sheetGesturesEnabled = false,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CoinPayGuideSheetContent(
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
                    .background(DemoColors.coinGuideSheetBottom),
            )
        }
    }
}

@Composable
internal fun CoinPayGuideSheetContent(
    state: CoinPayGuideUiState,
    onDismiss: () -> Unit,
    onPurchaseCoin: (Long) -> Unit,
    onPurchaseSale: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeCd = stringResource(R.string.store_guide_cd_close)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = ComponentSize.coinGuideSheetMaxHeight),
    ) {
        CoinPayGuideBackground(modifier = Modifier.matchParentSize())
        Column(modifier = Modifier.fillMaxSize()) {
            // Sticky title block — does not scroll with the catalog.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.coinGuideBalanceTop),
            ) {
                CoinPayGuideHeader(
                    balance = state.balance,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.coinGuideTextInset)
                        .padding(end = ComponentSize.coinGuideCloseChip),
                )
                Spacer(modifier = Modifier.height(Spacing.coinGuideHintToSale))
            }
            // Only sale carousel + coin packages scroll.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = Spacing.coinGuideBottom),
            ) {
                if (state.isLoading && state.isCatalogEmpty) {
                    CoinPayGuideSkeleton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md),
                    )
                } else {
                    CoinPayGuideCatalog(
                        state = state,
                        onPurchaseCoin = onPurchaseCoin,
                        onPurchaseSale = onPurchaseSale,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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

/**
 * Full-bleed Figma `弹窗背景 1` (`store_guide_bg`): wavy top + hearts are part of the
 * asset alpha. Do not crop/offset or clip to a rounded rect — that hides the silhouette.
 */
@Composable
private fun CoinPayGuideBackground(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.store_guide_bg),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier,
    )
}

@Composable
private fun CoinPayGuideHeader(
    balance: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.coinGuideBalanceIconGap),
        ) {
            Text(
                text = stringResource(R.string.store_guide_my_coins, balance),
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
        Text(
            text = stringResource(R.string.store_guide_low_balance),
            color = DemoColors.textSecondary,
            fontSize = TextSize.sm,
            fontWeight = FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CoinPayGuideCatalog(
    state: CoinPayGuideUiState,
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
            CoinPayGuideSaleCarousel(
                saleOffers = state.saleOffers,
                purchasingOfferId = state.purchasingOfferId,
                onPurchaseSale = onPurchaseSale,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
            )
        }
        Text(
            text = stringResource(R.string.store_choose_coins),
            color = DemoColors.textPrimary,
            fontSize = TextSize.sm,
            fontWeight = FontWeight.SemiBold,
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
        )
        if (state.coinOffers.isEmpty()) {
            Text(
                text = stringResource(R.string.store_empty),
                color = DemoColors.textSecondary,
                fontSize = TextSize.sm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.lg),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md)
                    .padding(top = Spacing.storeCoinRowGap),
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
private fun CoinPayGuideSaleCarousel(
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
private fun rememberCoinGuideSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "coinGuideSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "coinGuideSkeletonPulse",
    )
    return pulse
}

@Composable
private fun CoinPayGuideSkeleton(modifier: Modifier = Modifier) {
    val pulse = rememberCoinGuideSkeletonPulse()
    val fill = DemoColors.skeleton.copy(alpha = pulse)
    val highlight = DemoColors.skeletonHighlight.copy(alpha = pulse)
    val loadingCd = stringResource(R.string.store_cd_loading)
    val shape = RoundedCornerShape(Radius.sm)
    Column(
        modifier = modifier.semantics { contentDescription = loadingCd },
        verticalArrangement = Arrangement.spacedBy(Spacing.storeCoinRowGap),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.storeSaleCardHeight)
                .clip(shape)
                .background(fill),
        )
        Box(
            modifier = Modifier
                .width(ComponentSize.storeSkeletonSectionWidth)
                .height(ComponentSize.storeSkeletonSectionHeight)
                .clip(RoundedCornerShape(Radius.sm))
                .background(highlight),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.storeCoinColumnGap),
        ) {
            repeat(ComponentSize.storeCoinColumns) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(ComponentSize.storeCoinCardHeight)
                        .clip(RoundedCornerShape(Radius.storeCoinCard))
                        .border(
                            ComponentSize.meCardStroke,
                            DemoColors.storeCoinCardBorder,
                            RoundedCornerShape(Radius.storeCoinCard),
                        )
                        .background(DemoColors.storeCoinCardSurface),
                )
            }
        }
    }
}

private val PreviewGuideState = CoinPayGuideUiState(
    balance = 3,
    saleOffers = listOf(
        StoreSaleOfferUi(
            id = 21,
            sku = "sale_3333",
            baseCoins = 3333,
            bonusCoins = 123,
            price = "$3.33",
            originalPrice = "$13.33",
            discountPercent = "72%",
            badgeLabel = "Super Discounts",
        ),
        StoreSaleOfferUi(
            id = 22,
            sku = "sale_999",
            baseCoins = 999,
            bonusCoins = 50,
            price = "$1.99",
            originalPrice = "$4.99",
            discountPercent = "60%",
            badgeLabel = "Super Discounts",
        ),
    ),
    coinOffers = listOf(
        StoreCoinOfferUi(
            id = 1,
            sku = "coin_98",
            coinAmount = 98,
            price = "$1.89",
            originalPrice = "$3.04",
            discountLabel = "38% OFF",
            cornerBadge = "Most Popular",
            iconUrl = null,
        ),
        StoreCoinOfferUi(
            id = 2,
            sku = "coin_198",
            coinAmount = 198,
            price = "$3.49",
            originalPrice = "$5.99",
            discountLabel = "42% OFF",
            cornerBadge = "Most Popular",
            iconUrl = null,
        ),
        StoreCoinOfferUi(
            id = 3,
            sku = "coin_498",
            coinAmount = 498,
            price = "$7.99",
            originalPrice = "$12.99",
            discountLabel = "38% OFF",
            cornerBadge = null,
            iconUrl = null,
        ),
        StoreCoinOfferUi(
            id = 4,
            sku = "coin_998",
            coinAmount = 998,
            price = "$14.99",
            originalPrice = null,
            discountLabel = null,
            cornerBadge = null,
            iconUrl = null,
        ),
    ),
)

@PreviewScreenSizes
@PreviewFontScale
@Preview(showBackground = true, locale = "ar")
@Composable
private fun CoinPayGuideSheetPreview() {
    DemoTheme {
        CoinPayGuideSheetContent(
            state = PreviewGuideState,
            onDismiss = {},
            onPurchaseCoin = {},
            onPurchaseSale = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, name = "Coin guide skeleton")
@Composable
private fun CoinPayGuideSheetSkeletonPreview() {
    DemoTheme {
        CoinPayGuideSheetContent(
            state = CoinPayGuideUiState(isLoading = true, balance = 3),
            onDismiss = {},
            onPurchaseCoin = {},
            onPurchaseSale = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
