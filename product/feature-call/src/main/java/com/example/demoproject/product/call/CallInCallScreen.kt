package com.example.demoproject.product.call

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.platform.callkit.CallKitHolder
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.designsystem.gift.GiftSvgaOverlay
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import kotlinx.coroutines.delay

private const val CallChatInsertMillis = 260
private const val CallChatInsertFadeMillis = 200
private const val CallChatTranslationMillis = 220
private const val CallChatTranslationFadeInDelayMillis = 80
private const val CallChatTranslationFadeOutMillis = 120
private const val CallChatTranslateIconMillis = 160
private const val CallGiftSentTipFadeMillis = 280
private const val CallGiftSentTipVisibleMillis = 2_000L

private val CallChatItemEnter =
    expandVertically(
        animationSpec = tween(CallChatInsertMillis, easing = FastOutSlowInEasing),
        expandFrom = Alignment.Bottom,
        clip = false,
    ) + slideInVertically(
        animationSpec = tween(CallChatInsertMillis, easing = FastOutSlowInEasing),
        initialOffsetY = { it / 3 },
    ) + scaleIn(
        animationSpec = tween(CallChatInsertMillis, easing = FastOutSlowInEasing),
        initialScale = 0.94f,
        transformOrigin = TransformOrigin(0f, 1f),
    ) + fadeIn(
        animationSpec = tween(CallChatInsertFadeMillis, easing = LinearOutSlowInEasing),
    )

