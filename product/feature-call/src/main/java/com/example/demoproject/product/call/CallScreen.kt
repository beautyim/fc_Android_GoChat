package com.example.demoproject.product.call

import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.platform.rtc.api.RtcCallPermissions
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.designsystem.media.MediaViewerVideoCache
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CallScreen(
    viewModel: CallViewModel,
    onBack: () -> Unit = {},
    onOpenStore: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var pendingAnswerAfterPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val mediaOk = RtcCallPermissions.mediaGranted(context) ||
            (
                grants[android.Manifest.permission.RECORD_AUDIO] == true &&
                    grants[android.Manifest.permission.CAMERA] == true
                )
        if (pendingAnswerAfterPermission) {
            pendingAnswerAfterPermission = false
            if (mediaOk) {
                viewModel.onIntent(CallIntent.Answer)
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.call_permission_required),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    fun tryAnswer() {
        if (RtcCallPermissions.mediaGranted(context)) {
            viewModel.onIntent(CallIntent.Answer)
        } else {
            pendingAnswerAfterPermission = true
            permissionLauncher.launch(RtcCallPermissions.required())
        }
    }

    DisposableEffect(activity) {
        viewModel.bindActivity(activity)
        onDispose { viewModel.bindActivity(null) }
    }

    // Incoming / outgoing call screen: always request RTC permissions on enter.
    LaunchedEffect(Unit) {
        if (!RtcCallPermissions.mediaGranted(context)) {
            permissionLauncher.launch(RtcCallPermissions.required())
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CallEffect.Exit -> onBack()
                is CallEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                CallEffect.OpenStore -> onOpenStore()
            }
        }
    }

    BackHandler { viewModel.onIntent(CallIntent.Hangup) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.phase) {
            CallRingingPhase.InCall -> {
                CallInCallContent(
                    state = state,
                    onIntent = viewModel::onIntent,
                )
            }
            else -> {
                CallRingingContent(
                    state = state,
                    onHangup = { viewModel.onIntent(CallIntent.Hangup) },
                    onAnswer = { tryAnswer() },
                    onReport = { viewModel.onIntent(CallIntent.Report) },
                )
            }
        }

        if (state.isReportSheetVisible) {
            CallReportSheet(
                state = state.report,
                onDismiss = { viewModel.onIntent(CallIntent.DismissReport) },
                onToggleReason = { viewModel.onIntent(CallIntent.ToggleReportReason(it)) },
                onSubmit = { viewModel.onIntent(CallIntent.SubmitReport) },
            )
        }

        state.coinPayGuide?.let { guide ->
            com.example.demoproject.product.store.CoinPayGuideSheet(
                state = guide,
                onDismiss = { viewModel.onIntent(CallIntent.DismissCoinPayGuide) },
                onPurchaseCoin = { viewModel.onIntent(CallIntent.PurchaseCoinPayGuideCoin(it)) },
                onPurchaseSale = { viewModel.onIntent(CallIntent.PurchaseCoinPayGuideSale(it)) },
            )
        }
    }
}

