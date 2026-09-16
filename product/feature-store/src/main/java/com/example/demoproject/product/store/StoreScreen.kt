package com.example.demoproject.product.store

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoNavIconButton
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.currentWindowSize
import com.example.demoproject.ui.foundation.readableContentWidth
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.Canvas as ComposeCanvas

@Composable
fun StoreScreen(
    viewModel: StoreViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    val context = LocalContext.current

    DisposableEffect(activity) {
        viewModel.bindActivity(activity)
        onDispose { viewModel.bindActivity(null) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is StoreEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    StoreScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
    )
}

@Composable
fun StoreScreen(
    state: StoreUiState,
    onIntent: (StoreIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSize = currentWindowSize()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.sheet),
    ) {
        Image(
            painter = painterResource(R.drawable.store_bg_page),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.7f,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            StoreTopBar(
                balance = state.balance,
                onBack = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (windowSize.isCompactWidth) {
                            Modifier
                        } else {
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .readableContentWidth()
                        },
                    ),
            )
            when {
                state.isLoading && state.isCatalogEmpty -> {
                    StoreSkeleton(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (windowSize.isCompactWidth) {
                                    Modifier
                                } else {
                                    Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .readableContentWidth()
                                },
                            ),
                    )
                }
                state.errorMessage != null && state.isCatalogEmpty -> {
                    StoreErrorState(
                        message = state.errorMessage,
                        onRetry = { onIntent(StoreIntent.Refresh) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    StoreContent(
                        state = state,
                        onIntent = onIntent,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (windowSize.isCompactWidth) {
                                    Modifier
                                } else {
                                    Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .readableContentWidth()
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreTopBar(
    balance: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backCd = stringResource(R.string.store_cd_back)
    val balanceCd = stringResource(R.string.store_cd_balance)
    Box(
        modifier = modifier
            .height(ComponentSize.navHeaderHeight)
            .padding(horizontal = Spacing.md),
    ) {
        DemoNavIconButton(
            icon = painterResource(R.drawable.store_ic_back),
            contentDescription = backCd,
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
            iconSize = ComponentSize.storeNavIcon,
            mirrorInRtl = true,
        )
        Text(
            text = stringResource(R.string.store_title),
            color = DemoColors.navTitle,
            fontSize = TextSize.md,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.storeBalanceGap),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoColors.sheet)
                .padding(start = Spacing.sm, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs)
                .semantics { contentDescription = balanceCd },
        ) {
            Text(
                text = balance.toString(),
                color = DemoColors.textPrimary,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.SemiBold,
            )
            Image(
                painter = painterResource(R.drawable.store_ic_coin),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.storeBalanceCoin),
            )
            Image(
                painter = painterResource(R.drawable.store_ic_chevron),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.storeBalanceChevron),
            )
        }
    }
}

@Composable
private fun StoreContent(
    state: StoreUiState,
    onIntent: (StoreIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasVip = state.vipOffers.isNotEmpty()
    val hasSale = state.saleOffers.isNotEmpty()
    val hasCarousel = hasVip || hasSale
    LazyVerticalGrid(
        columns = GridCells.Fixed(ComponentSize.storeCoinColumns),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.storeNavToVip,
            bottom = Spacing.lg,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.storeCoinColumnGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.storeCoinRowGap),
    ) {
        if (hasCarousel) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "promo") {
                StorePromoCarousel(
                    vipOffers = state.vipOffers,
                    saleOffers = state.saleOffers,
                    purchasingOfferId = state.purchasingOfferId,
                    onPurchaseVip = { onIntent(StoreIntent.PurchaseVip(it)) },
                    onPurchaseSale = { onIntent(StoreIntent.PurchaseSale(it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "choose") {
            Text(
                text = stringResource(R.string.store_choose_coins),
                color = DemoColors.textPrimary,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = if (hasCarousel) Spacing.storePagerToSectionExtra else 0.dp,
                    ),
            )
        }
        if (state.coinOffers.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                Text(
                    text = stringResource(R.string.store_empty),
                    color = DemoColors.textSecondary,
                    fontSize = TextSize.sm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.lg),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // Every card takes the tallest variant so the grid rows stay flush.
            val tallCards = state.coinOffers.any { !it.cornerBadge.isNullOrBlank() }
            items(
                items = state.coinOffers,
                key = { it.id },
            ) { offer ->
                StoreCoinCard(
                    offer = offer,
                    enabled = state.purchasingOfferId == null,
                    isLoading = state.purchasingOfferId == offer.id,
                    tall = tallCards,
                    onPurchase = { onIntent(StoreIntent.PurchaseCoin(offer.id)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private sealed interface StorePromoPage {
    data class Vip(val offer: StoreVipOfferUi) : StorePromoPage
    data class Sale(val offer: StoreSaleOfferUi) : StorePromoPage
}

@Composable
internal fun StorePromoCarousel(
    vipOffers: List<StoreVipOfferUi>,
    saleOffers: List<StoreSaleOfferUi>,
    purchasingOfferId: Long?,
    onPurchaseVip: (Long) -> Unit,
    onPurchaseSale: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = remember(vipOffers, saleOffers) {
        buildList {
            vipOffers.forEach { add(StorePromoPage.Vip(it)) }
            saleOffers.forEach { add(StorePromoPage.Sale(it)) }
        }
    }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    val hasDots = pages.size > 1
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
            when (val item = pages[page]) {
                is StorePromoPage.Vip -> {
                    val offer = item.offer
                    StoreVipCard(
                        offer = offer,
                        enabled = purchasingOfferId == null,
                        isLoading = purchasingOfferId == offer.id,
                        onClick = { onPurchaseVip(offer.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is StorePromoPage.Sale -> {
                    val offer = item.offer
                    StoreSaleCard(
                        offer = offer,
                        enabled = purchasingOfferId == null,
                        isLoading = purchasingOfferId == offer.id,
                        onClick = { onPurchaseSale(offer.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (hasDots) {
            Spacer(modifier = Modifier.height(Spacing.storeVipToPager))
            StorePagerDots(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
            )
        }
    }
}

@Composable
internal fun StorePagerDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val active = index == currentPage
            Box(
                modifier = Modifier
                    .height(ComponentSize.storePagerDot)
                    .width(
                        if (active) {
                            ComponentSize.storePagerActiveWidth
                        } else {
                            ComponentSize.storePagerDot
                        },
                    )
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(
                        if (active) DemoColors.link else DemoColors.storePagerInactive,
                    ),
            )
        }
    }
}

@Composable
internal fun StoreVipCard(
    offer: StoreVipOfferUi,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.sm)
    val clickEnabled = enabled && !isLoading
    Box(
        modifier = modifier
            .height(ComponentSize.storeVipCardHeight)
            .shadow(
                elevation = ComponentSize.storeVipCardElevation,
                shape = shape,
                ambientColor = DemoColors.storeVipCardShadow,
                spotColor = DemoColors.storeVipCardShadow,
            )
            .clip(shape)
            .background(DemoGradients.storeVipCard)
            .border(ComponentSize.meCardStroke, DemoColors.storeVipCardBorder, shape)
            .clickable(enabled = clickEnabled, onClick = onClick),
    ) {
        StoreRibbonBadge(
            text = offer.badgeLabel?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.store_try_now),
            brush = DemoGradients.storeVipRibbon,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(ComponentSize.storeRibbonInset),
        )
        Image(
            painter = painterResource(R.drawable.store_ic_vip_badge),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = ComponentSize.storeVipBadgeStart,
                    top = ComponentSize.storeVipBadgeTop,
                )
                .size(ComponentSize.storeVipBadge),
            contentScale = ContentScale.Fit,
        )
        // Title is card-centered; benefits/price sit in the right cluster (Figma x=121…294).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ComponentSize.storeVipContentTop),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(
                    R.string.store_vip_offer_title,
                    offer.month,
                    offer.title,
                ),
                color = DemoColors.textPrimary,
                fontSize = TextSize.lg,
                lineHeight = TextSize.storeVipTitleLine,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(ComponentSize.storeVipTitleToBenefits))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = ComponentSize.storeVipContentStart,
                        end = ComponentSize.storeVipContentEnd,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StoreVipBenefitsRow(
                    giveCoins = offer.giveCoins,
                    matchCount = offer.matchCount,
                )
                Spacer(modifier = Modifier.height(ComponentSize.storeVipBenefitsToDivider))
                StoreDashedDivider(
                    color = DemoColors.storeVipCardBorder,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(ComponentSize.storeVipDividerToPrice))
                if (isLoading) {
                    val loadingCd = stringResource(R.string.store_cd_loading)
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(IconSize.sm)
                            .semantics { contentDescription = loadingCd },
                        color = DemoColors.textPrimary,
                        strokeWidth = ComponentSize.profileProgressStroke,
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ComponentSize.storeVipPriceGap),
                    ) {
                        Text(
                            text = offer.price,
                            color = DemoColors.textPrimary,
                            fontSize = TextSize.lg,
                            lineHeight = TextSize.storeVipTitleLine,
                            fontWeight = FontWeight.Black,
                        )
                        offer.originalPrice?.takeIf { it.isNotBlank() }?.let { original ->
                            StorePromoOriginalPrice(text = original)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun StoreSaleCard(
    offer: StoreSaleOfferUi,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.sm)
    val clickEnabled = enabled && !isLoading
    Box(
        modifier = modifier
            .height(ComponentSize.storeSaleCardHeight)
            .shadow(
                elevation = ComponentSize.storeVipCardElevation,
                shape = shape,
                ambientColor = DemoColors.storeVipCardShadow,
                spotColor = DemoColors.storeVipCardShadow,
            )
            .clip(shape)
            .background(DemoGradients.storeSaleCard)
            .border(ComponentSize.meCardStroke, DemoColors.storeSaleCardBorder, shape)
            .clickable(enabled = clickEnabled, onClick = onClick),
    ) {
        StoreRibbonBadge(
            text = offer.badgeLabel?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.store_super_discount),
            brush = DemoGradients.storeSaleRibbon,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(ComponentSize.storeRibbonInset),
        )
        Image(
            painter = painterResource(R.drawable.store_ill_sale_gift),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = ComponentSize.storeSaleGiftStart,
                    top = ComponentSize.storeSaleGiftTop,
                )
                .size(ComponentSize.storeSaleGift),
            contentScale = ContentScale.Fit,
        )
        offer.discountPercent?.takeIf { it.isNotBlank() }?.let { percent ->
            StoreSaleOffBadge(
                percent = percent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = ComponentSize.storeRibbonInset,
                        end = ComponentSize.storeSaleOffRibbonEnd,
                    ),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = ComponentSize.storeSaleContentTop,
                    start = ComponentSize.storeSaleContentStart,
                    end = ComponentSize.storeSaleContentEnd,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.store_sale_coins_amount, offer.baseCoins),
                color = DemoColors.textPrimary,
                fontSize = TextSize.lg,
                lineHeight = TextSize.storeVipTitleLine,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (offer.bonusCoins > 0) {
                Spacer(modifier = Modifier.height(ComponentSize.storeSaleTitleToExtra))
                StoreSaleExtraRow(bonusCoins = offer.bonusCoins)
            }
            Spacer(modifier = Modifier.height(ComponentSize.storeSaleExtraToDivider))
            StoreDashedDivider(
                color = DemoColors.storeSaleCardBorder,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(ComponentSize.storeSaleDividerToPrice))
            if (isLoading) {
                val loadingCd = stringResource(R.string.store_cd_loading)
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(IconSize.sm)
                        .semantics { contentDescription = loadingCd },
                    color = DemoColors.textPrimary,
                    strokeWidth = ComponentSize.profileProgressStroke,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ComponentSize.storeSalePriceGap),
                ) {
                    Text(
                        text = offer.price,
                        color = DemoColors.textPrimary,
                        fontSize = TextSize.lg,
                        lineHeight = TextSize.storeVipTitleLine,
                        fontWeight = FontWeight.Black,
                    )
                    offer.originalPrice?.takeIf { it.isNotBlank() }?.let { original ->
                        StorePromoOriginalPrice(
                            text = original,
                            strikeWidth = ComponentSize.storeSaleStrikeWidth,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreSaleOffBadge(
    percent: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(ComponentSize.storeSaleOffRibbonWidth)
            .height(ComponentSize.storeSaleOffRibbonHeight),
    ) {
        Image(
            painter = painterResource(R.drawable.store_ic_sale_off_ribbon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        // Figma: "72%" center y=13, "OFF" center y=30 inside the 45dp ribbon.
        Text(
            text = percent,
            color = DemoColors.onPrimaryButton,
            fontSize = TextSize.sm,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Spacing.xs),
        )
        Text(
            text = stringResource(R.string.store_sale_off),
            color = DemoColors.onPrimaryButton,
            fontSize = TextSize.xs,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = ComponentSize.storeSaleOffLabelTop),
        )
    }
}

@Composable
private fun StoreSaleExtraRow(
    bonusCoins: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.store_sale_extra),
            color = DemoColors.storeSaleExtraLabel,
            fontSize = TextSize.xs,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(modifier = Modifier.width(ComponentSize.storeSaleExtraLabelToCoin))
        Image(
            painter = painterResource(R.drawable.store_ic_sale_coin),
            contentDescription = null,
            modifier = Modifier.size(ComponentSize.storeSaleExtraCoin),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.width(ComponentSize.storeSaleExtraCoinToText))
        Text(
            text = bonusCoins.toString(),
            color = DemoColors.textPrimary,
            fontSize = TextSize.sm,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(modifier = Modifier.width(Spacing.xxs))
        Text(
            text = stringResource(R.string.store_vip_coins_label),
            color = DemoColors.storeVipBenefitMuted,
            fontSize = TextSize.sm,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun StorePromoOriginalPrice(
    text: String,
    modifier: Modifier = Modifier,
    strikeWidth: Dp = ComponentSize.storeVipStrikeWidth,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = DemoColors.storeVipOriginalPrice,
            fontSize = TextSize.xs,
            lineHeight = TextSize.storeVipOriginalLine,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .width(strikeWidth)
                .height(ComponentSize.storeDashedStroke)
                .graphicsLayer { rotationZ = -21.57f }
                .background(DemoColors.storeVipPriceStrike),
        )
    }
}

@Composable
private fun StoreVipBenefitsRow(
    giveCoins: Int,
    matchCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.store_ic_coin),
            contentDescription = null,
            modifier = Modifier.size(ComponentSize.storeVipBenefitIcon),
        )
        Spacer(modifier = Modifier.width(ComponentSize.storeVipIconToLabel))
        StoreBenefitLabeledValue(
            value = giveCoins.toString(),
            label = stringResource(R.string.store_vip_coins_label),
            labelColor = DemoColors.storeVipBenefitMuted,
        )
        Spacer(modifier = Modifier.width(ComponentSize.storeVipBenefitSectionGap))
        Box(
            modifier = Modifier
                .width(ComponentSize.meCardStroke)
                .height(ComponentSize.storeVipBenefitDividerHeight)
                .background(DemoColors.storeVipCardBorder),
        )
        Spacer(modifier = Modifier.width(ComponentSize.storeVipBenefitSectionGap))
        Image(
            painter = painterResource(R.drawable.store_ic_vip_match),
            contentDescription = null,
            modifier = Modifier.size(ComponentSize.storeVipBenefitIcon),
        )
        Spacer(modifier = Modifier.width(ComponentSize.storeVipIconToLabel))
        StoreBenefitLabeledValue(
            value = matchCount.toString(),
            label = stringResource(R.string.store_vip_match_label),
            labelColor = DemoColors.storeVipMatchMuted,
        )
    }
}

@Composable
private fun StoreBenefitLabeledValue(
    value: String,
    label: String,
    labelColor: Color,
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            color = DemoColors.textPrimary,
            fontSize = TextSize.sm,
            lineHeight = TextSize.storeVipBenefitLine,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = label,
            color = labelColor,
            fontSize = TextSize.sm,
            lineHeight = TextSize.storeVipBenefitLine,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun StoreDashedDivider(
    color: Color,
    modifier: Modifier = Modifier,
) {
    ComposeCanvas(
        modifier = modifier.height(ComponentSize.storeDashedStroke),
    ) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = size.height.coerceAtLeast(1f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = stroke.width,
            pathEffect = stroke.pathEffect,
            cap = stroke.cap,
        )
    }
}

@Composable
internal fun StoreCoinCard(
    offer: StoreCoinOfferUi,
    enabled: Boolean,
    isLoading: Boolean,
    tall: Boolean,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.storeCoinCard)
    val hasBadge = !offer.cornerBadge.isNullOrBlank()
    val hasDiscount = !offer.discountLabel.isNullOrBlank()
    Box(
        modifier = modifier
            .height(
                if (tall) {
                    ComponentSize.storeCoinCardHeightWithBadge
                } else {
                    ComponentSize.storeCoinCardHeight
                },
            )
            .shadow(
                elevation = ComponentSize.storeCoinCardElevation,
                shape = shape,
                ambientColor = DemoColors.storeCoinCardShadow,
                spotColor = DemoColors.storeCoinCardShadow,
            )
            .clip(shape)
            .background(DemoColors.storeCoinCardSurface)
            .border(ComponentSize.meCardStroke, DemoColors.storeCoinCardBorder, shape),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (tall) {
                        ComponentSize.storeCoinIconTopWithBadge
                    } else {
                        ComponentSize.storeCoinIconTop
                    },
                    bottom = ComponentSize.storeCoinCardBottom,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StoreCoinIcon(
                iconUrl = offer.iconUrl,
                modifier = Modifier.size(ComponentSize.storeCoinIcon),
            )
            Spacer(modifier = Modifier.height(ComponentSize.storeCoinIconToAmount))
            Text(
                text = offer.coinAmount.toString(),
                color = DemoColors.storeCoinAmount,
                fontSize = TextSize.storeCoinAmount,
                fontWeight = FontWeight.Black,
                lineHeight = TextSize.storeCoinAmount,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(ComponentSize.storeCoinAmountToDiscount))
            if (hasDiscount) {
                Box(
                    modifier = Modifier
                        .heightIn(min = ComponentSize.storeDiscountChipMinHeight)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(DemoColors.storeDiscountChip)
                        .padding(
                            horizontal = ComponentSize.storeDiscountChipHorizontal,
                            vertical = ComponentSize.storeDiscountChipVertical,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = offer.discountLabel.orEmpty(),
                        color = DemoColors.storeDiscountText,
                        fontSize = TextSize.storeDiscount,
                        lineHeight = TextSize.storeDiscount,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(ComponentSize.storeDiscountChipMinHeight))
            }
            Spacer(modifier = Modifier.height(ComponentSize.storeCoinDiscountToButton))
            StorePriceButton(
                price = offer.price,
                originalPrice = offer.originalPrice,
                enabled = enabled,
                isLoading = isLoading,
                onClick = onPurchase,
            )
        }
        if (hasBadge) {
            StoreRibbonBadge(
                text = offer.cornerBadge.orEmpty(),
                brush = DemoGradients.storeSaleRibbon,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(ComponentSize.storeRibbonInset),
            )
        }
    }
}

@Composable
private fun StoreCoinIcon(
    iconUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cd = stringResource(R.string.store_cd_coin_icon)
    if (iconUrl.isNullOrBlank()) {
        Image(
            painter = painterResource(R.drawable.store_ic_coin),
            contentDescription = cd,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(iconUrl)
                .crossfade(true)
                .build(),
            contentDescription = cd,
            placeholder = painterResource(R.drawable.store_ic_coin),
            error = painterResource(R.drawable.store_ic_coin),
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun StorePriceButton(
    price: String,
    originalPrice: String?,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clickEnabled = enabled && !isLoading
    Box(
        modifier = modifier
            .height(ComponentSize.storePriceButtonHeight)
            .widthIn(min = ComponentSize.storePriceButtonMinWidth)
            .clip(RoundedCornerShape(Radius.pill))
            .background(DemoGradients.primaryButton)
            .clickable(enabled = clickEnabled, onClick = onClick)
            .padding(horizontal = ComponentSize.storePriceButtonHorizontal),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            val loadingCd = stringResource(R.string.store_cd_loading)
            CircularProgressIndicator(
                modifier = Modifier
                    .size(IconSize.sm)
                    .semantics { contentDescription = loadingCd },
                color = DemoColors.onPrimaryButton,
                strokeWidth = ComponentSize.profileProgressStroke,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = ComponentSize.storePriceButtonGap,
                    alignment = Alignment.CenterHorizontally,
                ),
            ) {
                originalPrice?.takeIf { it.isNotBlank() }?.let { original ->
                    Text(
                        text = original,
                        color = DemoColors.storePriceOriginal,
                        fontSize = TextSize.storePriceOriginal,
                        lineHeight = TextSize.storePriceOriginal,
                        textDecoration = TextDecoration.LineThrough,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = price,
                    color = DemoColors.onPrimaryButton,
                    fontSize = TextSize.storePrice,
                    lineHeight = TextSize.storePrice,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StoreRibbonBadge(
    text: String,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(ComponentSize.storeRibbonHeight)
            .clip(
                RoundedCornerShape(
                    topStart = Radius.storeRibbonTopStart,
                    bottomEnd = Radius.storeRibbonBottomEnd,
                ),
            )
            .background(brush)
            .padding(horizontal = ComponentSize.storeRibbonHorizontal),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = DemoColors.onPrimaryButton,
            fontSize = TextSize.storeRibbon,
            fontWeight = FontWeight.Bold,
            lineHeight = TextSize.storeRibbonLine,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun rememberStoreSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "storeSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "storeSkeletonPulse",
    )
    return pulse
}

@Composable
private fun StoreSkeleton(modifier: Modifier = Modifier) {
    val pulse = rememberStoreSkeletonPulse()
    val fill = DemoColors.skeleton.copy(alpha = pulse)
    val highlight = DemoColors.skeletonHighlight.copy(alpha = pulse)
    val loadingCd = stringResource(R.string.store_cd_loading)
    LazyVerticalGrid(
        columns = GridCells.Fixed(ComponentSize.storeCoinColumns),
        modifier = modifier.semantics { contentDescription = loadingCd },
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.storeNavToVip,
            bottom = Spacing.lg,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.storeCoinColumnGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.storeCoinRowGap),
        userScrollEnabled = false,
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "promo") {
            StoreVipCardSkeleton(fill = fill, highlight = highlight)
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "choose") {
            Box(
                modifier = Modifier
                    .padding(top = Spacing.storePagerToSectionExtra)
                    .width(ComponentSize.storeSkeletonSectionWidth)
                    .height(ComponentSize.storeSkeletonSectionHeight)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(highlight),
            )
        }
        items(ComponentSize.storeSkeletonCoinCount) {
            StoreCoinCardSkeleton(
                fill = fill,
                highlight = highlight,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StoreVipCardSkeleton(
    fill: Color,
    highlight: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.sm)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.storeVipCardHeight)
            .shadow(
                elevation = ComponentSize.storeVipCardElevation,
                shape = shape,
                ambientColor = DemoColors.storeVipCardShadow,
                spotColor = DemoColors.storeVipCardShadow,
            )
            .clip(shape)
            .background(fill),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .width(ComponentSize.storeSkeletonSectionWidth)
                    .height(ComponentSize.storeSkeletonSectionHeight)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(highlight),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.storeVipBenefitDividerHeight)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(highlight.copy(alpha = 0.7f)),
            )
            Box(
                modifier = Modifier
                    .width(ComponentSize.storeSkeletonDiscountWidth)
                    .height(ComponentSize.storeSkeletonSectionHeight)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(highlight),
            )
        }
    }
}

@Composable
private fun StoreCoinCardSkeleton(
    fill: Color,
    highlight: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.storeCoinCard)
    Column(
        modifier = modifier
            .height(ComponentSize.storeCoinCardHeight)
            .shadow(
                elevation = ComponentSize.storeCoinCardElevation,
                shape = shape,
                ambientColor = DemoColors.storeCoinCardShadow,
                spotColor = DemoColors.storeCoinCardShadow,
            )
            .clip(shape)
            .background(DemoColors.storeCoinCardSurface)
            .border(ComponentSize.meCardStroke, DemoColors.storeCoinCardBorder, shape)
            .padding(
                top = ComponentSize.storeCoinIconTop,
                bottom = ComponentSize.storeCoinCardBottom,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(ComponentSize.storeCoinIcon)
                .clip(CircleShape)
                .background(fill),
        )
        Spacer(modifier = Modifier.height(ComponentSize.storeCoinIconToAmount))
        Box(
            modifier = Modifier
                .width(ComponentSize.storeSkeletonAmountWidth)
                .height(ComponentSize.storeSkeletonAmountHeight)
                .clip(RoundedCornerShape(Radius.sm))
                .background(highlight),
        )
        Spacer(modifier = Modifier.height(ComponentSize.storeCoinAmountToDiscount))
        Box(
            modifier = Modifier
                .width(ComponentSize.storeSkeletonDiscountWidth)
                .height(ComponentSize.storeDiscountChipMinHeight)
                .clip(RoundedCornerShape(Radius.pill))
                .background(fill),
        )
        Spacer(modifier = Modifier.height(ComponentSize.storeCoinDiscountToButton))
        Box(
            modifier = Modifier
                .height(ComponentSize.storePriceButtonHeight)
                .widthIn(min = ComponentSize.storePriceButtonMinWidth)
                .clip(RoundedCornerShape(Radius.pill))
                .background(fill),
        )
    }
}

@Composable
private fun StoreErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = DemoColors.textSecondary,
            fontSize = TextSize.sm,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.store_retry))
        }
    }
}

private val previewState = StoreUiState(
    balance = 40,
    vipOffers = listOf(
        StoreVipOfferUi(
            id = 1,
            sku = "vip_1m",
            title = "Month",
            month = 1,
            price = "$3.33",
            originalPrice = "$13.33",
            giveCoins = 123,
            matchCount = 1,
            badgeLabel = null,
        ),
        StoreVipOfferUi(
            id = 2,
            sku = "vip_3m",
            title = "Month",
            month = 3,
            price = "$8.99",
            originalPrice = "$29.99",
            giveCoins = 400,
            matchCount = 3,
            badgeLabel = "Try Now",
        ),
    ),
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
            sku = "sale_6666",
            baseCoins = 6666,
            bonusCoins = 300,
            price = "$5.99",
            originalPrice = "$19.99",
            discountPercent = "70%",
            badgeLabel = null,
        ),
    ),
    coinOffers = listOf(
        StoreCoinOfferUi(
            id = 11,
            sku = "coin_98",
            coinAmount = 98,
            price = "$1.89",
            originalPrice = "$3.04",
            discountLabel = "38% OFF",
            cornerBadge = null,
            iconUrl = null,
        ),
        StoreCoinOfferUi(
            id = 12,
            sku = "coin_198",
            coinAmount = 198,
            price = "$3.49",
            originalPrice = "$5.99",
            discountLabel = "40% OFF",
            cornerBadge = null,
            iconUrl = null,
        ),
        StoreCoinOfferUi(
            id = 13,
            sku = "coin_498",
            coinAmount = 498,
            price = "$7.99",
            originalPrice = "$12.99",
            discountLabel = "38% OFF",
            cornerBadge = null,
            iconUrl = null,
        ),
        StoreCoinOfferUi(
            id = 14,
            sku = "coin_998",
            coinAmount = 998,
            price = "$14.99",
            originalPrice = "$24.99",
            discountLabel = "40% OFF",
            cornerBadge = null,
            iconUrl = null,
        ),
    ),
)

@Preview(name = "Store skeleton", locale = "en")
@Composable
private fun StoreSkeletonPreview() {
    DemoTheme {
        StoreScreen(
            state = StoreUiState(isLoading = true, balance = 40),
            onIntent = {},
            onBack = {},
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "Store RTL", locale = "ar")
@Composable
private fun StoreScreenPreview() {
    DemoTheme {
        StoreScreen(
            state = previewState,
            onIntent = {},
            onBack = {},
        )
    }
}