@Composable
internal fun CallInCallContent(
    state: CallUiState,
    onIntent: (CallIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CallInCallLayout(
        state = state,
        onIntent = onIntent,
        matchNextCountdownSec = null,
        modifier = modifier,
    )
}

@Composable
internal fun MatchingCallInCallContent(
    state: CallUiState,
    onIntent: (CallIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CallInCallLayout(
        state = state,
        onIntent = onIntent,
        matchNextCountdownSec = state.matchNextCountdownSec,
        modifier = modifier,
    )
}

@Composable
private fun CallInCallLayout(
    state: CallUiState,
    onIntent: (CallIntent) -> Unit,
    matchNextCountdownSec: Int?,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isImeVisible = imeBottomPx > 0
    var displayedGiftSentTip by remember { mutableStateOf<CallGiftSentTipUi?>(null) }
    LaunchedEffect(state.giftSentTip?.id) {
        val tip = state.giftSentTip ?: return@LaunchedEffect
        displayedGiftSentTip = tip
        delay(CallGiftSentTipVisibleMillis)
        onIntent(CallIntent.DismissGiftSentTip)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.profileVideoScrim),
    ) {
        CallInCallRemoteSurface(
            remoteUid = state.remoteRtcUid,
            rtcActive = state.rtcSurfacesActive,
            peerMasked = state.peerMasked,
            videoUrl = state.videoUrl,
            coverUrl = state.coverUrl,
            avatarUrl = state.peerAvatarUrl,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.callRingingTopGradient)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DemoColors.callRingingTopScrim,
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.callInCallBottomGradient)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DemoColors.callRingingTopScrim,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .then(
                    if (isImeVisible) {
                        Modifier.imePadding()
                    } else {
                        Modifier.navigationBarsPadding()
                    },
                ),
        ) {
            CallInCallHeader(
                state = state,
                showHangup = state.isMatchHangupEnabled,
                onLike = { onIntent(CallIntent.Like) },
                onReport = { onIntent(CallIntent.Report) },
                onHangup = { onIntent(CallIntent.Hangup) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Spacing.callRingingHeaderStart,
                        end = Spacing.callRingingReportEnd,
                        top = Spacing.sm,
                    ),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .then(
                        if (isImeVisible) {
                            Modifier.pointerInput(isImeVisible) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.changes.any { it.changedToDown() }) {
                                            keyboard?.hide()
                                        }
                                    }
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                CallInCallPip(
                    avatarUrl = state.peerAvatarUrl,
                    rtcActive = state.rtcSurfacesActive,
                    cameraEnabled = state.cameraEnabled && !state.isMatchReceiveOnly,
                    onFlipCamera = { onIntent(CallIntent.FlipCamera) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = Spacing.callRingingReportEnd, top = Spacing.md),
                )

                val hasOtherOverlay = state.isMoreSheetVisible ||
                    state.isGiftSheetVisible ||
                    state.isReportSheetVisible ||
                    state.giftAnimationUrl != null
                val showBalanceFloat = state.shouldShowBalanceFloat(
                    imeVisible = isImeVisible,
                    hasOtherOverlay = hasOtherOverlay,
                )
                if (showBalanceFloat) {
                    state.balanceOffer?.let { offer ->
                        CallBalanceAlertFloatingWindow(
                            offer = offer,
                            onClick = { onIntent(CallIntent.OpenBalanceOfferGuide) },
                            onMoreOptions = { onIntent(CallIntent.OpenBalanceOfferGuide) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(
                                    end = Spacing.callBalanceFloatEnd,
                                    bottom = Spacing.callBalanceFloatBottom,
                                ),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            end = ComponentSize.callInCallPipWidth + Spacing.md,
                            bottom = Spacing.sm,
                        ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.callInCallChatGap),
                    horizontalAlignment = Alignment.Start,
                ) {
                    AnimatedVisibility(
                        visible = state.giftSentTip != null,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = CallGiftSentTipFadeMillis,
                                easing = LinearOutSlowInEasing,
                            ),
                        ),
                        exit = fadeOut(
                            animationSpec = tween(
                                durationMillis = CallGiftSentTipFadeMillis,
                                easing = FastOutLinearInEasing,
                            ),
                        ),
                    ) {
                        val tip = displayedGiftSentTip
                        if (tip != null) {
                            CallGiftSentTip(tip = tip)
                        }
                    }
                    CallInCallChatColumn(
                        items = state.chatItems,
                        isGiftSending = state.isGiftSending,
                        sendingGiftId = state.selectedGiftId,
                        onToggleTranslation = { onIntent(CallIntent.ToggleMessageTranslation(it)) },
                        onSendGiftRequest = { giftId ->
                            onIntent(CallIntent.SendQuickGift(giftId))
                        },
                        modifier = Modifier
                            .padding(start = Spacing.callInCallChatStart)
                            .widthIn(max = 227.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = state.showGiftQuickBar && state.gifts.isNotEmpty(),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                CallGiftQuickBar(
                    gifts = state.gifts.take(8),
                    enabled = !state.isGiftSending,
                    onSendGift = { onIntent(CallIntent.SendQuickGift(it)) },
                    modifier = Modifier.padding(bottom = Spacing.callInCallGiftQuickBottom),
                )
            }

            CallInCallBottomBar(
                draft = state.draftMessage,
                imeVisible = isImeVisible,
                matchNextCountdownSec = matchNextCountdownSec
                    ?.takeIf { state.showMatchNext },
                matchNextEnabled = state.isMatchNextEnabled,
                onDraftChange = { onIntent(CallIntent.DraftChanged(it)) },
                onMore = { onIntent(CallIntent.OpenMore) },
                onGift = { onIntent(CallIntent.OpenGiftSheet) },
                onSend = { onIntent(CallIntent.SendMessage) },
                onNextMatch = { onIntent(CallIntent.NextMatch) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Spacing.callInCallBottomBarHorizontal,
                        vertical = Spacing.sm,
                    ),
            )
        }

        if (state.isMoreSheetVisible) {
            CallMoreSheet(
                micEnabled = state.micEnabled,
                cameraEnabled = state.cameraEnabled,
                onDismiss = { onIntent(CallIntent.DismissMore) },
                onMicChanged = { onIntent(CallIntent.SetMicEnabled(it)) },
                onCameraChanged = { onIntent(CallIntent.SetCameraEnabled(it)) },
            )
        }

        if (state.isGiftSheetVisible) {
            CallGiftSheet(
                nickname = state.peerNickname.ifBlank {
                    stringResource(R.string.call_ringing_unknown_peer)
                },
                avatarUrl = state.peerAvatarUrl.takeIf { it.isNotBlank() },
                coinBalance = state.coinBalance,
                gifts = state.gifts,
                selectedGiftId = state.selectedGiftId,
                isCatalogLoading = state.isGiftCatalogLoading,
                isSending = state.isGiftSending,
                onDismiss = { onIntent(CallIntent.DismissGiftSheet) },
                onSelectGift = { onIntent(CallIntent.SelectGift(it)) },
                onSend = { onIntent(CallIntent.SendSelectedGift) },
                onOpenCoins = { onIntent(CallIntent.OpenCoins) },
            )
        }

        state.giftAnimationUrl?.takeIf { it.isNotBlank() }?.let { url ->
            GiftSvgaOverlay(
                svgaUrl = url,
                onFinished = { onIntent(CallIntent.DismissGiftAnimation) },
            )
        }
    }
}

@Composable
private fun CallInCallHeader(
    state: CallUiState,
    showHangup: Boolean,
    onLike: () -> Unit,
    onReport: () -> Unit,
    onHangup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reportCd = stringResource(R.string.call_ringing_cd_report)
    val hangupCd = stringResource(R.string.call_ringing_cd_hangup)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(state.peerAvatarUrl.takeIf { it.isNotBlank() })
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(ComponentSize.callRingingAvatar)
                .clip(CircleShape)
                .background(DemoColors.callInCallOverlay),
        )
        Column(
            modifier = Modifier
                .padding(start = Spacing.callRingingAvatarGap)
                .weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.callInCallLikeGap),
            ) {
                Text(
                    text = state.displayName.ifBlank {
                        stringResource(R.string.call_ringing_unknown_peer)
                    },
                    color = DemoColors.callRingingOnVideo,
                    fontSize = TextSize.callRingingName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = callShadowStyle(),
                    modifier = Modifier.weight(1f, fill = false),
                )
                AnimatedVisibility(
                    visible = state.likePhase != CallLikePhase.Hidden,
                    enter = fadeIn(tween(160)),
                    exit = scaleOut(
                        targetScale = 1.45f,
                        transformOrigin = TransformOrigin.Center,
                        animationSpec = tween(
                            durationMillis = 320,
                            easing = FastOutLinearInEasing,
                        ),
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 260, delayMillis = 60),
                    ),
                ) {
                    CallLikeChip(
                        liked = state.likePhase != CallLikePhase.Visible,
                        onClick = onLike,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
                modifier = Modifier.padding(top = Spacing.callRingingStatusGap),
            ) {
                Image(
                    painter = painterResource(R.drawable.call_incall_ic_timer),
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.callInCallTimerIcon),
                )
                Text(
                    text = state.formattedDuration,
                    color = DemoColors.callRingingStatus,
                    fontSize = TextSize.sm,
                    fontWeight = FontWeight.Medium,
                    style = callShadowStyle(),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.callInCallHeaderEndGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CallCircleIconButton(
                iconRes = R.drawable.call_incall_ic_report,
                iconSize = ComponentSize.callInCallHeaderIcon,
                padding = Spacing.sm,
                contentDescription = reportCd,
                onClick = onReport,
                buttonSize = ComponentSize.callInCallHeaderAction,
            )
            if (showHangup) {
                CallCircleIconButton(
                    iconRes = R.drawable.call_incall_ic_hangup,
                    iconSize = ComponentSize.callInCallHangupIcon,
                    padding = Spacing.xs + Spacing.xxs,
                    contentDescription = hangupCd,
                    onClick = onHangup,
                    buttonSize = ComponentSize.callInCallHeaderAction,
                )
            }
        }
    }
}

