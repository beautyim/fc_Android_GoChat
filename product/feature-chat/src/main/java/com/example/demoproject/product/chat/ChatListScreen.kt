package com.example.demoproject.product.chat

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoConfirmDialog
import com.example.demoproject.ui.designsystem.DemoNavIconButton
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.roundToInt

/** Figma 1:1875 — 14sp / 17sp line box. */
private val ChatListNameStyle = TextStyle(
    fontSize = TextSize.sm,
    lineHeight = TextSize.chatListNameLine,
    fontWeight = FontWeight.SemiBold,
    color = DemoColors.textPrimary,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** Figma 1:1876 — 12sp / 14sp line box. */
private val ChatListPreviewStyle = TextStyle(
    fontSize = TextSize.xs,
    lineHeight = TextSize.chatListPreviewLine,
    fontWeight = FontWeight.Normal,
    color = DemoColors.chatPreview,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** Figma 1:1877 / 1:1879 — 11sp / 13sp line box. */
private val ChatListMetaStyle = TextStyle(
    fontSize = TextSize.meMeta,
    lineHeight = TextSize.chatListMetaLine,
    fontWeight = FontWeight.Normal,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** Figma 1:1645 — 12sp / 14sp line box on notification banner. */
private val ChatNotifTitleStyle = TextStyle(
    fontSize = TextSize.xs,
    lineHeight = TextSize.chatNotifTitleLine,
    fontWeight = FontWeight.Normal,
    color = DemoColors.textPrimary,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** Figma 1:1646 — 10sp / 12sp line box on notification banner. */
private val ChatNotifSubtitleStyle = TextStyle(
    fontSize = TextSize.caption,
    lineHeight = TextSize.chatNotifSubtitleLine,
    fontWeight = FontWeight.Normal,
    color = DemoColors.textAuxiliary,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** Figma 1:1651 — 10sp / 12sp on Turn On pill. */
private val ChatNotifCtaStyle = TextStyle(
    fontSize = TextSize.caption,
    lineHeight = TextSize.chatNotifCtaLine,
    fontWeight = FontWeight.Normal,
    color = DemoColors.onPrimaryButton,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onNavigateTab: (String) -> Unit,
    onOpenChatDetail: (conversationId: String, nickname: String) -> Unit,
    onOpenHome: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onIntent(ChatListIntent.NotificationPermissionResult)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ChatListEffect.OpenChatDetail -> {
                    onOpenChatDetail(effect.conversationId, effect.nickname)
                }
                ChatListEffect.OpenHome -> onOpenHome()
                ChatListEffect.OpenSupport -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.chat_support_soon),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                ChatListEffect.RequestNotificationPermission -> onRequestNotificationPermission()
                is ChatListEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ChatListScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateTab = onNavigateTab,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    state: ChatListUiState,
    onIntent: (ChatListIntent) -> Unit,
    onNavigateTab: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.sheet),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatListHeader(
                onSupportClick = { onIntent(ChatListIntent.OpenSupport) },
            )
            if (state.showNotificationBanner) {
                ChatNotificationBanner(
                    onTurnOn = { onIntent(ChatListIntent.TurnOnNotifications) },
                    onDismiss = { onIntent(ChatListIntent.DismissNotificationBanner) },
                    modifier = Modifier
                        .padding(horizontal = ComponentSize.chatListHorizontalInset)
                        .padding(bottom = Spacing.chipGap),
                )
            }
            ChatListBody(
                state = state,
                onIntent = onIntent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = ComponentSize.onlineTabBarBodyHeight),
            )
        }
        ChatListBottomBar(
            selected = ChatBottomTab.Chat,
            chatUnreadCount = state.totalUnread,
            onSelect = onNavigateTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        val pendingDelete = state.pendingDeleteId
        if (pendingDelete != null) {
            DemoConfirmDialog(
                title = stringResource(R.string.chat_delete_title),
                body = stringResource(R.string.chat_delete_body),
                negativeText = stringResource(R.string.chat_delete_cancel),
                positiveText = stringResource(R.string.chat_delete_confirm),
                onNegative = { onIntent(ChatListIntent.CancelDelete) },
                onPositive = { onIntent(ChatListIntent.ConfirmDelete) },
                isPositiveLoading = state.isDeleting,
                illustrationRes = R.drawable.chat_ic_delete_dialog,
                positiveContainerColor = DemoColors.dialogDestructive,
                titleFontSize = TextSize.sm,
                bodyFontSize = TextSize.xs,
                minHeight = ComponentSize.confirmDialogDestructiveMinHeight,
            )
        }
    }
}

@Composable
private fun ChatListHeader(
    onSupportClick: () -> Unit,
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
                text = stringResource(R.string.chat_title),
                color = DemoColors.navTitle,
                fontSize = TextSize.title,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            DemoNavIconButton(
                icon = painterResource(R.drawable.chat_ic_support),
                contentDescription = stringResource(R.string.chat_cd_support),
                onClick = onSupportClick,
            )
        }
    }
}

