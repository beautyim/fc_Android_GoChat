package com.example.demoproject.product.match

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demoproject.platform.data.network.dto.MATCH_SEX_FEMALE
import com.example.demoproject.platform.data.network.dto.MATCH_SEX_MALE
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.WindowWidthClass
import com.example.demoproject.ui.foundation.currentWindowSize
import com.example.demoproject.ui.foundation.readableContentWidth

@Composable
fun MatchScreen(
    viewModel: MatchViewModel,
    onNavigateTab: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MatchScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateTab = onNavigateTab,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    state: MatchUiState,
    onIntent: (MatchIntent) -> Unit,
    onNavigateTab: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.sheet),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MatchHeader(
                coinBalance = state.coinBalance,
                onCoinsClick = { onIntent(MatchIntent.OpenCoins) },
            )
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = ComponentSize.onlineTabBarBodyHeight),
            ) {
                val contentModifier = when (currentWindowSize().widthClass) {
                    WindowWidthClass.Compact -> Modifier.fillMaxWidth()
                    WindowWidthClass.Medium, WindowWidthClass.Expanded ->
                        Modifier
                            .readableContentWidth()
                            .align(Alignment.TopCenter)
                }
                Column(
                    modifier = contentModifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.matchHeroInset)
                        .padding(top = Spacing.sm, bottom = Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MatchHeroCard(
                        state = state,
                        onFilterClick = { onIntent(MatchIntent.OpenFilter) },
                        onStartClick = { onIntent(MatchIntent.StartVideoMatch) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(ComponentSize.matchHeroAspect),
                    )
                }
            }
        }
        MatchBottomBar(
            chatUnreadCount = state.chatUnreadCount,
            onSelect = onNavigateTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        if (state.isFilterSheetVisible) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { onIntent(MatchIntent.DismissFilter) },
                sheetState = sheetState,
                containerColor = DemoColors.sheet,
                scrimColor = DemoColors.scrim,
                shape = RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg),
                dragHandle = null,
            ) {
                MatchFilterSheetContent(
                    selectedSex = state.draftMatchSex,
                    onSelect = { onIntent(MatchIntent.SelectMatchSex(it)) },
                    onApply = { onIntent(MatchIntent.ApplyFilters) },
                    onReset = { onIntent(MatchIntent.ResetFilters) },
                    onDismiss = { onIntent(MatchIntent.DismissFilter) },
                )
            }
        }
        if (state.isSearching) {
            MatchSearchingOverlay(onCancel = { onIntent(MatchIntent.CancelVideoMatch) })
        }
    }
}