@Composable
private fun CallLikeChip(
    liked: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (liked) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 520f),
        label = "likeScale",
    )
    val label = stringResource(
        if (liked) R.string.call_incall_liked else R.string.call_incall_like,
    )
    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(Radius.pill))
            .background(DemoColors.callInCallAccent)
            .clickable(
                enabled = !liked,
                role = Role.Button,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Crossfade(
            targetState = liked,
            animationSpec = tween(180),
            label = "likeIcon",
        ) { isLiked ->
            Image(
                painter = painterResource(
                    if (isLiked) R.drawable.call_incall_ic_liked else R.drawable.call_incall_ic_like,
                ),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.callInCallLikeIcon),
            )
        }
        Text(
            text = label,
            color = DemoColors.onPrimaryButton,
            fontSize = TextSize.callInCallLike,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun CallInCallPip(
    avatarUrl: String,
    rtcActive: Boolean,
    cameraEnabled: Boolean,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flipCd = stringResource(R.string.call_incall_cd_flip_camera)
    val rtc = CallKitHolder.rtc
    val localView = remember(rtc) { rtc?.createVideoView() }
    LaunchedEffect(rtcActive, localView, cameraEnabled) {
        if (rtcActive && localView != null && cameraEnabled) {
            rtc?.setupLocalVideo(localView)
            rtc?.refreshLocalPreview()
        }
    }
    DisposableEffect(localView) {
        onDispose {
            rtc?.setupLocalVideo(null)
        }
    }
    Box(
        modifier = modifier
            .width(ComponentSize.callInCallPipWidth)
            .height(ComponentSize.callInCallPipHeight)
            .clip(RoundedCornerShape(ComponentSize.callInCallPipRadius))
            .background(DemoColors.profileVideoScrim),
    ) {
        if (rtcActive && localView != null && cameraEnabled) {
            AndroidView(
                factory = {
                    (localView.parent as? ViewGroup)?.removeView(localView)
                    localView
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
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
        Image(
            painter = painterResource(R.drawable.call_incall_ic_flip_camera),
            contentDescription = flipCd,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.sm)
                .size(ComponentSize.callInCallFlipIcon)
                .semantics { contentDescription = flipCd }
                .clickable(
                    role = Role.Button,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onFlipCamera,
                ),
        )
    }
}

@Composable
private fun CallInCallChatColumn(
    items: List<CallChatItem>,
    isGiftSending: Boolean,
    sendingGiftId: Long?,
    onToggleTranslation: (String) -> Unit,
    onSendGiftRequest: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.maxValue }
            .collect { max -> scrollState.scrollTo(max) }
    }
    Column(
        modifier = modifier
            .heightIn(max = 320.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(Spacing.callInCallChatGap, Alignment.Bottom),
    ) {
        items.forEach { item ->
            key(item.id) {
                CallChatItemAppearance {
                    when (item) {
                        is CallChatItem.SystemTips -> CallSystemTipsBubble()
                        is CallChatItem.BillingTip -> CallBillingTipBubble(text = item.text)
                        is CallChatItem.Message -> CallMessageBubble(
                            item = item,
                            onToggleTranslation = { onToggleTranslation(item.id) },
                        )
                        is CallChatItem.GiftRequest -> CallGiftRequestCard(
                            item = item,
                            isSending = isGiftSending && sendingGiftId == item.giftId,
                            enabled = !isGiftSending,
                            onSendGift = { onSendGiftRequest(item.giftId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.CallChatItemAppearance(
    content: @Composable () -> Unit,
) {
    val appearState = remember { MutableTransitionState(false) }
    appearState.targetState = true
    AnimatedVisibility(
        visibleState = appearState,
        enter = CallChatItemEnter,
        exit = ExitTransition.None,
        label = "callChatItemAppear",
    ) {
        content()
    }
}

@Composable
private fun CallBillingTipBubble(text: String) {
    Text(
        text = text,
        color = DemoColors.callRingingOnVideo,
        fontSize = TextSize.callInCallChat,
        lineHeight = TextSize.callInCallChat * 1.4f,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.chatBubble))
            .background(DemoColors.callInCallOverlay)
            .padding(Spacing.sm),
    )
}

@Composable
private fun CallSystemTipsBubble() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.chatBubble))
            .background(DemoColors.callInCallOverlay)
            .padding(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.call_incall_system_title),
            color = DemoColors.callRingingOnVideo,
            fontSize = TextSize.callInCallSystemTitle,
        )
        Text(
            text = stringResource(R.string.call_incall_system_tips),
            color = DemoColors.callRingingOnVideo,
            fontSize = TextSize.callInCallChat,
            lineHeight = TextSize.callInCallChat * 1.5f,
            modifier = Modifier.padding(top = Spacing.xxs),
        )
    }
}

@Composable
private fun CallMessageBubble(
    item: CallChatItem.Message,
    onToggleTranslation: () -> Unit,
) {
    Box {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.chatBubble))
                .background(DemoColors.callInCallOverlay)
                .padding(Spacing.sm)
                .padding(end = if (item.isLocal) Spacing.md else 0.dp),
        ) {
            if (item.isLocal) {
                Text(
                    text = stringResource(R.string.call_incall_you_message_fmt, item.text),
                    color = DemoColors.callRingingOnVideo,
                    fontSize = TextSize.callInCallChat,
                )
            } else {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = DemoColors.callInCallAccent,
                                fontWeight = FontWeight.Medium,
                            ),
                        ) {
                            append(item.senderName)
                            append(": ")
                        }
                        withStyle(SpanStyle(color = DemoColors.callRingingOnVideo)) {
                            append(item.text)
                        }
                    },
                    fontSize = TextSize.callInCallChat,
                )
            }
            val translation = item.translation
            AnimatedVisibility(
                visible = item.showTranslation && !translation.isNullOrBlank(),
                enter = expandVertically(
                    animationSpec = tween(CallChatTranslationMillis, easing = FastOutSlowInEasing),
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = CallChatTranslationMillis - CallChatTranslationFadeInDelayMillis,
                        delayMillis = CallChatTranslationFadeInDelayMillis,
                        easing = LinearOutSlowInEasing,
                    ),
                ),
                exit = fadeOut(
                    animationSpec = tween(CallChatTranslationFadeOutMillis),
                ) + shrinkVertically(
                    animationSpec = tween(CallChatTranslationMillis, easing = FastOutSlowInEasing),
                ),
                label = "callMessageTranslation",
            ) {
                Column {
                    HorizontalDivider(
                        color = DemoColors.callInCallGiftQuickPrice,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = Spacing.xs),
                    )
                    Text(
                        text = translation.orEmpty(),
                        color = DemoColors.callRingingOnVideo,
                        fontSize = TextSize.callInCallChat,
                    )
                }
            }
        }
        if (item.isLocal) {
            val translateCd = stringResource(R.string.call_incall_cd_translate)
            val iconScale by animateFloatAsState(
                targetValue = if (item.showTranslation) 1.12f else 1f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
                label = "translateIconScale",
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .scale(iconScale)
                    .padding(Spacing.xxs)
                    .clip(CircleShape)
                    .background(DemoColors.callInCallOverlay)
                    .padding(Spacing.xxs)
                    .size(ComponentSize.callInCallTranslate)
                    .semantics { contentDescription = translateCd }
                    .clickable(
                        role = Role.Button,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onToggleTranslation,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(
                    targetState = item.showTranslation,
                    animationSpec = tween(CallChatTranslateIconMillis),
                    label = "translateIcon",
                ) { translated ->
                    Image(
                        painter = painterResource(
                            if (translated) {
                                R.drawable.call_incall_ic_translated
                            } else {
                                R.drawable.call_incall_ic_translate
                            },
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(ComponentSize.callInCallTranslate),
                    )
                }
            }
        }
    }
}

