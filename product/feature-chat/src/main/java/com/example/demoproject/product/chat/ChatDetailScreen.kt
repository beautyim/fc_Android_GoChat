package com.example.demoproject.product.chat

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.product.store.CoinPayGuideSheet
import com.example.demoproject.product.vip.VipPayGuideSheet
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.designsystem.gift.GiftSvgaOverlay
import com.example.demoproject.ui.designsystem.media.MediaViewer
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import com.example.demoproject.ui.foundation.R as FoundationR
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.unit.sp

@Composable
fun ChatDetailScreen(
    viewModel: ChatDetailViewModel,
    onBack: () -> Unit,
    onOpenPeerProfile: (externalUserId: String) -> Unit,
    onStartVideoCall: (peerUserId: String) -> Unit,
    onOpenStore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    DisposableEffect(activity) {
        viewModel.bindActivity(activity)
        onDispose { viewModel.bindActivity(null) }
    }
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val value = uri?.toString()?.takeIf { it.isNotBlank() } ?: return@rememberLauncherForActivityResult
        viewModel.onIntent(ChatDetailIntent.SendPickedImage(value))
    }
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val value = uri?.toString()?.takeIf { it.isNotBlank() } ?: return@rememberLauncherForActivityResult
        viewModel.onIntent(ChatDetailIntent.SendPickedVideo(value))
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                ChatDetailEffect.NavigateBack -> onBack()
                is ChatDetailEffect.OpenPeerProfile -> onOpenPeerProfile(effect.externalUserId)
                is ChatDetailEffect.StartVideoCall -> onStartVideoCall(effect.peerUserId)
                is ChatDetailEffect.ShowMessage ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                ChatDetailEffect.PickSendImage -> pickImageLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
                ChatDetailEffect.PickSendVideo -> pickVideoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                )
                ChatDetailEffect.OpenMoreMenu -> Toast.makeText(
                    context, context.getString(R.string.chat_detail_soon_more), Toast.LENGTH_SHORT,
                ).show()
                ChatDetailEffect.OpenStore -> onOpenStore()
            }
        }
    }
    ChatDetailScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
    )
}

private const val ChatEmojiPanelAnimMillis = 280

private val ChatEmojiPanelEnter = slideInVertically(
    animationSpec = tween(
        durationMillis = ChatEmojiPanelAnimMillis,
        easing = FastOutSlowInEasing,
    ),
    initialOffsetY = { it },
) + fadeIn(
    animationSpec = tween(
        durationMillis = ChatEmojiPanelAnimMillis,
        easing = FastOutSlowInEasing,
    ),
)

private val ChatEmojiPanelExit = slideOutVertically(
    animationSpec = tween(
        durationMillis = ChatEmojiPanelAnimMillis,
        easing = FastOutSlowInEasing,
    ),
    targetOffsetY = { it },
) + fadeOut(
    animationSpec = tween(
        durationMillis = ChatEmojiPanelAnimMillis,
        easing = FastOutSlowInEasing,
    ),
)