@Composable
private fun MatchHeader(
    coinBalance: Int,
    onCoinsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DemoColors.onlineHeaderStart, DemoColors.sheet),
                ),
            )
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.navHeaderHeight)
                .padding(start = Spacing.md + Spacing.xs, end = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.match_title),
                color = DemoColors.navTitle,
                fontSize = TextSize.title,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            val coinsCd = stringResource(R.string.match_cd_coins)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(DemoColors.chip)
                    .semantics { contentDescription = coinsCd }
                    .clickable(onClick = onCoinsClick)
                    .padding(
                        start = Spacing.sm,
                        end = Spacing.xs,
                        top = Spacing.xs,
                        bottom = Spacing.xs,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                Text(
                    text = coinBalance.toString(),
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.sm,
                    fontWeight = FontWeight.SemiBold,
                )
                Image(
                    painter = painterResource(R.drawable.match_ic_coin),
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.onlineCoinIcon),
                )
                Image(
                    painter = painterResource(R.drawable.match_ic_chevron_nav),
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.sm - Spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun MatchHeroCard(
    state: MatchUiState,
    onFilterClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(DemoColors.matchHeroFallback),
    ) {
        Image(
            painter = painterResource(R.drawable.match_bg_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DemoColors.matchHeroScrim),
        )
        MatchRadarOverlay(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .aspectRatio(1f),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(Spacing.matchHeroChrome),
        ) {
            if (state.isVip) {
                MatchVipOffBadge(modifier = Modifier.align(Alignment.CenterStart))
            }
            MatchPriceChip(
                price = state.matchPrice,
                modifier = Modifier.align(Alignment.Center),
            )
            val filterCd = stringResource(R.string.match_cd_filter)
            // Figma 315:5307: once a gender filter is applied the funnel carries that glyph.
            val appliedFilterIcon = when {
                !state.hasAppliedFilters -> null
                state.matchSex == MATCH_SEX_MALE -> R.drawable.match_ic_filter_male_selected
                state.matchSex == MATCH_SEX_FEMALE -> R.drawable.match_ic_filter_female_selected
                else -> null
            }
            Image(
                painter = painterResource(appliedFilterIcon ?: R.drawable.match_ic_filter),
                contentDescription = filterCd,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(
                        if (appliedFilterIcon == null) {
                            ComponentSize.matchFilterIcon
                        } else {
                            ComponentSize.matchAppliedFilterIcon
                        },
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onFilterClick,
                    ),
                contentScale = ContentScale.Fit,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = Spacing.matchCtaHorizontal,
                    end = Spacing.matchCtaHorizontal,
                    bottom = Spacing.matchCtaBottom,
                ),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(ComponentSize.matchCtaHeight)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(DemoGradients.primaryButton)
                    .clickable(
                        enabled = !state.isLoading && !state.isSearching,
                        onClick = onStartClick,
                    ),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = DemoColors.onPrimaryButton,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(IconSize.md),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.match_action_video),
                        color = DemoColors.onPrimaryButton,
                        fontSize = TextSize.md,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (state.matchFreeCount > 0) {
                Text(
                    text = stringResource(R.string.match_free_badge),
                    color = DemoColors.onPrimaryButton,
                    fontSize = TextSize.xs,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = ComponentSize.matchFreeBadgeLift)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(DemoColors.freeBadge)
                        .padding(horizontal = Spacing.chipGap, vertical = Spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun MatchVipOffBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(DemoColors.matchVipOffBadge)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Image(
            painter = painterResource(R.drawable.match_ic_vip_crown),
            contentDescription = null,
            modifier = Modifier.size(ComponentSize.matchVipOffIcon),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = stringResource(R.string.match_vip_off),
            color = DemoColors.onPrimaryButton,
            fontSize = TextSize.matchVipOff,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun MatchPriceChip(
    price: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(DemoColors.matchPriceChip)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(
            text = price.toString(),
            color = DemoColors.onPrimaryButton,
            fontSize = TextSize.md,
            fontWeight = FontWeight.Medium,
        )
        Image(
            painter = painterResource(R.drawable.match_ic_coin),
            contentDescription = null,
            modifier = Modifier.size(ComponentSize.matchPriceCoin),
        )
        Text(
            text = stringResource(R.string.match_price_per_time),
            color = DemoColors.onPrimaryButton,
            fontSize = TextSize.md,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MatchRadarOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "matchRipple")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "matchRippleProgress",
    )
    val rippleColor = DemoColors.matchRipple
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2f
            val stroke = Stroke(width = 1.5.dp.toPx())
            for (i in 0 until 3) {
                val phase = (progress + i / 3f) % 1f
                val radius = maxRadius * (0.28f + phase * 0.72f)
                val alpha = (1f - phase).coerceIn(0f, 1f) * 0.55f
                drawCircle(
                    color = rippleColor.copy(alpha = alpha),
                    radius = radius,
                    style = stroke,
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.match_ic_heart_glow),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = Spacing.lg, end = Spacing.xl)
                .size(ComponentSize.matchHeartGlow),
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(R.drawable.match_ic_heart_white),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = Spacing.md)
                .size(ComponentSize.matchHeartWhite),
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(R.drawable.match_ic_heart_glow),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = Spacing.xl, start = Spacing.lg)
                .size(ComponentSize.matchHeartGlow),
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(R.drawable.match_ic_heart_white),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = Spacing.lg, end = Spacing.md)
                .size(ComponentSize.matchHeartWhite),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun MatchFilterSheetContent(
    selectedSex: Int,
    onSelect: (Int) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(
                    start = Spacing.md + Spacing.xs,
                    top = Spacing.md + Spacing.xs,
                ),
            ) {
                Text(
                    text = stringResource(R.string.match_filter_title),
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.matchFilterTitle,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.match_filter_subtitle),
                    color = DemoColors.textSecondary,
                    fontSize = TextSize.sm,
                )
            }
            Image(
                painter = painterResource(R.drawable.match_ic_filter_close),
                contentDescription = stringResource(R.string.match_cd_close_filter),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = Spacing.md, end = Spacing.md)
                    .size(ComponentSize.matchFilterCloseIcon)
                    .clickable(onClick = onDismiss),
                contentScale = ContentScale.Fit,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        Column(
            modifier = Modifier.padding(horizontal = Spacing.md + Spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.match_filter_gender),
                color = DemoColors.textPrimary,
                fontSize = TextSize.md,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                MatchFilterOption(
                    label = stringResource(R.string.match_filter_all),
                    selected = selectedSex == MATCH_SEX_ALL,
                    onClick = { onSelect(MATCH_SEX_ALL) },
                    modifier = Modifier.weight(1f),
                )
                MatchFilterOption(
                    label = stringResource(R.string.match_filter_male),
                    iconRes = R.drawable.match_ic_filter_male,
                    selected = selectedSex == MATCH_SEX_MALE,
                    onClick = { onSelect(MATCH_SEX_MALE) },
                    modifier = Modifier.weight(1f),
                )
                MatchFilterOption(
                    label = stringResource(R.string.match_filter_female),
                    iconRes = R.drawable.match_ic_filter_female,
                    selected = selectedSex == MATCH_SEX_FEMALE,
                    onClick = { onSelect(MATCH_SEX_FEMALE) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .height(ComponentSize.matchCtaHeight)
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoGradients.primaryButton)
                .clickable(onClick = onApply),
        ) {
            Text(
                text = stringResource(R.string.match_filter_apply),
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.md,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            text = stringResource(R.string.match_filter_reset),
            color = DemoColors.textAuxiliary,
            fontSize = TextSize.sm,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = Spacing.md)
                .clickable(onClick = onReset),
        )
    }
}