@Composable
private fun CallGiftSentTip(
    tip: CallGiftSentTipUi,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .height(ComponentSize.callInCallGiftSentTipHeight)
                .background(
                    color = DemoColors.callInCallGiftSentTip,
                    shape = RoundedCornerShape(
                        topEnd = Radius.callInCallGiftSentTipEnd,
                        bottomEnd = Radius.callInCallGiftSentTipEnd,
                    ),
                )
                .padding(
                    start = Spacing.callInCallGiftSentTipStart,
                    end = Spacing.xs,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = stringResource(R.string.call_incall_gift_sent_title),
                    color = DemoColors.callRingingOnVideo,
                    fontSize = TextSize.callInCallGiftSentTipTitle,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tip.giftTitle,
                    color = DemoColors.callInCallGiftSentTipSub,
                    fontSize = TextSize.callInCallGiftSentTipName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Layout width is only the in-bar portion; icon draws past the start into the purple bar
        // and past the top/bottom of the 44dp bar.
        Box(
            modifier = Modifier
                .width(
                    ComponentSize.callInCallGiftSentTipIcon - Spacing.callInCallGiftSentTipIconOverhang,
                )
                .height(ComponentSize.callInCallGiftSentTipIcon),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(tip.giftIconUrl.takeIf { it.isNotBlank() })
                    .crossfade(true)
                    .build(),
                contentDescription = tip.giftTitle,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(ComponentSize.callInCallGiftSentTipIcon)
                    .align(Alignment.CenterEnd),
            )
        }
        Text(
            text = stringResource(R.string.call_incall_gift_sent_count, tip.count),
            color = DemoColors.callRingingOnVideo,
            fontSize = TextSize.callInCallGiftSentTipCount,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(start = Spacing.callInCallGiftSentTipEndGap),
        )
    }
}

