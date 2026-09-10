package com.example.demoproject.product.call

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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallRecordsScreen(
    state: CallRecordsUiState,
    onIntent: (CallRecordsIntent) -> Unit,
    onNavigateTab: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tabs = CallRecordsTab.entries
    val pagerState = rememberPagerState(
        initialPage = state.selectedTab.ordinal,
        pageCount = { tabs.size },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                onIntent(CallRecordsIntent.SelectTab(tabs[page]))
            }
    }
    LaunchedEffect(state.selectedTab) {
        val index = state.selectedTab.ordinal
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
            CallRecordsHeader(
                coinBalance = state.coinBalance,
                onCoinsClick = { onIntent(CallRecordsIntent.OpenCoins) },
            )
            CallRecordsSegmentedControl(
                selected = state.selectedTab,
                onSelect = { onIntent(CallRecordsIntent.SelectTab(it)) },
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
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
                val tab = tabs[pageIndex]
                val page = state.pages[tab] ?: CallRecordsTabPage()
                CallRecordsTabPageContent(
                    page = page,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        CallRecordsBottomBar(
            selected = CallRecordsBottomTab.Records,
            onSelect = onNavigateTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallRecordsTabPageContent(
    page: CallRecordsTabPage,
    onIntent: (CallRecordsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, page.hasMore, page.isLoadingMore, page.records.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 3
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (page.hasMore && !page.isLoadingMore && !page.isLoading && !page.isRefreshing) {
                    onIntent(CallRecordsIntent.LoadMore)
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = page.isRefreshing,
        onRefresh = { onIntent(CallRecordsIntent.Refresh) },
        modifier = modifier,
    ) {
        when {
            page.isLoading || (!page.hasLoaded && page.records.isEmpty() && page.errorMessage == null) -> {
                CallRecordsSkeletonList(modifier = Modifier.fillMaxSize())
            }
            page.errorMessage != null && page.records.isEmpty() -> {
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
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = { onIntent(CallRecordsIntent.Refresh) }) {
                        Text(stringResource(R.string.call_records_retry))
                    }
                }
            }
            page.records.isEmpty() -> {
                CallRecordsEmptyState(
                    onStartVideoChat = { onIntent(CallRecordsIntent.StartVideoChat) },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Spacing.lg),
                ) {
                    itemsIndexed(
                        items = page.records,
                        key = { _, item -> item.id },
                    ) { index, record ->
                        CallRecordRow(
                            record = record,
                            onRowClick = { onIntent(CallRecordsIntent.OpenProfile(record)) },
                            onCallClick = { onIntent(CallRecordsIntent.StartCall(record)) },
                        )
                        if (index < page.records.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = Spacing.md),
                                color = DemoColors.divider,
                                thickness = 1.dp,
                            )
                        }
                    }
                    if (page.isLoadingMore) {
                        item(key = "loading-more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(IconSize.md),
                                    color = DemoColors.link,
                                    strokeWidth = 2.dp,
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
private fun CallRecordsHeader(
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
                text = stringResource(R.string.call_records_title),
                color = DemoColors.navTitle,
                fontSize = TextSize.title,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                )
                Image(
                    painter = painterResource(R.drawable.call_records_ic_coin),
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.onlineCoinIcon),
                )
                Image(
                    painter = painterResource(R.drawable.call_records_ic_chevron),
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.sm - Spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun CallRecordsSegmentedControl(
    selected: CallRecordsTab,
    onSelect: (CallRecordsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.callRecordsSegmentHeight)
            .clip(RoundedCornerShape(Radius.sm))
            .background(DemoColors.segmentTrack)
            .padding(Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CallRecordsTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(
                                    elevation = Spacing.xs,
                                    shape = RoundedCornerShape(Radius.sm - Spacing.xxs),
                                    ambientColor = DemoColors.segmentSelectedShadow,
                                    spotColor = DemoColors.segmentSelectedShadow,
                                )
                                .clip(RoundedCornerShape(Radius.sm - Spacing.xxs))
                                .background(DemoColors.sheet)
                        } else {
                            Modifier.clip(RoundedCornerShape(Radius.sm - Spacing.xxs))
                        },
                    )
                    .clickable { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(tab.labelRes),
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.xs,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CallRecordRow(
    record: CallRecordUi,
    onRowClick: () -> Unit,
    onCallClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (record.tone) {
        CallRecordTone.Negative -> DemoColors.callRecordsNegative
        CallRecordTone.Positive -> DemoColors.callRecordsPositive
    }
    val statusIcon = when (record.tone) {
        CallRecordTone.Negative -> R.drawable.call_records_ic_status_missed
        CallRecordTone.Positive -> R.drawable.call_records_ic_status_ok
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(record.avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = record.nickname,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(ComponentSize.callRecordsAvatar)
                .clip(CircleShape)
                .background(DemoColors.chip),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = record.title,
                color = DemoColors.textPrimary,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Image(
                    painter = painterResource(statusIcon),
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.callRecordsStatusIcon),
                )
                Text(
                    text = record.statusText,
                    color = statusColor,
                    fontSize = TextSize.callRecordsStatus,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(IconSize.action - Spacing.chipGap)
                .clip(CircleShape)
                .clickable(onClick = onCallClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.call_records_ic_action_video),
                contentDescription = stringResource(R.string.call_records_cd_video),
                modifier = Modifier.size(ComponentSize.callRecordsAction),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun CallRecordsEmptyState(
    onStartVideoChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.call_records_ill_empty),
            contentDescription = null,
            modifier = Modifier
                .size(
                    width = ComponentSize.callRecordsEmptyWidth,
                    height = ComponentSize.callRecordsEmptyHeight,
                ),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.call_records_empty_title),
            color = DemoColors.textPrimary,
            fontSize = TextSize.md,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.call_records_empty_subtitle),
            color = DemoColors.textSecondary,
            fontSize = TextSize.xs,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.callRecordsCtaHeight)
                .clip(RoundedCornerShape(Radius.pill))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(DemoColors.gradientStart, DemoColors.gradientEnd),
                    ),
                )
                .clickable(onClick = onStartVideoChat),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.chipGap, Alignment.CenterHorizontally),
        ) {
            Image(
                painter = painterResource(R.drawable.call_records_ic_cta_video),
                contentDescription = null,
                modifier = Modifier.size(IconSize.sm),
            )
            Text(
                text = stringResource(R.string.call_records_empty_cta),
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.md,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CallRecordsSkeletonList(
    modifier: Modifier = Modifier,
    count: Int = 8,
) {
    val pulse = rememberCallRecordsSkeletonPulse()
    val loadingCd = stringResource(R.string.call_records_cd_loading)
    Column(
        modifier = modifier.semantics { contentDescription = loadingCd },
    ) {
        repeat(count) { index ->
            CallRecordRowSkeleton(pulse = pulse)
            if (index < count - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Spacing.md),
                    color = DemoColors.divider.copy(alpha = pulse),
                    thickness = 1.dp,
                )
            }
        }
    }
}

@Composable
private fun CallRecordRowSkeleton(
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val fill = DemoColors.skeleton.copy(alpha = pulse)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(ComponentSize.callRecordsAvatar)
                .clip(CircleShape)
                .background(fill),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width = ComponentSize.callRecordsSkeletonNameWidth,
                        height = ComponentSize.callRecordsSkeletonNameHeight,
                    )
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(fill),
            )
            Box(
                modifier = Modifier
                    .size(
                        width = ComponentSize.callRecordsSkeletonStatusWidth,
                        height = ComponentSize.callRecordsSkeletonStatusHeight,
                    )
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(fill),
            )
        }
        Box(
            modifier = Modifier
                .size(ComponentSize.callRecordsAction)
                .clip(CircleShape)
                .background(fill),
        )
    }
}