@Composable
private fun ChatNotificationBanner(
    onTurnOn: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Figma 1:1640 — white card, 8dp radius, 1dp hairline, 12/8 padding, purple 2dp glow.
    val bannerShape = RoundedCornerShape(Radius.chatBanner)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = Spacing.xxs,
                shape = bannerShape,
                ambientColor = DemoColors.chatBannerShadow,
                spotColor = DemoColors.chatBannerShadow,
            )
            .clip(bannerShape)
            .background(DemoColors.sheet)
            .border(ComponentSize.chatBannerStroke, DemoColors.chatBannerBorder, bannerShape)
            .padding(horizontal = ComponentSize.chatListHorizontalInset, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(ComponentSize.chatNotifBell)
                .clip(CircleShape)
                .background(DemoColors.chatNotifBellBg),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.chat_ic_notif_bell),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.chatNotifBell),
                contentScale = ContentScale.Fit,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.chat_notif_title),
                style = ChatNotifTitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(ComponentSize.chatNotifTitleToSubtitle))
            Text(
                text = stringResource(R.string.chat_notif_subtitle),
                style = ChatNotifSubtitleStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Figma: Turn On ↔ close gap 8dp (309 − 301).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .height(ComponentSize.chatNotifCtaHeight)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(DemoColors.gradientStart, DemoColors.gradientEnd),
                        ),
                    )
                    .clickable(onClick = onTurnOn)
                    .padding(horizontal = ComponentSize.chatNotifCtaHorizontal),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.chat_notif_turn_on),
                    style = ChatNotifCtaStyle,
                    maxLines = 1,
                )
            }
            DemoNavIconButton(
                icon = painterResource(R.drawable.chat_ic_banner_close),
                contentDescription = stringResource(R.string.chat_cd_dismiss_banner),
                onClick = onDismiss,
                iconSize = ComponentSize.chatNotifClose,
                size = ComponentSize.chatNotifClose,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatListBody(
    state: ChatListUiState,
    onIntent: (ChatListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.hasMore, state.isLoadingMore, state.conversations.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 3
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (state.hasMore && !state.isLoadingMore && !state.isLoading && !state.isRefreshing) {
                    onIntent(ChatListIntent.LoadMore)
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onIntent(ChatListIntent.Refresh) },
        modifier = modifier,
    ) {
        when {
            state.isLoading || (!state.hasLoaded && state.conversations.isEmpty() && state.errorMessage == null) -> {
                ChatSkeletonList(modifier = Modifier.fillMaxSize())
            }
            state.errorMessage != null && state.conversations.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterVertically),
                ) {
                    Text(
                        text = state.errorMessage,
                        color = DemoColors.textSecondary,
                        fontSize = TextSize.sm,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = { onIntent(ChatListIntent.Refresh) }) {
                        Text(stringResource(R.string.chat_retry))
                    }
                }
            }
            state.conversations.isEmpty() -> {
                ChatEmptyState(
                    onStartChatting = { onIntent(ChatListIntent.StartChatting) },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = ComponentSize.chatListHorizontalInset,
                        end = ComponentSize.chatListHorizontalInset,
                        top = ComponentSize.chatListTopInset,
                        bottom = Spacing.lg,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.chipGap),
                ) {
                    itemsIndexed(
                        items = state.conversations,
                        key = { _, item -> item.id },
                    ) { _, item ->
                        ChatConversationRow(
                            conversation = item,
                            revealed = state.revealedConversationId == item.id,
                            onRevealChange = { open ->
                                onIntent(
                                    ChatListIntent.RevealActions(
                                        if (open) item.id else null,
                                    ),
                                )
                            },
                            onClick = { onIntent(ChatListIntent.OpenConversation(item)) },
                            onPin = { onIntent(ChatListIntent.TogglePin(item.id)) },
                            onDelete = { onIntent(ChatListIntent.RequestDelete(item.id)) },
                        )
                    }
                    if (state.isLoadingMore) {
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
private fun ChatConversationRow(
    conversation: ChatConversationUi,
    revealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val actionsWidthPx = with(density) {
        (ComponentSize.chatSwipeAction * 2).toPx()
    }
    var dragOffset by remember(conversation.id) { mutableFloatStateOf(0f) }
    var isDragging by remember(conversation.id) { mutableStateOf(false) }
    LaunchedEffect(revealed, conversation.id) {
        if (!isDragging) {
            dragOffset = if (revealed) -actionsWidthPx else 0f
        }
    }
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = tween(durationMillis = if (isDragging) 0 else 180),
        label = "chatSwipeOffset",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.chatRowHeight),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(ComponentSize.chatSwipeAction * 2),
        ) {
            Box(
                modifier = Modifier
                    .width(ComponentSize.chatSwipeAction)
                    .fillMaxHeight()
                    .background(DemoColors.chatSwipePin)
                    .clickable(onClick = onPin),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(
                        if (conversation.isPinned) {
                            R.drawable.chat_ic_swipe_unpin
                        } else {
                            R.drawable.chat_ic_swipe_pin
                        },
                    ),
                    contentDescription = stringResource(
                        if (conversation.isPinned) R.string.chat_cd_unpin else R.string.chat_cd_pin,
                    ),
                    modifier = Modifier.size(IconSize.sm),
                    contentScale = ContentScale.Fit,
                )
            }
            Box(
                modifier = Modifier
                    .width(ComponentSize.chatSwipeAction)
                    .fillMaxHeight()
                    .background(DemoColors.chatSwipeDelete)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.chat_ic_swipe_delete),
                    contentDescription = stringResource(R.string.chat_cd_delete),
                    modifier = Modifier.size(IconSize.sm),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        Row(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .fillMaxSize()
                .background(DemoColors.sheet)
                .pointerInput(conversation.id, actionsWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            val open = dragOffset <= -actionsWidthPx / 2f
                            dragOffset = if (open) -actionsWidthPx else 0f
                            isDragging = false
                            onRevealChange(open)
                        },
                        onDragCancel = {
                            dragOffset = if (revealed) -actionsWidthPx else 0f
                            isDragging = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset = (dragOffset + dragAmount)
                                .coerceIn(-actionsWidthPx, 0f)
                        },
                    )
                }
                .clickable {
                    if (revealed) {
                        onRevealChange(false)
                    } else {
                        onClick()
                    }
                },
            // Figma 1:1873 — avatar top; name/preview and meta top-biased in the 68dp cell.
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(ComponentSize.chatRowTextStartGap),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(conversation.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = conversation.nickname,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ComponentSize.callRecordsAvatar)
                    .clip(CircleShape)
                    .background(DemoColors.chip),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        top = ComponentSize.chatRowTextTopInset,
                        end = Spacing.sm,
                    ),
                verticalArrangement = Arrangement.spacedBy(ComponentSize.chatRowNamePreviewGap),
            ) {
                Text(
                    text = conversation.title,
                    style = ChatListNameStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = conversation.preview,
                    style = ChatListPreviewStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                modifier = Modifier.padding(top = ComponentSize.chatRowMetaTopInset),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(ComponentSize.chatRowMetaGap),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    if (conversation.isPinned) {
                        Image(
                            painter = painterResource(R.drawable.chat_ic_pinned),
                            contentDescription = null,
                            modifier = Modifier.size(ComponentSize.chatPinnedIcon),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Text(
                        text = conversation.timestampLabel,
                        style = ChatListMetaStyle.copy(color = DemoColors.chatTimestamp),
                        maxLines = 1,
                    )
                }
                if (conversation.unreadCount > 0) {
                    // Figma 1:1878 — 19×19 pill, top 32; trailing inset 12 vs timestamp flush end.
                    Text(
                        text = if (conversation.unreadCount > 99) {
                            "99+"
                        } else {
                            conversation.unreadCount.toString()
                        },
                        style = ChatListMetaStyle.copy(
                            color = DemoColors.onPrimaryButton,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier
                            .padding(end = ComponentSize.chatUnreadBadgeEndInset)
                            .defaultMinSize(
                                minWidth = ComponentSize.chatUnreadBadgeMin,
                                minHeight = ComponentSize.chatUnreadBadgeMin,
                            )
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(DemoColors.meUnreadBadge)
                            .padding(
                                horizontal = ComponentSize.chatUnreadBadgeHorizontal,
                                vertical = ComponentSize.chatUnreadBadgeVertical,
                            ),
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            color = DemoColors.divider,
            thickness = 1.dp,
        )
    }
}

@Composable
private fun ChatEmptyState(
    onStartChatting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Gaps / CTA match Call Records so copy + button stay put.
    // Illustration paints at Figma 258×258 but layout height stays Call Records
    // empty slot (220×176) so Arrangement.Center does not shift text/CTA.
    val callRecordsSegmentChrome =
        ComponentSize.callRecordsSegmentHeight + Spacing.sm + Spacing.sm
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = callRecordsSegmentChrome)
            .padding(horizontal = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(
                width = ComponentSize.callRecordsEmptyWidth,
                height = ComponentSize.callRecordsEmptyHeight,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.chat_ill_empty),
                contentDescription = null,
                modifier = Modifier
                    .requiredSize(ComponentSize.chatEmptyIll)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.chat_empty_title),
            color = DemoColors.textPrimary,
            fontSize = TextSize.md,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.chat_empty_subtitle),
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
                .clickable(onClick = onStartChatting),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.chipGap, Alignment.CenterHorizontally),
        ) {
            Image(
                painter = painterResource(R.drawable.chat_ic_cta_smile),
                contentDescription = null,
                modifier = Modifier.size(IconSize.sm),
            )
            Text(
                text = stringResource(R.string.chat_empty_cta),
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.md,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ChatSkeletonList(
    modifier: Modifier = Modifier,
    count: Int = 8,
) {
    val pulse = rememberChatSkeletonPulse()
    val loadingCd = stringResource(R.string.chat_cd_loading)
    Column(
        modifier = modifier
            .semantics { contentDescription = loadingCd }
            .padding(
                horizontal = ComponentSize.chatListHorizontalInset,
                vertical = ComponentSize.chatListTopInset,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.chipGap),
    ) {
        repeat(count) { index ->
            ChatRowSkeleton(pulse = pulse)
            if (index < count - 1) {
                HorizontalDivider(color = DemoColors.divider.copy(alpha = pulse), thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun ChatRowSkeleton(
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val fill = DemoColors.skeleton.copy(alpha = pulse)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.chatRowHeight),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(ComponentSize.chatRowTextStartGap),
    ) {
        Box(
            modifier = Modifier
                .size(ComponentSize.callRecordsAvatar)
                .clip(CircleShape)
                .background(fill),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = ComponentSize.chatRowTextTopInset),
            verticalArrangement = Arrangement.spacedBy(ComponentSize.chatRowNamePreviewGap),
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width = ComponentSize.chatSkeletonNameWidth,
                        height = ComponentSize.chatSkeletonNameHeight,
                    )
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(fill),
            )
            Box(
                modifier = Modifier
                    .size(
                        width = ComponentSize.chatSkeletonPreviewWidth,
                        height = ComponentSize.chatSkeletonPreviewHeight,
                    )
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(fill),
            )
        }
    }
}

@Composable
private fun rememberChatSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "chatSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "chatSkeletonPulse",
    )
    return pulse
}