@Composable
private fun CallGiftRequestCard(
    item: CallChatItem.GiftRequest,
    isSending: Boolean,
    enabled: Boolean,
    onSendGift: () -> Unit,
) {
    val sendCd = stringResource(R.string.call_incall_gift_request_cta)
    val loadingCd = stringResource(R.string.call_records_cd_loading)
    Box(
        modifier = Modifier
            .widthIn(max = 227.dp)
            .clip(RoundedCornerShape(Radius.chatBubble))
            .background(DemoColors.callInCallOverlay)
            .padding(Spacing.sm),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = ComponentSize.callInCallGiftRequestDecor / 2),
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(item.senderName)
                    }
                    append(stringResource(R.string.call_incall_gift_request_mid))
                    withStyle(SpanStyle(color = DemoColors.callInCallGiftRequestAccent)) {
                        append(stringResource(R.string.call_incall_gift_request_highlight))
                    }
                },
                color = DemoColors.callRingingOnVideo,
                fontSize = TextSize.callInCallChat,
            )
            Text(
                text = stringResource(R.string.call_incall_gift_request_sub),
                color = DemoColors.callInCallGiftRequestSub,
                fontSize = TextSize.callInCallGiftRequestSub,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            Box(
                modifier = Modifier
                    .padding(top = Spacing.sm)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(DemoGradients.primaryButton)
                    .clickable(
                        enabled = enabled && !isSending,
                        role = Role.Button,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onSendGift,
                    )
                    .semantics {
                        contentDescription = if (isSending) loadingCd else sendCd
                    }
                    .padding(
                        start = Spacing.md,
                        end = Spacing.md + Spacing.xs,
                        top = Spacing.xs + Spacing.xxs,
                        bottom = Spacing.xs + Spacing.xxs,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.alpha(if (isSending) 0f else 1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Image(
                        painter = painterResource(R.drawable.call_incall_ic_gift),
                        contentDescription = null,
                        modifier = Modifier.size(ComponentSize.callInCallLikeIcon + Spacing.xs),
                    )
                    Text(
                        text = stringResource(R.string.call_incall_gift_request_cta),
                        color = DemoColors.onPrimaryButton,
                        fontSize = TextSize.callInCallGiftRequestCta,
                    )
                }
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ComponentSize.giftBalanceIcon),
                        color = DemoColors.onPrimaryButton,
                        strokeWidth = ComponentSize.profileProgressStroke,
                    )
                }
            }
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.giftIconUrl.takeIf { it.isNotBlank() })
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            placeholder = painterResource(R.drawable.call_incall_ic_gift),
            error = painterResource(R.drawable.call_incall_ic_gift),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(ComponentSize.callInCallGiftRequestDecor),
        )
    }
}