@Composable
internal fun CallRingingContent(
    state: CallUiState,
    onHangup: () -> Unit,
    onAnswer: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusText = when (state.phase) {
        CallRingingPhase.Incoming -> stringResource(R.string.call_ringing_status_incoming)
        CallRingingPhase.Outgoing,
        CallRingingPhase.Preparing,
        -> stringResource(R.string.call_ringing_status_outgoing)
        CallRingingPhase.Connecting -> stringResource(R.string.call_ringing_status_connecting)
        CallRingingPhase.InCall,
        CallRingingPhase.Ended,
        -> stringResource(R.string.call_ringing_status_ended)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.profileVideoScrim),
    ) {
        CallRingingBackground(
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            CallRingingHeader(
                displayName = state.displayName.ifBlank { stringResource(R.string.call_ringing_unknown_peer) },
                statusText = statusText,
                avatarUrl = state.peerAvatarUrl,
                onReport = onReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Spacing.callRingingHeaderStart,
                        end = Spacing.callRingingReportEnd,
                        top = Spacing.sm,
                    ),
            )
            Spacer(modifier = Modifier.weight(1f))
            when (state.phase) {
                CallRingingPhase.Incoming -> {
                    // After local answer (RTC active) keep Incoming look but hangup-only —
                    // Answer is already consumed and peer may not have joined yet.
                    if (state.rtcSurfacesActive) {
                        CallRingingOutgoingActions(
                            onCancel = onHangup,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Spacing.callRingingBottomInset),
                        )
                    } else {
                        CallRingingIncomingActions(
                            onHangup = onHangup,
                            onAnswer = onAnswer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Spacing.callRingingBottomInset),
                        )
                    }
                }
                CallRingingPhase.Outgoing,
                CallRingingPhase.Preparing,
                -> {
                    CallRingingOutgoingActions(
                        onCancel = onHangup,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.callRingingBottomInset),
                    )
                }
                CallRingingPhase.Connecting,
                CallRingingPhase.InCall,
                CallRingingPhase.Ended,
                -> Unit
            }
        }
    }
}

@Composable
private fun CallRingingHeader(
    displayName: String,
    statusText: String,
    avatarUrl: String,
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reportCd = stringResource(R.string.call_ringing_cd_report)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl.takeIf { it.isNotBlank() })
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(ComponentSize.callRingingAvatar)
                .clip(CircleShape)
                .background(DemoColors.callRingingReportBg),
        )
        Column(
            modifier = Modifier
                .padding(start = Spacing.callRingingAvatarGap)
                .weight(1f),
        ) {
            Text(
                text = displayName,
                color = DemoColors.callRingingOnVideo,
                fontSize = TextSize.callRingingName,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    shadow = Shadow(
                        color = DemoColors.callRingingTextShadow,
                        blurRadius = 2f,
                    ),
                ),
            )
            Text(
                text = statusText,
                color = DemoColors.callRingingStatus,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Spacing.callRingingStatusGap),
                style = TextStyle(
                    shadow = Shadow(
                        color = DemoColors.callRingingTextShadow,
                        blurRadius = 2f,
                    ),
                ),
            )
        }
        Box(
            modifier = Modifier
                .size(ComponentSize.callRingingReport)
                .clip(CircleShape)
                .background(DemoColors.callRingingReportBg)
                .semantics { contentDescription = reportCd }
                .clickable(
                    role = Role.Button,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onReport,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.call_ringing_ic_report),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.callRingingReportIcon),
            )
        }
    }
}

@Composable
private fun CallRingingOutgoingActions(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CallRingingActionButton(
            background = DemoColors.callRingingHangup,
            iconRes = R.drawable.call_ringing_ic_hangup,
            iconSize = ComponentSize.callRingingHangupIcon,
            buttonSize = ComponentSize.callRingingHangup,
            label = stringResource(R.string.call_ringing_cancel),
            contentDescription = stringResource(R.string.call_ringing_cd_cancel),
            onClick = onCancel,
        )
    }
}

@Composable
private fun CallRingingIncomingActions(
    onHangup: () -> Unit,
    onAnswer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = Spacing.xl),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        CallRingingActionButton(
            background = DemoColors.callRingingHangup,
            iconRes = R.drawable.call_ringing_ic_hangup,
            iconSize = ComponentSize.callRingingHangupIcon,
            buttonSize = ComponentSize.callRingingHangup,
            label = stringResource(R.string.call_ringing_hangup),
            contentDescription = stringResource(R.string.call_ringing_cd_hangup),
            onClick = onHangup,
        )
        CallRingingAnswerButton(onClick = onAnswer)
    }
}