@Composable
private fun rememberCallRecordsSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "callRecordsSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "callRecordsSkeletonPulse",
    )
    return pulse
}

private enum class CallRecordsBottomTab {
    Home,
    Records,
    Match,
    Chat,
    Profile,
}

@Composable
private fun CallRecordsBottomBar(
    selected: CallRecordsBottomTab,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.call_records_bg_tab_bar),
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
                CallRecordsTabIcon(
                    iconRes = if (selected == CallRecordsBottomTab.Home) {
                        R.drawable.call_records_ic_tab_home
                    } else {
                        R.drawable.call_records_ic_tab_home_inactive
                    },
                    onClick = { onSelect("home") },
                )
                CallRecordsTabIcon(
                    iconRes = if (selected == CallRecordsBottomTab.Records) {
                        R.drawable.call_records_ic_tab_feed
                    } else {
                        R.drawable.call_records_ic_tab_feed_inactive
                    },
                    onClick = { onSelect("call-records") },
                )
                CallRecordsMatchTab(onClick = { onSelect("match") })
                CallRecordsTabIcon(
                    iconRes = if (selected == CallRecordsBottomTab.Chat) {
                        R.drawable.call_records_ic_tab_chat
                    } else {
                        R.drawable.call_records_ic_tab_chat_inactive
                    },
                    onClick = { onSelect("chat") },
                )
                CallRecordsTabIcon(
                    iconRes = if (selected == CallRecordsBottomTab.Profile) {
                        R.drawable.call_records_ic_tab_profile
                    } else {
                        R.drawable.call_records_ic_tab_profile_inactive
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
private fun CallRecordsMatchTab(onClick: () -> Unit) {
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
            painter = painterResource(R.drawable.call_records_ic_tab_match),
            contentDescription = stringResource(R.string.call_records_nav_match),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun CallRecordsTabIcon(
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

private val CallRecordsTab.labelRes: Int
    @StringRes get() = when (this) {
        CallRecordsTab.All -> R.string.call_records_tab_all
        CallRecordsTab.Unanswered -> R.string.call_records_tab_unanswered
        CallRecordsTab.Matches -> R.string.call_records_tab_matches
    }

@PreviewScreenSizes
@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun CallRecordsScreenPreview() {
    DemoTheme {
        CallRecordsScreen(
            state = CallRecordsUiState(
                coinBalance = 40,
                pages = mapOf(
                    CallRecordsTab.All to CallRecordsTabPage(
                        records = listOf(
                            CallRecordUi(
                                id = 1,
                                peerId = "1",
                                peerExternalUserId = "1",
                                nickname = "Madelyn",
                                age = 22,
                                avatarUrl = null,
                                statusText = "Missed the match",
                                tone = CallRecordTone.Negative,
                            ),
                            CallRecordUi(
                                id = 2,
                                peerId = "2",
                                peerExternalUserId = "2",
                                nickname = "Livia",
                                age = 22,
                                avatarUrl = null,
                                statusText = "Random match, 11s",
                                tone = CallRecordTone.Positive,
                            ),
                        ),
                        hasLoaded = true,
                    ),
                    CallRecordsTab.Unanswered to CallRecordsTabPage(hasLoaded = true),
                    CallRecordsTab.Matches to CallRecordsTabPage(hasLoaded = true),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun CallRecordsEmptyPreview() {
    DemoTheme {
        CallRecordsEmptyState(onStartVideoChat = {}, modifier = Modifier.fillMaxSize())
    }
}

@Preview
@Composable
private fun CallRecordsSkeletonPreview() {
    DemoTheme {
        CallRecordsSkeletonList(modifier = Modifier.fillMaxSize())
    }
}