@Composable
private fun CallInCallBottomBar(
    draft: String,
    imeVisible: Boolean,
    matchNextCountdownSec: Int?,
    matchNextEnabled: Boolean,
    onDraftChange: (String) -> Unit,
    onMore: () -> Unit,
    onGift: () -> Unit,
    onSend: () -> Unit,
    onNextMatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val moreCd = stringResource(R.string.call_incall_cd_more)
    val giftCd = stringResource(R.string.call_incall_cd_gift)
    val sendCd = stringResource(R.string.call_incall_cd_send)
    val showSend = imeVisible
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.callInCallBottomBarGap),
    ) {
        CallCircleIconButton(
            iconRes = R.drawable.call_incall_ic_more,
            iconSize = ComponentSize.callInCallActionIcon,
            padding = Spacing.sm + Spacing.xxs,
            contentDescription = moreCd,
            onClick = onMore,
            buttonSize = ComponentSize.callInCallAction,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(ComponentSize.callInCallAction)
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoColors.callInCallOverlay)
                .padding(horizontal = Spacing.md),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (draft.isEmpty()) {
                Text(
                    text = stringResource(R.string.call_incall_message_hint),
                    color = DemoColors.callInCallPlaceholder,
                    fontSize = TextSize.sm,
                )
            }
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = DemoColors.callRingingOnVideo,
                    fontSize = TextSize.sm,
                ),
                cursorBrush = SolidColor(DemoColors.callRingingOnVideo),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier
                .size(ComponentSize.callInCallAction)
                .clip(CircleShape)
                .background(DemoGradients.primaryButton)
                .semantics {
                    contentDescription = if (showSend) sendCd else giftCd
                }
                .clickable(
                    role = Role.Button,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { if (showSend) onSend() else onGift() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(
                    if (showSend) R.drawable.call_incall_ic_send else R.drawable.call_incall_ic_gift,
                ),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.callInCallActionIcon),
            )
        }
        matchNextCountdownSec?.let { countdown ->
            CallMatchNextButton(
                countdownSec = countdown,
            enabled = matchNextEnabled,
                onClick = onNextMatch,
            )
        }
    }
}