@Composable
private fun MatchFilterOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
) {
    Row(
        modifier = modifier
            .height(ComponentSize.matchFilterOptionHeight)
            .clip(RoundedCornerShape(Radius.pill))
            .background(
                if (selected) DemoColors.matchFilterSelected else DemoColors.matchFilterOption,
            )
            .border(
                width = ComponentSize.matchFilterOptionStroke,
                color = if (selected) DemoColors.link else DemoColors.matchFilterOptionBorder,
                shape = RoundedCornerShape(Radius.pill),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.matchFilterGenderIcon),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.size(Spacing.sm))
        }
        Text(
            text = label,
            color = DemoColors.textPrimary,
            fontSize = TextSize.xs,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MatchBottomBar(
    chatUnreadCount: Int,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.match_bg_tab_bar),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ComponentSize.onlineTabBarAspect)
                    .align(Alignment.BottomCenter),
                contentScale = ContentScale.FillBounds,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = ComponentSize.onlineTabBarShadowBottom)
                    .height(ComponentSize.onlineTabRowHeight),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MatchTabIcon(
                    iconRes = R.drawable.match_ic_tab_home_inactive,
                    onClick = { onSelect("home") },
                )
                MatchTabIcon(
                    iconRes = R.drawable.match_ic_tab_feed_inactive,
                    onClick = { onSelect("feed") },
                )
                MatchCenterTab(onClick = { onSelect("match") })
                Box {
                    MatchTabIcon(
                        iconRes = R.drawable.match_ic_tab_chat_inactive,
                        onClick = { onSelect("chat") },
                    )
                    if (chatUnreadCount > 0) {
                        Text(
                            text = if (chatUnreadCount > 99) "99+" else chatUnreadCount.toString(),
                            color = DemoColors.onPrimaryButton,
                            fontSize = TextSize.meMeta,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clip(RoundedCornerShape(Radius.pill))
                                .background(DemoColors.meUnreadBadge)
                                .padding(
                                    horizontal = Spacing.xs + Spacing.xxs,
                                    vertical = Spacing.xxs,
                                ),
                        )
                    }
                }
                MatchTabIcon(
                    iconRes = R.drawable.match_ic_tab_profile_inactive,
                    onClick = { onSelect("profile") },
                )
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(DemoColors.sheet),
        )
    }
}

@Composable
private fun MatchCenterTab(onClick: () -> Unit) {
    val glow = DemoColors.tabMatchGlow
    val glowExtra = ComponentSize.onlineMatchGlowBlur
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(ComponentSize.onlineMatchTab)
            .drawBehind {
                val radius = size.minDimension / 2f + glowExtra.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.45f to glow,
                            1f to Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                )
            }
            .clip(CircleShape)
            .background(DemoColors.tabMatchButton)
            .clickable(onClick = onClick)
            .padding(Spacing.xs),
    ) {
        Image(
            painter = painterResource(R.drawable.match_ic_tab_match),
            contentDescription = stringResource(R.string.match_nav_match),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun MatchTabIcon(
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier
            .size(IconSize.lg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentScale = ContentScale.Fit,
    )
}

private val PreviewState = MatchUiState(
    coinBalance = 40,
    isVip = true,
    matchFreeCount = 1,
    matchPrice = 20,
    payUserMatchPrice = 40,
    vipMatchPrice = 20,
    matchSex = MATCH_SEX_FEMALE,
)

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "Match VIP", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MatchScreenVipPreview() {
    DemoTheme {
        MatchScreen(state = PreviewState, onIntent = {}, onNavigateTab = {})
    }
}

@Preview(name = "Match non-VIP", showBackground = true, widthDp = 375, heightDp = 812)
@Preview(name = "Match RTL", locale = "ar", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MatchScreenNonVipPreview() {
    DemoTheme {
        MatchScreen(
            state = PreviewState.copy(isVip = false, matchPrice = 40, matchFreeCount = 0),
            onIntent = {},
            onNavigateTab = {},
        )
    }
}

@Preview(name = "Match filtered male", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MatchScreenFilteredPreview() {
    DemoTheme {
        MatchScreen(
            state = PreviewState.copy(matchSex = MATCH_SEX_MALE, hasAppliedFilters = true),
            onIntent = {},
            onNavigateTab = {},
        )
    }
}

@PreviewFontScale
@Preview(name = "Match filter sheet", showBackground = true, widthDp = 375)
@Preview(name = "Match filter sheet small", showBackground = true, widthDp = 320)
@Preview(name = "Match filter sheet RTL", locale = "ar", showBackground = true, widthDp = 375)
@Composable
private fun MatchFilterSheetContentPreview() {
    DemoTheme {
        MatchFilterSheetContent(
            selectedSex = MATCH_SEX_MALE,
            onSelect = {},
            onApply = {},
            onReset = {},
            onDismiss = {},
        )
    }
}