private enum class ChatBottomTab {
    Home,
    Feed,
    Match,
    Chat,
    Profile,
}

@Composable
private fun ChatListBottomBar(
    selected: ChatBottomTab,
    chatUnreadCount: Int,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.chat_bg_tab_bar),
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
                ChatTabIcon(
                    iconRes = if (selected == ChatBottomTab.Home) {
                        R.drawable.chat_ic_tab_home
                    } else {
                        R.drawable.chat_ic_tab_home_inactive
                    },
                    onClick = { onSelect("home") },
                )
                ChatTabIcon(
                    iconRes = if (selected == ChatBottomTab.Feed) {
                        R.drawable.chat_ic_tab_feed
                    } else {
                        R.drawable.chat_ic_tab_feed_inactive
                    },
                    onClick = { onSelect("feed") },
                )
                ChatMatchTab(onClick = { onSelect("match") })
                Box {
                    ChatTabIcon(
                        iconRes = if (selected == ChatBottomTab.Chat) {
                            R.drawable.chat_ic_tab_chat
                        } else {
                            R.drawable.chat_ic_tab_chat_inactive
                        },
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
                ChatTabIcon(
                    iconRes = if (selected == ChatBottomTab.Profile) {
                        R.drawable.chat_ic_tab_profile
                    } else {
                        R.drawable.chat_ic_tab_profile_inactive
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
private fun ChatMatchTab(onClick: () -> Unit) {
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
            painter = painterResource(R.drawable.chat_ic_tab_match),
            contentDescription = stringResource(R.string.chat_nav_match),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun ChatTabIcon(
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

private val PreviewConversations = listOf(
    ChatConversationUi(
        id = "1",
        peerId = "u1",
        peerExternalUserId = "u1",
        nickname = "Brayan",
        age = 19,
        avatarUrl = null,
        preview = "Hey! How's your day going?",
        timestampLabel = "09:42AM",
        unreadCount = 3,
        isPinned = true,
    ),
    ChatConversationUi(
        id = "2",
        peerId = "u2",
        peerExternalUserId = "u2",
        nickname = "Sophia",
        age = 23,
        avatarUrl = null,
        preview = "That looks so amazing!",
        timestampLabel = "09:42AM",
        unreadCount = 3,
        isPinned = false,
    ),
    ChatConversationUi(
        id = "3",
        peerId = "u3",
        peerExternalUserId = "u3",
        nickname = "Emma",
        age = 22,
        avatarUrl = null,
        preview = "Are we still on for tomorrow?",
        timestampLabel = "Yesterday",
        unreadCount = 0,
        isPinned = false,
    ),
)

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "ChatList", locale = "en")
@Preview(name = "ChatList RTL", locale = "ar")
@Composable
private fun ChatListScreenPreview() {
    DemoTheme {
        ChatListScreen(
            state = ChatListUiState(
                conversations = PreviewConversations,
                totalUnread = 6,
                hasLoaded = true,
                showNotificationBanner = true,
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "ChatEmpty")
@Composable
private fun ChatEmptyPreview() {
    DemoTheme {
        ChatListScreen(
            state = ChatListUiState(hasLoaded = true),
            onIntent = {},
        )
    }
}