@Composable
private fun CallMatchNextButton(
    countdownSec: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (countdownSec > 0) {
        stringResource(R.string.call_match_next_countdown, countdownSec)
    } else {
        stringResource(R.string.call_match_next)
    }
    val layoutDirection = LocalLayoutDirection.current
    Row(
        modifier = modifier
            .height(ComponentSize.callInCallAction)
            .widthIn(min = ComponentSize.callMatchNextMinWidth)
            .clip(RoundedCornerShape(Radius.pill))
            .background(DemoColors.callMatchNextEnabled)
            .alpha(if (enabled) 1f else 0.5f)
            .semantics { contentDescription = label }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(
                horizontal = if (enabled) {
                    Spacing.callMatchNextHorizontal
                } else {
                    Spacing.callMatchNextCountdownHorizontal
                },
                vertical = Spacing.xs,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = label,
            color = DemoColors.callRingingOnVideo,
            fontSize = TextSize.md,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Image(
            painter = painterResource(R.drawable.call_match_ic_next),
            contentDescription = null,
            modifier = Modifier
                .size(ComponentSize.callMatchNextIcon)
                .graphicsLayer {
                    scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
                },
        )
    }
}

@Composable
private fun CallCircleIconButton(
    iconRes: Int,
    iconSize: androidx.compose.ui.unit.Dp,
    padding: androidx.compose.ui.unit.Dp,
    contentDescription: String,
    onClick: () -> Unit,
    buttonSize: androidx.compose.ui.unit.Dp? = null,
) {
    Box(
        modifier = Modifier
            .then(
                if (buttonSize != null) {
                    Modifier.size(buttonSize)
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(DemoColors.callInCallOverlay)
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                role = Role.Button,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun CallInCallRemoteSurface(
    remoteUid: Int,
    rtcActive: Boolean,
    peerMasked: Boolean,
    videoUrl: String,
    coverUrl: String,
    avatarUrl: String,
    modifier: Modifier = Modifier,
) {
    val rtc = CallKitHolder.rtc
    val remoteView = remember(rtc) { rtc?.createRemoteVideoView() }
    LaunchedEffect(remoteUid, rtcActive, remoteView) {
        if (rtcActive && remoteUid > 0 && remoteView != null) {
            rtc?.setupRemoteVideo(remoteUid, remoteView)
        }
    }
    DisposableEffect(remoteView) {
        onDispose {
            if (remoteUid > 0) {
                rtc?.setupRemoteVideo(remoteUid, null)
            }
        }
    }
    Box(modifier = modifier) {
        val showRtc = rtcActive && remoteUid > 0 && remoteView != null && !peerMasked
        if (!showRtc) {
            // Once RTC is active, never fall back to ExoPlayer — it fights Agora surfaces
            // while waiting for remoteUid / after remote leaves.
            CallRingingBackground(
                videoUrl = if (rtcActive || peerMasked) "" else videoUrl,
                coverUrl = coverUrl,
                avatarUrl = avatarUrl,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AndroidView(
                factory = {
                    (remoteView.parent as? ViewGroup)?.removeView(remoteView)
                    remoteView
                },
                update = { view ->
                    if (remoteUid > 0) {
                        rtc?.setupRemoteVideo(remoteUid, view)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun callShadowStyle(): TextStyle = TextStyle(
    shadow = Shadow(
        color = DemoColors.callRingingTextShadow,
        blurRadius = 2f,
    ),
)

@PreviewScreenSizes
@PreviewFontScale
@Preview(showBackground = true, locale = "ar")
@Composable
private fun CallInCallPreview() {
    DemoTheme {
        CallInCallContent(
            state = CallUiState(
                phase = CallRingingPhase.InCall,
                peerNickname = "Isabella",
                peerAge = 23,
                callDurationSec = 23,
                showGiftQuickBar = true,
                giftSentTip = CallGiftSentTipUi(
                    giftTitle = "Heart",
                    giftIconUrl = "",
                    count = 1,
                ),
                gifts = listOf(
                    CallGiftUi(1, "A", 50, ""),
                    CallGiftUi(2, "B", 50, ""),
                    CallGiftUi(3, "C", 50, ""),
                ),
                chatItems = listOf(
                    CallChatItem.SystemTips(),
                    CallChatItem.Message(
                        id = "1",
                        isLocal = false,
                        senderName = "Isabella",
                        text = "You look amazing today!",
                    ),
                    CallChatItem.Message(
                        id = "2",
                        isLocal = true,
                        senderName = "You",
                        text = "Hi! Nice to see you",
                    ),
                    CallChatItem.GiftRequest(
                        id = "g",
                        senderName = "Isabella",
                        giftIconUrl = "",
                        giftPrice = 30,
                        giftId = 1,
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, name = "Liked")
@Composable
private fun CallInCallLikedPreview() {
    DemoTheme {
        CallInCallContent(
            state = CallUiState(
                phase = CallRingingPhase.InCall,
                peerNickname = "Isabella",
                peerAge = 23,
                callDurationSec = 23,
                likePhase = CallLikePhase.Liked,
            ),
            onIntent = {},
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(showBackground = true, locale = "ar", name = "Match call countdown")
@Composable
private fun MatchingCallInCallCountdownPreview() {
    DemoTheme {
        MatchingCallInCallContent(
            state = CallUiState(
                phase = CallRingingPhase.InCall,
                isMatchCall = true,
                peerNickname = "Isabella",
                peerAge = 23,
                callDurationSec = 0,
                matchTimeSeconds = 60,
                nextTimeSeconds = 6,
                showGiftQuickBar = true,
                gifts = listOf(
                    CallGiftUi(1, "A", 50, ""),
                    CallGiftUi(2, "B", 50, ""),
                    CallGiftUi(3, "C", 50, ""),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, name = "Match call next enabled")
@Composable
private fun MatchingCallInCallEnabledPreview() {
    DemoTheme {
        MatchingCallInCallContent(
            state = CallUiState(
                phase = CallRingingPhase.InCall,
                isMatchCall = true,
                peerNickname = "Isabella",
                peerAge = 23,
                callDurationSec = 6,
                matchTimeSeconds = 60,
                nextTimeSeconds = 6,
                showGiftQuickBar = true,
                gifts = listOf(
                    CallGiftUi(1, "A", 50, ""),
                    CallGiftUi(2, "B", 50, ""),
                    CallGiftUi(3, "C", 50, ""),
                ),
            ),
            onIntent = {},
        )
    }
}
