package com.example.demoproject.product.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.platform.data.model.OnlinePresence
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoTextAutoSize
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.WindowWidthClass
import com.example.demoproject.ui.foundation.currentWindowSize
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private val OnlineCardTitleStyle = TextStyle(
    fontSize = TextSize.md,
    lineHeight = 16.sp,
    fontWeight = FontWeight.Medium,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

private val OnlineCardStatusStyle = TextStyle(
    fontSize = TextSize.caption,
    lineHeight = 12.sp,
    fontWeight = FontWeight.Normal,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    onNavigateTab: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val filters = OnlineFilter.entries
    val pagerState = rememberPagerState(
        initialPage = state.selectedFilter.ordinal,
        pageCount = { filters.size },
    )
    val columns = when (currentWindowSize().widthClass) {
        WindowWidthClass.Compact -> 2
        WindowWidthClass.Medium, WindowWidthClass.Expanded -> 3
    }

    LaunchedEffect(pagerState) {
        // Use settledPage so animateScrollToPage (e.g. All→Following) does not
        // briefly report the intermediate page (New) and overwrite the chip selection.
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                onIntent(HomeIntent.SelectFilter(filters[page]))
            }
    }
    LaunchedEffect(state.selectedFilter) {
        val index = state.selectedFilter.ordinal
        if (pagerState.currentPage != index) {
            pagerState.animateScrollToPage(index)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.sheet),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnlineHeader(
                coinBalance = state.coinBalance,
                onCoinsClick = { onIntent(HomeIntent.OpenCoins) },
            )
            OnlineFilterChips(
                selected = state.selectedFilter,
                onSelect = { onIntent(HomeIntent.SelectFilter(it)) },
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = ComponentSize.onlineTabBarBodyHeight),
                beyondViewportPageCount = 1,
            ) { pageIndex ->
                val filter = filters[pageIndex]
                val page = state.pages[filter] ?: OnlineTabPage()
                OnlineTabPageContent(
                    page = page,
                    columns = columns,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        OnlineBottomBar(
            selected = OnlineBottomTab.Home,
            onSelect = onNavigateTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        state.videoShowUser?.let { user ->
            VideoShowOverlay(
                user = user,
                coinBalance = state.coinBalance,
                gifts = state.gifts,
                selectedGiftId = state.selectedGiftId,
                isGiftSheetVisible = state.isGiftSheetVisible,
                isGiftCatalogLoading = state.isGiftCatalogLoading,
                isGiftSending = state.isGiftSending,
                giftAnimationUrl = state.giftAnimationUrl,
                onIntent = onIntent,
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineTabPageContent(
    page: OnlineTabPage,
    columns: Int,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, page.hasMore, page.isLoadingMore, page.users.size) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 3
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (page.hasMore && !page.isLoadingMore && !page.isLoading && !page.isRefreshing) {
                    onIntent(HomeIntent.LoadMore)
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = page.isRefreshing,
        onRefresh = { onIntent(HomeIntent.Refresh) },
        modifier = modifier,
    ) {
        when {
            page.isLoading || (!page.hasLoaded && page.users.isEmpty() && page.errorMessage == null) -> {
                OnlineSkeletonGrid(
                    columns = columns,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            page.errorMessage != null && page.users.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterVertically),
                ) {
                    Text(
                        text = page.errorMessage,
                        color = DemoColors.textSecondary,
                        fontSize = TextSize.sm,
                    )
                    TextButton(onClick = { onIntent(HomeIntent.Refresh) }) {
                        Text(stringResource(R.string.home_online_retry))
                    }
                }
            }
            page.users.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.home_online_empty),
                        color = DemoColors.textAuxiliary,
                        fontSize = TextSize.sm,
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = Spacing.chipGap,
                        end = Spacing.chipGap,
                        top = Spacing.sm,
                        bottom = Spacing.lg,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.chipGap),
                    verticalArrangement = Arrangement.spacedBy(Spacing.chipGap),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(
                        items = page.users,
                        key = { _, user -> user.id },
                    ) { _, user ->
                        OnlineUserCard(
                            user = user,
                            onCardClick = { onIntent(HomeIntent.OpenUserProfile(user)) },
                            onActionClick = { onIntent(HomeIntent.OpenUserAction(user)) },
                            onReportClick = { onIntent(HomeIntent.ReportUser(user.id)) },
                        )
                    }
                    if (page.isLoadingMore) {
                        item(span = { GridItemSpan(columns) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(IconSize.md),
                                    color = DemoColors.link,
                                    strokeWidth = Spacing.xxs,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineHeader(
    coinBalance: Int,
    onCoinsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
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
                // Figma Section: title start 20, trailing end 16.
                .padding(start = Spacing.md + Spacing.xs, end = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_online_title),
                color = DemoColors.navTitle,
                fontSize = TextSize.title,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(DemoColors.chip)
                    .clickable(onClick = onCoinsClick)
                    .padding(start = Spacing.sm, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                Text(
                    text = coinBalance.toString(),
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.sm,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = DemoTextAutoSize.price(TextSize.sm),
                )
                Image(
                    painter = painterResource(R.drawable.online_ic_coin),
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.onlineCoinIcon),
                )
                Image(
                    painter = painterResource(R.drawable.online_ic_chevron),
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.sm - Spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun OnlineFilterChips(
    selected: OnlineFilter,
    onSelect: (OnlineFilter) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected) {
        listState.animateScrollToItem(selected.ordinal)
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = Spacing.chipGap, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(OnlineFilter.entries, key = { it.name }) { filter ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.chip))
                    .background(if (isSelected) DemoColors.link else DemoColors.chip)
                    .clickable { onSelect(filter) }
                    .padding(
                        horizontal = if (filter == OnlineFilter.All) Spacing.md + Spacing.xs else Spacing.md,
                        vertical = Spacing.sm,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(filter.labelRes),
                    color = if (isSelected) DemoColors.onPrimaryButton else DemoColors.textAuxiliary,
                    fontSize = TextSize.xs,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun OnlineUserCard(
    user: OnlineUserUi,
    onCardClick: () -> Unit,
    onActionClick: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Figma 34:3866 — 170×282 card: top info + bottom 58dp action (8dp inset).
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ComponentSize.onlineCardAspect)
            .shadow(
                elevation = ComponentSize.onlineCardShadow,
                shape = RoundedCornerShape(Radius.card),
                ambientColor = DemoColors.cardShadow,
                spotColor = DemoColors.cardShadow,
            )
            .clip(RoundedCornerShape(Radius.card))
            .background(DemoColors.chip)
            .clickable(onClick = onCardClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(user.avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = user.nickname,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.onlineTopGradient)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                    ),
                ),
        )
        // Figma 1:521 — avatar 34 @ (8,12), title/status from x=50, report reserved at end.
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(
                    start = Spacing.sm,
                    top = Spacing.chipGap,
                    end = IconSize.md + Spacing.chipGap,
                ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(user.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ComponentSize.onlineCardAvatar)
                    .clip(CircleShape)
                    .background(DemoColors.chip),
            )
            Column(
                modifier = Modifier.weight(1f),
                // Figma status sits tight under the title; lift 5dp from the base title/status gap.
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = user.nickname,
                        color = DemoColors.onPrimaryButton,
                        style = OnlineCardTitleStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (user.age > 0) {
                        Text(
                            text = ", ${user.age}",
                            color = DemoColors.onPrimaryButton,
                            style = OnlineCardTitleStyle,
                            maxLines = 1,
                        )
                    }
                }
                Row(
                    modifier = Modifier.offset(y = -5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Image(
                        painter = painterResource(user.presence.statusIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.xs),
                    )
                    Text(
                        text = stringResource(user.presence.labelRes),
                        color = DemoColors.onCardMuted,
                        style = OnlineCardStatusStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.chipGap)
                .size(IconSize.md)
                .clickable(onClick = onReportClick),
            contentAlignment = Alignment.TopEnd,
        ) {
            Image(
                painter = painterResource(R.drawable.online_ic_report),
                contentDescription = stringResource(R.string.home_online_cd_report),
                modifier = Modifier.size(IconSize.xs),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = ComponentSize.onlineActionBottomInset)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .size(ComponentSize.onlineAction)
                    .clip(CircleShape)
                    .background(DemoColors.onlineAction)
                    .clickable(onClick = onActionClick),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(
                        if (user.prefersMessageAction) {
                            R.drawable.online_ic_action_message
                        } else {
                            R.drawable.online_ic_action_video
                        },
                    ),
                    contentDescription = stringResource(
                        if (user.prefersMessageAction) {
                            R.string.home_online_cd_message
                        } else {
                            R.string.home_online_cd_video
                        },
                    ),
                    modifier = Modifier.size(IconSize.md),
                )
            }
            if (user.showFreeBadge) {
                Text(
                    text = stringResource(R.string.home_online_free),
                    color = DemoColors.onPrimaryButton,
                    fontSize = TextSize.xs,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = ComponentSize.onlineFreeBadgeLift)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(DemoColors.freeBadge)
                        .padding(horizontal = Spacing.chipGap, vertical = Spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun OnlineSkeletonGrid(
    columns: Int,
    modifier: Modifier = Modifier,
    count: Int = columns * 3,
) {
    val pulse = rememberSkeletonPulse()
    val loadingCd = stringResource(R.string.home_online_cd_loading)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(
            start = Spacing.chipGap,
            end = Spacing.chipGap,
            top = Spacing.sm,
            bottom = Spacing.lg,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.chipGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.chipGap),
        userScrollEnabled = false,
        modifier = modifier.semantics {
            contentDescription = loadingCd
        },
    ) {
        items(count) {
            OnlineUserCardSkeleton(pulse = pulse)
        }
    }
}

@Composable
private fun rememberSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "onlineSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "onlineSkeletonPulse",
    )
    return pulse
}

@Composable
internal fun OnlineUserCardSkeleton(
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val fill = DemoColors.skeleton.copy(alpha = pulse)
    val highlight = DemoColors.skeletonHighlight.copy(alpha = pulse)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ComponentSize.onlineCardAspect)
            .shadow(
                elevation = ComponentSize.onlineCardShadow,
                shape = RoundedCornerShape(Radius.card),
                ambientColor = DemoColors.cardShadow,
                spotColor = DemoColors.cardShadow,
            )
            .clip(RoundedCornerShape(Radius.card))
            .background(fill),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.onlineTopGradient)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(highlight, Color.Transparent),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(
                    start = Spacing.sm,
                    top = Spacing.chipGap,
                    end = IconSize.md + Spacing.chipGap,
                ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(ComponentSize.onlineCardAvatar)
                    .clip(CircleShape)
                    .background(highlight),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Box(
                    modifier = Modifier
                        .width(ComponentSize.onlineSkeletonNameWidth)
                        .height(ComponentSize.onlineSkeletonNameHeight)
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(highlight),
                )
                Box(
                    modifier = Modifier
                        .offset(y = -5.dp)
                        .width(ComponentSize.onlineSkeletonStatusWidth)
                        .height(ComponentSize.onlineSkeletonStatusHeight)
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(highlight),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.chipGap)
                .size(IconSize.xs)
                .clip(RoundedCornerShape(Spacing.xxs))
                .background(highlight),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = ComponentSize.onlineActionBottomInset)
                .size(ComponentSize.onlineAction)
                .clip(CircleShape)
                .background(highlight),
        )
    }
}

private enum class OnlineBottomTab {
    Home,
    Feed,
    Match,
    Chat,
    Profile,
}

@Composable
private fun OnlineBottomBar(
    selected: OnlineBottomTab,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.online_bg_tab_bar),
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
                OnlineTabIcon(
                    iconRes = if (selected == OnlineBottomTab.Home) {
                        R.drawable.online_ic_tab_home
                    } else {
                        R.drawable.online_ic_tab_home_inactive
                    },
                    onClick = { onSelect("home") },
                )
                OnlineTabIcon(
                    iconRes = if (selected == OnlineBottomTab.Feed) {
                        R.drawable.online_ic_tab_feed
                    } else {
                        R.drawable.online_ic_tab_feed_inactive
                    },
                    onClick = { onSelect("feed") },
                )
                OnlineMatchTab(onClick = { onSelect("match") })
                OnlineTabIcon(
                    iconRes = if (selected == OnlineBottomTab.Chat) {
                        R.drawable.online_ic_tab_chat
                    } else {
                        R.drawable.online_ic_tab_chat_inactive
                    },
                    onClick = { onSelect("chat") },
                )
                OnlineTabIcon(
                    iconRes = if (selected == OnlineBottomTab.Profile) {
                        R.drawable.online_ic_tab_profile
                    } else {
                        R.drawable.online_ic_tab_profile_inactive
                    },
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
private fun OnlineMatchTab(onClick: () -> Unit) {
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
            painter = painterResource(R.drawable.online_ic_tab_match),
            contentDescription = stringResource(R.string.home_nav_match),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun OnlineTabIcon(
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

private val OnlineFilter.labelRes: Int
    @StringRes get() = when (this) {
        OnlineFilter.All -> R.string.home_online_filter_all
        OnlineFilter.New -> R.string.home_online_filter_new
        OnlineFilter.Following -> R.string.home_online_filter_following
        OnlineFilter.Popular -> R.string.home_online_filter_popular
        OnlineFilter.English -> R.string.home_online_filter_english
        OnlineFilter.Spanish -> R.string.home_online_filter_spanish
        OnlineFilter.Portuguese -> R.string.home_online_filter_portuguese
    }

private val OnlinePresence.statusIconRes: Int
    @DrawableRes get() = when (this) {
        OnlinePresence.Online -> R.drawable.online_ic_status_online
        OnlinePresence.Chatting -> R.drawable.online_ic_status_chatting
        OnlinePresence.Offline -> R.drawable.online_ic_status_offline
    }

private val OnlinePresence.labelRes: Int
    @StringRes get() = when (this) {
        OnlinePresence.Online -> R.string.home_online_status_online
        OnlinePresence.Chatting -> R.string.home_online_status_chatting
        OnlinePresence.Offline -> R.string.home_online_status_offline
    }

@PreviewScreenSizes
@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun HomeScreenPreview() {
    DemoTheme {
        val previewUsers = listOf(
            OnlineUserUi("1", "1", "Dulce", 22, null, OnlinePresence.Online, showFreeBadge = true),
            OnlineUserUi("2", "2", "Miracle", 23, null, OnlinePresence.Offline, showFreeBadge = false),
            OnlineUserUi("3", "3", "Hanna", 22, null, OnlinePresence.Online, showFreeBadge = true),
            OnlineUserUi("4", "4", "Jaydon", 25, null, OnlinePresence.Chatting, showFreeBadge = false),
        )
        HomeScreen(
            state = HomeUiState(
                coinBalance = 40,
                callFreeMin = 1,
                pages = OnlineFilter.entries.associateWith { filter ->
                    if (filter == OnlineFilter.All) {
                        OnlineTabPage(
                            users = previewUsers,
                            hasLoaded = true,
                            hasMore = false,
                        )
                    } else {
                        OnlineTabPage(hasLoaded = false)
                    }
                },
            ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun OnlineSkeletonPreview() {
    DemoTheme {
        OnlineSkeletonGrid(columns = 2, modifier = Modifier.fillMaxSize())
    }
}

@Preview
@Composable
private fun OnlineUserCardPreview() {
    DemoTheme {
        OnlineUserCard(
            user = OnlineUserUi(
                id = "1",
                externalUserId = "1",
                nickname = "Dulce",
                age = 22,
                avatarUrl = null,
                presence = OnlinePresence.Online,
                showFreeBadge = true,
            ),
            onCardClick = {},
            onActionClick = {},
            onReportClick = {},
            modifier = Modifier
                .width(ComponentSize.onlineCardMinWidth + Spacing.chipGap)
                .padding(Spacing.chipGap),
        )
    }
}

@Preview
@Composable
private fun OnlineUserCardLongNamePreview() {
    DemoTheme {
        OnlineUserCard(
            user = OnlineUserUi(
                id = "2",
                externalUserId = "2",
                nickname = "VeryLongNicknameThatShouldEllipsize",
                age = 24,
                avatarUrl = null,
                presence = OnlinePresence.Online,
                showFreeBadge = false,
            ),
            onCardClick = {},
            onActionClick = {},
            onReportClick = {},
            modifier = Modifier
                .width(ComponentSize.onlineCardMinWidth + Spacing.chipGap)
                .padding(Spacing.chipGap),
        )
    }
}