@Composable
fun ChatDetailScreen(
    state: ChatDetailUiState,
    onIntent: (ChatDetailIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val navBottomPx = WindowInsets.navigationBars.getBottom(density)
    val imeVisible = imeBottomPx > 0
    val emojiPanelTotalPx = with(density) {
        ComponentSize.chatEmojiPanelHairline.roundToPx() +
            ComponentSize.chatEmojiPanelHeight.roundToPx()
    } + navBottomPx
    // While emoji → keyboard, keep at least the panel height until IME catches up.
    var imeHandoffFloorPx by remember { mutableIntStateOf(0) }
    // Block auto-dismiss only while IME is still closing after Keyboard → Emoji.
    var suppressImeEmojiDismiss by remember { mutableStateOf(false) }
    val composerFocusRequester = remember { FocusRequester() }

    LaunchedEffect(imeBottomPx, imeHandoffFloorPx) {
        if (imeHandoffFloorPx <= 0) return@LaunchedEffect
        if (imeBottomPx >= imeHandoffFloorPx) {
            imeHandoffFloorPx = 0
            return@LaunchedEffect
        }
        // IME visible but shorter than the emoji panel: wait for growth; if settled, drop floor.
        kotlinx.coroutines.delay(500)
        if (imeHandoffFloorPx > 0 && imeBottomPx < imeHandoffFloorPx) {
            imeHandoffFloorPx = 0
        }
    }

    // Clear suppress once the keyboard is fully gone (or was never up).
    LaunchedEffect(suppressImeEmojiDismiss, imeVisible, state.isEmojiSheetVisible) {
        if (suppressImeEmojiDismiss && state.isEmojiSheetVisible && !imeVisible) {
            suppressImeEmojiDismiss = false
        }
    }

    // Input focus / keyboard button: dismiss emoji once IME is actually up.
    LaunchedEffect(imeVisible, state.isEmojiSheetVisible, suppressImeEmojiDismiss) {
        if (imeVisible && state.isEmojiSheetVisible && !suppressImeEmojiDismiss) {
            imeHandoffFloorPx = maxOf(imeHandoffFloorPx, emojiPanelTotalPx)
            onIntent(ChatDetailIntent.DismissEmojiSheet)
        }
    }

    // Bottom reservation: always max(IME, emoji-if-open, handoff floor, nav).
    // Never dips during keyboard ↔ emoji handoff; rises with IME when keyboard is taller.
    val targetBottomPx = maxOf(
        imeBottomPx,
        navBottomPx,
        if (state.isEmojiSheetVisible) emojiPanelTotalPx else 0,
        imeHandoffFloorPx,
    )
    val followImeLive = imeVisible || imeHandoffFloorPx > 0
    val bottomInsetPx by animateIntAsState(
        targetValue = targetBottomPx,
        animationSpec = if (followImeLive) {
            snap()
        } else {
            tween(
                durationMillis = ChatEmojiPanelAnimMillis,
                easing = FastOutSlowInEasing,
            )
        },
        label = "chatDetailBottomInset",
    )
    val bottomPanelDp = with(density) { bottomInsetPx.toDp() }
    val giftQuickBarVisible = state.showGiftQuickBar && state.gifts.isNotEmpty()
    val giftQuickBarInset by animateDpAsState(
        targetValue = if (giftQuickBarVisible) {
            ComponentSize.chatGiftQuickBarHeight + Spacing.xs
        } else {
            0.dp
        },
        animationSpec = tween(
            durationMillis = ChatEmojiPanelAnimMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "chatGiftQuickBarInset",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DemoColors.chatDetailPage)
                .then(
                    if (state.showGreetingGesture) {
                        Modifier.pointerInput(state.showGreetingGesture) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.changes.any { it.changedToDown() }) {
                                        onIntent(ChatDetailIntent.DismissGreeting)
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            ChatDetailTopBar(
                state = state,
                onBack = onBack,
                onOpenPeerProfile = { onIntent(ChatDetailIntent.OpenPeerProfile) },
                onFollow = { onIntent(ChatDetailIntent.Follow) },
                onMore = { onIntent(ChatDetailIntent.OpenMore) },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (imeVisible || state.isEmojiSheetVisible) {
                            Modifier.pointerInput(imeVisible, state.isEmojiSheetVisible) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.changes.any { it.changedToDown() }) {
                                            if (imeVisible) keyboard?.hide()
                                            if (state.isEmojiSheetVisible) {
                                                imeHandoffFloorPx = 0
                                                onIntent(ChatDetailIntent.DismissEmojiSheet)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                when {
                    state.isLoading && state.items.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center).size(IconSize.md),
                            color = DemoColors.link,
                            strokeWidth = Spacing.xxs,
                        )
                    }
                    state.errorMessage != null && state.items.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(Spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Text(
                                text = state.errorMessage,
                                color = DemoColors.textSecondary,
                                fontSize = TextSize.sm,
                                textAlign = TextAlign.Center,
                            )
                            TextButton(onClick = { onIntent(ChatDetailIntent.RetryLoad) }) {
                                Text(stringResource(R.string.chat_retry))
                            }
                        }
                    }
                    else -> ChatDetailMessageList(
                        state = state,
                        onIntent = onIntent,
                        bottomContentPadding = giftQuickBarInset,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.showGreetingGesture,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = ChatEmojiPanelAnimMillis,
                            easing = FastOutSlowInEasing,
                        ),
                    ),
                    exit = fadeOut(
                        animationSpec = tween(
                            durationMillis = ChatEmojiPanelAnimMillis,
                            easing = FastOutSlowInEasing,
                        ),
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom = giftQuickBarInset + ComponentSize.chatGreetingAboveGiftBar,
                        ),
                ) {
                    ChatGreetingWave(
                        enabled = true,
                        onClick = { onIntent(ChatDetailIntent.SendGreeting) },
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = giftQuickBarVisible,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = ChatEmojiPanelAnimMillis,
                            easing = FastOutSlowInEasing,
                        ),
                    ),
                    exit = fadeOut(
                        animationSpec = tween(
                            durationMillis = ChatEmojiPanelAnimMillis,
                            easing = FastOutSlowInEasing,
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    ChatGiftQuickBar(
                        gifts = state.gifts,
                        enabled = !state.isGiftSending,
                        onSendGift = { onIntent(ChatDetailIntent.SendQuickGift(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.xs),
                    )
                }
            }
            ChatDetailComposer(
                state = state,
                onIntent = onIntent,
                focusRequester = composerFocusRequester,
                onToggleEmoji = {
                    if (state.isEmojiSheetVisible) {
                        // Emoji → keyboard: hold panel height so bar only rises with IME.
                        suppressImeEmojiDismiss = false
                        imeHandoffFloorPx = maxOf(bottomInsetPx, emojiPanelTotalPx)
                        composerFocusRequester.requestFocus()
                        keyboard?.show()
                    } else {
                        // Keyboard/idle → emoji: open panel first so max(ime, emoji) never dips.
                        imeHandoffFloorPx = 0
                        if (imeVisible) {
                            suppressImeEmojiDismiss = true
                            keyboard?.hide()
                        } else {
                            suppressImeEmojiDismiss = false
                        }
                        onIntent(ChatDetailIntent.OpenEmoji)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bottomPanelDp)
                    .clipToBounds(),
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.isEmojiSheetVisible,
                    enter = ChatEmojiPanelEnter,
                    exit = ChatEmojiPanelExit,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DemoColors.sheet),
                    ) {
                        ChatEmojiSheet(
                            onSelect = { onIntent(ChatDetailIntent.InsertEmoji(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .align(Alignment.TopCenter),
                        )
                    }
                }
            }
        }
        if (state.isGiftSheetVisible) {
            ChatGiftSheet(
                nickname = state.nickname,
                avatarUrl = state.peerAvatarUrl,
                coinBalance = state.coinBalance,
                gifts = state.gifts,
                selectedGiftId = state.selectedGiftId,
                isCatalogLoading = state.isGiftCatalogLoading,
                isSending = state.isGiftSending,
                onDismiss = { onIntent(ChatDetailIntent.DismissGiftSheet) },
                onSelectGift = { onIntent(ChatDetailIntent.SelectGift(it)) },
                onSend = { onIntent(ChatDetailIntent.SendGift) },
                onOpenCoins = { onIntent(ChatDetailIntent.OpenCoins) },
            )
        }
        if (state.isMediaTypeSheetVisible) {
            ChatMediaTypeSheet(
                onSendImage = { onIntent(ChatDetailIntent.SelectSendImage) },
                onSendVideo = { onIntent(ChatDetailIntent.SelectSendVideo) },
                onDismiss = { onIntent(ChatDetailIntent.DismissMediaTypeSheet) },
            )
        }
        state.vipPayGuide?.let { guide ->
            VipPayGuideSheet(
                state = guide,
                onDismiss = { onIntent(ChatDetailIntent.DismissVipPayGuide) },
                onUpgrade = { onIntent(ChatDetailIntent.PurchaseVipPayGuide) },
            )
        }
        state.coinPayGuide?.let { guide ->
            CoinPayGuideSheet(
                state = guide,
                onDismiss = { onIntent(ChatDetailIntent.DismissCoinPayGuide) },
                onPurchaseCoin = { onIntent(ChatDetailIntent.PurchaseCoinPayGuideCoin(it)) },
                onPurchaseSale = { onIntent(ChatDetailIntent.PurchaseCoinPayGuideSale(it)) },
            )
        }
        state.giftAnimationUrl?.let { url ->
            GiftSvgaOverlay(
                svgaUrl = url,
                onFinished = { onIntent(ChatDetailIntent.DismissGiftAnimation) },
            )
        }
        state.mediaViewerIndex?.let { index ->
            MediaViewer(
                items = state.mediaViewerItems,
                initialIndex = index,
                onDismiss = { onIntent(ChatDetailIntent.DismissMediaPreview) },
                showVideoChat = false,
            )
        }
    }
}

private val ChatDetailTitleStyle = TextStyle(
    fontSize = TextSize.xs,
    lineHeight = 14.sp,
    fontWeight = FontWeight.Medium,
    color = DemoColors.textPrimary,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

private val ChatDetailOnlineStyle = TextStyle(
    fontSize = TextSize.caption,
    lineHeight = 12.sp,
    fontWeight = FontWeight.Normal,
    color = DemoColors.textAuxiliary,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** Figma 176:4234 — 10sp / 12sp line height (13 lines → 156dp). */
private val ChatDetailSafetyTipsStyle = TextStyle(
    fontSize = TextSize.caption,
    lineHeight = 12.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

@Composable
private fun ChatDetailTopBar(
    state: ChatDetailUiState,
    onBack: () -> Unit,
    onOpenPeerProfile: () -> Unit,
    onFollow: () -> Unit,
    onMore: () -> Unit,
) {
    // Figma 176:4116 — 375×56: back@16, avatar@52, title@96/13, online@96/35+102/31,
    // follow@291, more@335; circular press feedback on back / follow / more.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DemoColors.sheet)
            .statusBarsPadding()
            .height(ComponentSize.chatDetailTopBar)
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatDetailTopBarIcon(
            iconRes = R.drawable.chat_ic_back,
            contentDescription = stringResource(FoundationR.string.action_back),
            onClick = onBack,
        )
        Spacer(Modifier.width(ComponentSize.chatDetailTopBarBackGap))
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenPeerProfile),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ComponentSize.chatDetailTopBarAvatarGap),
        ) {
            ChatAvatar(state.peerAvatarUrl, state.nickname, ComponentSize.chatDetailHeaderAvatar)
            Column(
                verticalArrangement = Arrangement.spacedBy(ComponentSize.chatDetailTopBarTitleGap),
            ) {
                Text(
                    text = state.title.ifBlank { stringResource(R.string.chat_detail_title_fallback) },
                    style = ChatDetailTitleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.isOnline) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            ComponentSize.chatDetailTopBarOnlineGap,
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ComponentSize.chatDetailOnlineDot)
                                .clip(CircleShape)
                                .background(DemoColors.chatOnlineDot),
                        )
                        Text(
                            text = stringResource(R.string.chat_detail_online),
                            style = ChatDetailOnlineStyle,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        when {
            state.showFollowAction -> {
                ChatDetailTopBarIcon(
                    iconRes = R.drawable.chat_ic_follow,
                    contentDescription = null,
                    onClick = onFollow,
                )
                Spacer(Modifier.width(ComponentSize.chatDetailTopBarActionGap))
            }
            state.showFollowedFlash -> {
                Image(
                    painter = painterResource(R.drawable.chat_ic_followed),
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.md),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(ComponentSize.chatDetailTopBarActionGap))
            }
        }
        ChatDetailTopBarIcon(
            iconRes = R.drawable.chat_ic_more,
            contentDescription = null,
            onClick = onMore,
        )
    }
}

@Composable
private fun ChatDetailTopBarIcon(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(IconSize.md)
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(IconSize.md),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun ChatDetailMessageList(
    state: ChatDetailUiState,
    onIntent: (ChatDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
) {
    // reverseLayout + newest-first display: index 0 is the visual bottom on the first frame,
    // so entering the room never paints the top then jumps.
    val listState = rememberLazyListState()
    val displayItems = remember(state.items) { state.items.asReversed() }

    LaunchedEffect(listState, state.hasMore, state.isLoadingMore, state.isLoading) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 2
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (state.hasMore && !state.isLoadingMore && !state.isLoading) {
                    onIntent(ChatDetailIntent.LoadMore)
                }
            }
    }
    val newestItemKey = state.items.lastOrNull()?.key
    LaunchedEffect(newestItemKey, state.isLoadingMore) {
        if (displayItems.isEmpty() || state.isLoadingMore) return@LaunchedEffect
        // reverseLayout: index 0 = visual bottom. A new "Today …" TimeSeparator lands at
        // index 1; local→server id remount can bump firstVisibleItemIndex past 1 and the
        // old <=1 gate would skip scrollToItem — outbound bubbles then fail to pin.
        val newestIsMine =
            (displayItems.firstOrNull() as? ChatDetailListItem.MessageRow)?.message?.isMine == true
        if (newestIsMine || listState.firstVisibleItemIndex <= 2) {
            listState.scrollToItem(0)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        reverseLayout = true,
        contentPadding = PaddingValues(
            start = Spacing.chipGap,
            end = Spacing.chipGap,
            // reverseLayout: bottom padding lands at the visual bottom (composer side).
            top = Spacing.sm,
            bottom = Spacing.sm + bottomContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(ComponentSize.chatDetailMessageGap),
    ) {
        items(items = displayItems, key = { it.key }) { item ->
            when (item) {
                ChatDetailListItem.SafetyTips -> ChatDetailSafetyTips()
                is ChatDetailListItem.ProfileCard -> ChatDetailProfileCard(
                    card = item,
                    onOpenPeerProfile = { onIntent(ChatDetailIntent.OpenPeerProfile) },
                )
                is ChatDetailListItem.TimeSeparator -> Text(
                    text = item.label,
                    color = DemoColors.textAuxiliary,
                    fontSize = TextSize.xs,
                    lineHeight = TextSize.chatSystemNoticeLine,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        // List gap is 12dp; Figma wants 16dp under the separator (172:3649).
                        .padding(bottom = ComponentSize.chatDetailTimeSeparatorExtraGap),
                )
                is ChatDetailListItem.MessageRow -> ChatDetailMessageRow(
                    message = item.message,
                    peerAvatarUrl = state.peerAvatarUrl,
                    selfAvatarUrl = state.selfAvatarUrl,
                    isTranslating = state.translatingMessageId == item.message.id,
                    onIntent = onIntent,
                )
            }
        }
        // With reverseLayout, trailing DSL items sit at the visual top (history side).
        if (state.isLoadingMore) {
            item(key = "loading-more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
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

@Composable
private fun ChatDetailSafetyTips(modifier: Modifier = Modifier) {
    val text = buildAnnotatedString {
        fun bold(s: String) = withStyle(
            SpanStyle(fontWeight = FontWeight.Bold, color = DemoColors.textPrimary),
        ) { append(s) }
        fun body(s: String) = withStyle(SpanStyle(color = DemoColors.textAuxiliary)) { append(s) }
        bold(stringResource(R.string.chat_detail_safety_title))
        append('\n')
        body(stringResource(R.string.chat_detail_safety_welcome))
        append("\n\n")
        bold(stringResource(R.string.chat_detail_safety_respect_title))
        append('\n')
        body(stringResource(R.string.chat_detail_safety_respect_body))
        append("\n\n")
        bold(stringResource(R.string.chat_detail_safety_appropriate_title))
        append('\n')
        body(stringResource(R.string.chat_detail_safety_appropriate_body))
        append("\n\n")
        bold(stringResource(R.string.chat_detail_safety_safe_title))
        append('\n')
        body(stringResource(R.string.chat_detail_safety_safe_body))
        append("\n\n")
        body(stringResource(R.string.chat_detail_safety_fun))
    }
    Text(
        text = text,
        style = ChatDetailSafetyTipsStyle,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.chatBanner))
            .background(DemoColors.sheet)
            .padding(Spacing.sm),
    )
}

@Composable
private fun ChatDetailProfileCard(
    card: ChatDetailListItem.ProfileCard,
    onOpenPeerProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.chatBanner))
            .background(DemoColors.sheet)
            .clickable(onClick = onOpenPeerProfile)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.chipGap),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md - Spacing.xxs),
        ) {
            ChatAvatar(card.avatarUrl, card.nickname, ComponentSize.chatDetailProfileAvatar)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = card.title,
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.md,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val country = listOf(card.countryFlag, card.countryName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                if (country.isNotBlank()) {
                    Text(
                        text = country,
                        color = DemoColors.textAuxiliary,
                        fontSize = TextSize.xs,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (card.photoUrls.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xxs)) {
                card.photoUrls.forEachIndexed { index, url ->
                    Box(
                        modifier = Modifier
                            .size(
                                width = ComponentSize.chatDetailProfilePhotoWidth,
                                height = ComponentSize.chatDetailProfilePhotoHeight,
                            )
                            .clip(RoundedCornerShape(Radius.chatProfilePhoto)),
                    ) {
                        ChatAsyncImage(url, Modifier.fillMaxSize())
                        if (index == card.photoUrls.lastIndex && card.extraPhotoCount > 0) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(DemoColors.chatMediaScrim),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.chat_detail_extra_photos,
                                        card.extraPhotoCount,
                                    ),
                                    color = DemoColors.onPrimaryButton,
                                    fontSize = TextSize.md,
                                    fontWeight = FontWeight.SemiBold,
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
private fun ChatDetailMessageRow(
    message: ChatDetailMessageUi,
    peerAvatarUrl: String?,
    selfAvatarUrl: String?,
    isTranslating: Boolean,
    onIntent: (ChatDetailIntent) -> Unit,
) {
    when (val body = message.body) {
        is ChatDetailMessageBody.SystemNotice -> Text(
            text = body.text,
            color = DemoColors.textAuxiliary,
            fontSize = TextSize.xs,
            lineHeight = TextSize.chatSystemNoticeLine,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        else -> {
            val isMine = message.isMine
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(ComponentSize.chatDetailBubbleTimeGap),
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    if (!isMine) {
                        ChatAvatar(
                            peerAvatarUrl,
                            null,
                            ComponentSize.chatDetailBubbleAvatar,
                        ) { onIntent(ChatDetailIntent.OpenPeerProfile) }
                    }
                    ChatDetailBubbleContent(message, body, isTranslating, onIntent)
                    if (isMine) {
                        ChatAvatar(selfAvatarUrl, null, ComponentSize.chatDetailBubbleAvatar)
                    }
                }
                Text(
                    text = message.timeLabel,
                    color = DemoColors.textAuxiliary,
                    fontSize = TextSize.caption,
                    lineHeight = TextSize.chatTimestampLine,
                    modifier = Modifier.padding(
                        start = if (isMine) 0.dp else ComponentSize.chatDetailTimeStampIndent,
                        end = if (isMine) ComponentSize.chatDetailTimeStampIndent else 0.dp,
                    ),
                    textAlign = if (isMine) TextAlign.End else TextAlign.Start,
                )
            }
        }
    }
}

@Composable
private fun ChatDetailBubbleContent(
    message: ChatDetailMessageUi,
    body: ChatDetailMessageBody,
    isTranslating: Boolean,
    onIntent: (ChatDetailIntent) -> Unit,
) {
    val isMine = message.isMine
    val isTextLike = body is ChatDetailMessageBody.Text || body is ChatDetailMessageBody.Emoji
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (isMine) {
            OutboundSideAction(
                status = message.status,
                showTranslate = isTextLike,
                translated = (body as? ChatDetailMessageBody.Text)?.translatedText != null,
                isTranslating = isTranslating,
                onResend = { onIntent(ChatDetailIntent.Resend(message.id)) },
                onTranslate = { onIntent(ChatDetailIntent.Translate(message.id)) },
            )
        }
        when (body) {
            is ChatDetailMessageBody.Text -> TextBubble(body.text, body.translatedText, isMine)
            is ChatDetailMessageBody.Emoji -> TextBubble(body.text, null, isMine)
            is ChatDetailMessageBody.Image -> MediaBubble(body.url, body.locked, null) {
                onIntent(ChatDetailIntent.OpenMedia(message.id))
            }
            is ChatDetailMessageBody.Video -> MediaBubble(body.url, body.locked, body.durationLabel) {
                onIntent(ChatDetailIntent.OpenMedia(message.id))
            }
            is ChatDetailMessageBody.Gift -> GiftBubble(
                gift = body,
                onSendGift = { onIntent(ChatDetailIntent.SendRequestedGift(message.id)) },
                onPlayAnimation = { onIntent(ChatDetailIntent.PlayGiftAnimation(message.id)) },
            )
            is ChatDetailMessageBody.Call -> CallBubble(body, isMine)
            is ChatDetailMessageBody.SystemNotice -> Unit
        }
        if (!isMine && isTextLike) {
            TranslateChip(
                done = (body as? ChatDetailMessageBody.Text)?.translatedText != null,
                loading = isTranslating,
            ) {
                onIntent(ChatDetailIntent.Translate(message.id))
            }
        }
    }
}

/**
 * Outgoing side action sits where the translate chip is:
 * Sending → loading, Failed → resend, Sent text → translate.
 */
@Composable
private fun OutboundSideAction(
    status: ChatDetailMessageStatus,
    showTranslate: Boolean,
    translated: Boolean,
    isTranslating: Boolean,
    onResend: () -> Unit,
    onTranslate: () -> Unit,
) {
    when (status) {
        ChatDetailMessageStatus.Sending -> {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(DemoColors.chatActionChip)
                    .padding(Spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ComponentSize.chatDetailTranslateIcon),
                    color = DemoColors.link,
                    strokeWidth = Spacing.xxs,
                )
            }
        }
        ChatDetailMessageStatus.Failed -> {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .clickable(onClick = onResend)
                    .padding(Spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.chat_ic_resend),
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.chatDetailTranslateIcon),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        ChatDetailMessageStatus.Sent -> if (showTranslate) {
            TranslateChip(done = translated, loading = isTranslating, onClick = onTranslate)
        }
    }
}

@Composable
private fun TextBubble(text: String, translatedText: String?, isMine: Boolean) {
    val bg = if (isMine) DemoColors.chatBubbleOutgoing else DemoColors.chatBubbleIncoming
    val fg = if (isMine) DemoColors.onPrimaryButton else DemoColors.textPrimary
    Column(
        modifier = Modifier
            .widthIn(max = ComponentSize.chatDetailGiftWidth)
            .clip(RoundedCornerShape(Radius.chatBubble))
            .background(bg)
            .padding(horizontal = Spacing.sm + Spacing.xxs, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(text = text, color = fg, fontSize = TextSize.sm)
        if (!translatedText.isNullOrBlank()) {
            HorizontalDivider(color = fg.copy(alpha = 0.25f))
            Text(text = translatedText, color = fg, fontSize = TextSize.sm)
        }
    }
}

@Composable
private fun TranslateChip(done: Boolean, loading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(DemoColors.chatActionChip)
            .clickable(enabled = !loading, onClick = onClick)
            .padding(Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ComponentSize.chatDetailTranslateIcon),
                color = DemoColors.link,
                strokeWidth = Spacing.xxs,
            )
        } else {
            Image(
                painter = painterResource(
                    if (done) R.drawable.chat_ic_translate_done else R.drawable.chat_ic_translate,
                ),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.chatDetailTranslateIcon),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun MediaBubble(
    url: String,
    locked: Boolean,
    durationLabel: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(
                width = ComponentSize.chatDetailMediaWidth,
                height = ComponentSize.chatDetailMediaHeight,
            )
            .clip(RoundedCornerShape(Radius.chatBubble))
            .clickable(onClick = onClick),
    ) {
        ChatAsyncImage(url, Modifier.fillMaxSize())
        if (locked) {
            Box(
                modifier = Modifier.fillMaxSize().background(DemoColors.chatMediaScrim),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(IconSize.md)
                        .clip(CircleShape)
                        .background(DemoColors.chatMediaLockCircle),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.chat_ic_lock),
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.xs),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
        if (!durationLabel.isNullOrBlank()) {
            Text(
                text = durationLabel,
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.caption,
                modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.sm),
            )
        }
    }
}

@Composable
private fun GiftBubble(
    gift: ChatDetailMessageBody.Gift,
    onSendGift: () -> Unit,
    onPlayAnimation: () -> Unit,
) {
    val header = if (gift.isRequest) {
        stringResource(R.string.chat_detail_gift_request, gift.peerName)
    } else {
        stringResource(R.string.chat_detail_gift_sent)
    }
    val playCd = stringResource(R.string.chat_detail_gift_cd_play)
    Box(
        modifier = Modifier
            .size(
                width = ComponentSize.chatDetailGiftWidth,
                height = ComponentSize.chatDetailGiftHeight,
            )
            .clip(RoundedCornerShape(Radius.chatBubble))
            .then(
                if (gift.canPlayAnimation) {
                    Modifier
                        .clickable(role = Role.Button, onClick = onPlayAnimation)
                        .semantics { contentDescription = playCd }
                } else {
                    Modifier
                },
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.chat_bg_gift_card),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.md),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = header,
                color = DemoColors.textPrimary,
                fontSize = TextSize.xs,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                GiftBubbleIcon(
                    iconUrl = gift.iconUrl,
                    modifier = Modifier.size(ComponentSize.chatDetailGiftIcon),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                ) {
                    Text(
                        text = gift.title,
                        color = DemoColors.textSecondary,
                        fontSize = TextSize.xs,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (gift.price != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.chat_ic_coin),
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.xs + Spacing.xs),
                                contentScale = ContentScale.Fit,
                            )
                            Text(
                                text = gift.price.toString(),
                                color = DemoColors.chatGiftCoin,
                                fontSize = TextSize.xs,
                            )
                        }
                    }
                }
                if (gift.isRequest) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(DemoGradients.primaryButton)
                            .clickable(onClick = onSendGift)
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs + Spacing.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.chat_ic_gift_white),
                            contentDescription = null,
                            modifier = Modifier.size(ComponentSize.chatDetailTranslateIcon),
                            contentScale = ContentScale.Fit,
                        )
                        Text(
                            text = stringResource(R.string.chat_detail_send_gift),
                            color = DemoColors.onPrimaryButton,
                            fontSize = TextSize.caption,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Falls back to the local gift glyph while the CDN icon loads, or when the payload has none. */
@Composable
private fun GiftBubbleIcon(iconUrl: String, modifier: Modifier = Modifier) {
    val fallback = painterResource(R.drawable.chat_ic_gift)
    if (iconUrl.isBlank()) {
        Image(
            painter = fallback,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
        return
    }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(iconUrl).crossfade(true).build(),
        contentDescription = null,
        placeholder = fallback,
        error = fallback,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Composable
private fun CallBubble(call: ChatDetailMessageBody.Call, isMine: Boolean) {
    val bg = if (isMine) DemoColors.chatBubbleOutgoing else DemoColors.chatBubbleIncoming
    val icon = when {
        call.isMissed -> R.drawable.chat_ic_call_missed
        isMine -> R.drawable.chat_ic_call_on_primary
        else -> R.drawable.chat_ic_call
    }
    val textColor = when {
        call.isMissed -> DemoColors.chatCallMissed
        isMine -> DemoColors.onPrimaryButton
        else -> DemoColors.textPrimary
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.chatBubble))
            .background(bg)
            .padding(horizontal = Spacing.sm + Spacing.xxs, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xxs),
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(IconSize.xs + Spacing.xs),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = call.label,
            color = textColor,
            fontSize = TextSize.sm,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ChatDetailComposer(
    state: ChatDetailUiState,
    onIntent: (ChatDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onToggleEmoji: () -> Unit = {
        if (state.isEmojiSheetVisible) {
            onIntent(ChatDetailIntent.DismissEmojiSheet)
        } else {
            onIntent(ChatDetailIntent.OpenEmoji)
        }
    },
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val resolvedFocusRequester = focusRequester ?: remember { FocusRequester() }
    val placeholder = if (state.freeMessageCount > 0 && state.draft.isBlank()) {
        stringResource(R.string.chat_detail_free_messages_left, state.freeMessageCount)
    } else {
        stringResource(R.string.chat_detail_input_hint)
    }
    val canSend = state.draft.isNotBlank()
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = state.draft, selection = TextRange(state.draft.length)))
    }
    LaunchedEffect(state.draft) {
        if (fieldValue.text != state.draft) {
            fieldValue = TextFieldValue(
                text = state.draft,
                selection = TextRange(state.draft.length),
            )
        }
    }
    Row(
        modifier = modifier
            .height(ComponentSize.chatDetailComposerBar)
            .background(DemoColors.sheet)
            .padding(horizontal = Spacing.chipGap, vertical = Spacing.xs + Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.chipGap),
    ) {
        ComposerCircle(R.drawable.chat_ic_plus) {
            keyboard?.hide()
            onIntent(ChatDetailIntent.OpenPlusMenu)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(ComponentSize.chatDetailComposerField)
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoColors.chatComposerField)
                .padding(start = Spacing.chipGap, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = fieldValue,
                onValueChange = { next ->
                    fieldValue = next
                    onIntent(ChatDetailIntent.DraftChanged(next.text))
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.xs,
                ),
                cursorBrush = SolidColor(DemoColors.link),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRequester(resolvedFocusRequester),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (fieldValue.text.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = DemoColors.inputPlaceholder,
                                fontSize = TextSize.xs,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .size(ComponentSize.chatDetailComposerControl)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onToggleEmoji,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(
                        if (state.isEmojiSheetVisible) {
                            R.drawable.chat_ic_keyboard
                        } else {
                            R.drawable.chat_ic_emoji
                        },
                    ),
                    contentDescription = stringResource(
                        if (state.isEmojiSheetVisible) {
                            R.string.chat_detail_cd_keyboard
                        } else {
                            R.string.chat_detail_cd_emoji
                        },
                    ),
                    modifier = Modifier.size(IconSize.sm),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        ComposerCircle(R.drawable.chat_ic_gift) {
            keyboard?.hide()
            onIntent(ChatDetailIntent.OpenGiftPanel)
        }
        if (state.composerShowsVideoCall) {
            VideoCallButton { onIntent(ChatDetailIntent.StartVideoCall) }
        } else {
            Box(
                modifier = Modifier
                    .size(ComponentSize.chatDetailComposerControl)
                    .clip(CircleShape)
                    .background(if (canSend) DemoColors.link else DemoColors.chatComposerField)
                    .clickable(enabled = canSend) {
                        onIntent(ChatDetailIntent.SendText)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.chat_ic_send),
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.xs + Spacing.xs),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun ComposerCircle(iconRes: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(ComponentSize.chatDetailComposerControl)
            .clip(CircleShape)
            .background(DemoColors.chatComposerField)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(IconSize.sm),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun VideoCallButton(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "videoShine")
    val shine by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "videoShineProgress",
    )
    val shineColor = DemoColors.onPrimaryButton
    Box(
        modifier = Modifier
            .size(ComponentSize.chatDetailComposerControl)
            .clip(CircleShape)
            .background(DemoColors.link)
            .drawWithContent {
                drawContent()
                val band = size.width * 0.55f
                val startX = (shine * (size.width + band)) - band
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            shineColor.copy(alpha = 0f),
                            shineColor.copy(alpha = 0.45f),
                            shineColor.copy(alpha = 0f),
                        ),
                        start = Offset(startX, 0f),
                        end = Offset(startX + band, size.height),
                    ),
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.chat_ic_video),
            contentDescription = null,
            modifier = Modifier.size(IconSize.xs + Spacing.xs),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun ChatAvatar(
    url: String?,
    contentDescription: String?,
    size: Dp,
    onClick: (() -> Unit)? = null,
) {
    val base = Modifier.size(size).clip(CircleShape).background(DemoColors.chip)
    ChatAsyncImage(
        url = url,
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        contentDescription = contentDescription,
    )
}

@Composable
private fun ChatAsyncImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

private val PreviewDetailItems = listOf(
    ChatDetailListItem.SafetyTips,
    ChatDetailListItem.ProfileCard(
        nickname = "Terry",
        age = 23,
        avatarUrl = null,
        countryFlag = "🇸🇬",
        countryName = "Singapore",
        photoUrls = listOf("", "", "", ""),
        extraPhotoCount = 3,
    ),
    ChatDetailListItem.TimeSeparator(label = "Today 9:30 AM", key = "time_preview"),
    ChatDetailListItem.MessageRow(
        ChatDetailMessageUi(
            id = "1",
            isMine = false,
            status = ChatDetailMessageStatus.Sent,
            createdAtMillis = 1L,
            timeLabel = "9:30",
            body = ChatDetailMessageBody.Text("Hey! How's your day going?"),
        ),
    ),
    ChatDetailListItem.MessageRow(
        ChatDetailMessageUi(
            id = "2",
            isMine = true,
            status = ChatDetailMessageStatus.Sent,
            createdAtMillis = 2L,
            timeLabel = "9:30",
            body = ChatDetailMessageBody.Text("Hey! It's going great, thanks!\nHow about you?"),
        ),
    ),
    ChatDetailListItem.MessageRow(
        ChatDetailMessageUi(
            id = "3",
            isMine = false,
            status = ChatDetailMessageStatus.Sent,
            createdAtMillis = 3L,
            timeLabel = "9:31",
            body = ChatDetailMessageBody.Text(
                text = "Pretty good! Just finished a workout.",
                translatedText = "꽤 좋아요! 운동을 막 끝냈어요.",
            ),
        ),
    ),
)

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "ChatDetail", locale = "en")
@Preview(name = "ChatDetail RTL", locale = "ar")
@Composable
private fun ChatDetailScreenPreview() {
    DemoTheme {
        ChatDetailScreen(
            state = ChatDetailUiState(
                nickname = "Terry",
                age = 23,
                isOnline = true,
                freeMessageCount = 10,
                items = PreviewDetailItems,
                isLoading = false,
                hasLoaded = true,
                hasMore = false,
            ),
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "ChatDetail FirstVisit", locale = "en")
@Composable
private fun ChatDetailFirstVisitPreview() {
    DemoTheme {
        ChatDetailScreen(
            state = ChatDetailUiState(
                nickname = "Terry",
                age = 23,
                isOnline = true,
                freeMessageCount = 10,
                showGiftQuickBar = true,
                showGreetingGesture = true,
                gifts = listOf(
                    ChatGiftUi(id = 1, title = "Lollipop", price = 50, iconUrl = ""),
                    ChatGiftUi(id = 2, title = "Lips", price = 50, iconUrl = ""),
                    ChatGiftUi(id = 3, title = "Berry", price = 50, iconUrl = ""),
                    ChatGiftUi(id = 4, title = "Lipstick", price = 50, iconUrl = ""),
                ),
                items = PreviewDetailItems,
                isLoading = false,
                hasLoaded = true,
                hasMore = false,
            ),
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "ChatDetail Emoji", locale = "en")
@Composable
private fun ChatDetailEmojiSheetPreview() {
    DemoTheme {
        ChatDetailScreen(
            state = ChatDetailUiState(
                nickname = "Terry",
                age = 23,
                isOnline = true,
                draft = "hdcsbdj",
                isEmojiSheetVisible = true,
                items = PreviewDetailItems,
                isLoading = false,
                hasLoaded = true,
                hasMore = false,
            ),
            onIntent = {},
            onBack = {},
        )
    }
}