@Composable
private fun CallRingingAnswerButton(onClick: () -> Unit) {
    val cd = stringResource(R.string.call_ringing_cd_answer)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Figma Group 81: outer 90 → mid 81 → solid 70, icon 36.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(ComponentSize.callRingingAnswerGlow),
        ) {
            Box(
                modifier = Modifier
                    .size(ComponentSize.callRingingAnswerGlow)
                    .clip(CircleShape)
                    .background(DemoColors.callRingingAnswerGlow),
            )
            Box(
                modifier = Modifier
                    .size(ComponentSize.callRingingAnswerGlowMid)
                    .clip(CircleShape)
                    .background(DemoColors.callRingingAnswerGlowMid),
            )
            Box(
                modifier = Modifier
                    .size(ComponentSize.callRingingAnswer)
                    .clip(CircleShape)
                    .background(DemoColors.callRingingAnswer)
                    .semantics { contentDescription = cd }
                    .clickable(
                        role = Role.Button,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.call_ringing_ic_answer),
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.callRingingAnswerIcon),
                )
            }
        }
        Text(
            text = stringResource(R.string.call_ringing_answer),
            color = DemoColors.callRingingOnVideo,
            fontSize = TextSize.sm,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(ComponentSize.callRingingHangup)
                .padding(top = Spacing.callRingingActionLabelGap),
        )
    }
}

@Composable
private fun CallRingingActionButton(
    background: Color,
    iconRes: Int,
    iconSize: Dp,
    buttonSize: Dp,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(background)
                .semantics { this.contentDescription = contentDescription }
                .clickable(
                    role = Role.Button,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        }
        Text(
            text = label,
            color = DemoColors.callRingingOnVideo,
            fontSize = TextSize.sm,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(buttonSize)
                .padding(top = Spacing.callRingingActionLabelGap),
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun CallRingingBackground(
    videoUrl: String,
    coverUrl: String,
    avatarUrl: String,
    modifier: Modifier = Modifier,
) {
    val fallbackUrl = coverUrl.ifBlank { avatarUrl }
    if (videoUrl.isBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(fallbackUrl.takeIf { it.isNotBlank() })
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.background(DemoColors.profileVideoScrim),
        )
        return
    }

    val context = LocalContext.current
    var showCover by remember(videoUrl) { mutableStateOf(true) }
    var isLoading by remember(videoUrl) { mutableStateOf(true) }
    val errorToasted = remember(videoUrl) { AtomicBoolean(false) }

    val player = remember(videoUrl) {
        val cacheFactory = MediaViewerVideoCache.createCacheDataSourceFactory(context)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(videoUrl))
                prepare()
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
                volume = 0f
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                showCover = false
                isLoading = false
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                if (playing) {
                    showCover = false
                    isLoading = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = false
                if (errorToasted.compareAndSet(false, true)) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.call_ringing_play_failed),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isLoading = playbackState == Player.STATE_BUFFERING ||
                    playbackState == Player.STATE_IDLE
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(modifier = modifier.background(DemoColors.profileVideoScrim)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )
        if (showCover || isLoading) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(fallbackUrl.takeIf { it.isNotBlank() })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (isLoading && showCover) {
            CircularProgressIndicator(
                color = DemoColors.callRingingOnVideo,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(showBackground = true, locale = "ar")
@Composable
private fun CallRingingIncomingPreview() {
    DemoTheme {
        CallRingingContent(
            state = CallUiState(
                phase = CallRingingPhase.Incoming,
                peerNickname = "Isabella",
                peerAge = 23,
                peerAvatarUrl = "",
            ),
            onHangup = {},
            onAnswer = {},
            onReport = {},
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Composable
private fun CallRingingOutgoingPreview() {
    DemoTheme {
        CallRingingContent(
            state = CallUiState(
                phase = CallRingingPhase.Outgoing,
                peerNickname = "Isabella",
                peerAge = 23,
            ),
            onHangup = {},
            onAnswer = {},
            onReport = {},
        )
    }
}
