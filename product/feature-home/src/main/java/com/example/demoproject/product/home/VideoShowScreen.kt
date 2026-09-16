package com.example.demoproject.product.home

import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.demoproject.platform.data.model.OnlinePresence
import com.example.demoproject.product.profile.ProfileGiftUi
import com.example.demoproject.product.profile.gift.ProfileGiftSheet
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.designsystem.gift.GiftSvgaOverlay
import com.example.demoproject.ui.designsystem.media.MediaViewerVideoCache
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun VideoShowOverlay(
    user: OnlineUserUi,
    coinBalance: Int,
    gifts: List<ProfileGiftUi>,
    selectedGiftId: Long?,
    isGiftSheetVisible: Boolean,
    isGiftCatalogLoading: Boolean,
    isGiftSending: Boolean,
    giftAnimationUrl: String?,
    onIntent: (HomeIntent) -> Unit,
) {
    val show = user.videoShow ?: return
    Dialog(
        onDismissRequest = { onIntent(HomeIntent.CloseVideoShow) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BackHandler { onIntent(HomeIntent.CloseVideoShow) }
        VideoShowContent(
            user = user,
            videoShow = show,
            onClose = { onIntent(HomeIntent.OpenVideoShowProfile) },
            onOpenProfile = { onIntent(HomeIntent.OpenVideoShowProfile) },
            onReport = { onIntent(HomeIntent.ReportUser(user.id)) },
            onGift = { onIntent(HomeIntent.OpenVideoShowGift) },
            onVideoChat = { onIntent(HomeIntent.StartVideoShowCall) },
            onNext = { onIntent(HomeIntent.NextVideoShow) },
        )
    }
    if (isGiftSheetVisible) {
        ProfileGiftSheet(
            nickname = user.nickname,
            avatarUrl = user.avatarUrl,
            coinBalance = coinBalance,
            gifts = gifts,
            selectedGiftId = selectedGiftId,
            isCatalogLoading = isGiftCatalogLoading,
            isSending = isGiftSending,
            onDismiss = { onIntent(HomeIntent.DismissVideoShowGiftSheet) },
            onSelectGift = { onIntent(HomeIntent.SelectVideoShowGift(it)) },
            onSend = { onIntent(HomeIntent.SendVideoShowGift) },
            onOpenCoins = { onIntent(HomeIntent.OpenCoins) },
        )
    }
    giftAnimationUrl?.let { url ->
        GiftSvgaOverlay(
            svgaUrl = url,
            onFinished = { onIntent(HomeIntent.DismissVideoShowGiftAnimation) },
        )
    }
}

@Composable
internal fun VideoShowContent(
    user: OnlineUserUi,
    videoShow: VideoShowUi,
    onClose: () -> Unit,
    onOpenProfile: () -> Unit,
    onReport: () -> Unit,
    onGift: () -> Unit,
    onVideoChat: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember(videoShow.mediaId, videoShow.videoUrl) { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.profileVideoScrim),
    ) {
        VideoShowPlayer(
            videoShow = videoShow,
            onProgress = { progress = it },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.videoShowTopGradient)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DemoColors.profileVideoScrim,
                            DemoColors.profileVideoScrim.copy(alpha = 0f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.videoShowBottomGradient)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DemoColors.profileVideoScrim.copy(alpha = 0f),
                            DemoColors.profileVideoScrim,
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
            VideoShowProgressBar(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.chipGap)
                    .padding(top = Spacing.xs),
            )
            VideoShowTopBar(
                user = user,
                onClose = onClose,
                onOpenProfile = onOpenProfile,
                onReport = onReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.chipGap)
                    .padding(top = Spacing.sm),
            )
            Spacer(modifier = Modifier.weight(1f))
            VideoShowBottomChrome(
                showFreeBadge = user.showFreeBadge,
                onGift = onGift,
                onVideoChat = onVideoChat,
                onNext = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md)
                    .padding(bottom = Spacing.sm),
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoShowPlayer(
    videoShow: VideoShowUi,
    onProgress: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showCover by remember(videoShow.mediaId, videoShow.videoUrl) { mutableStateOf(true) }
    var isLoading by remember(videoShow.mediaId, videoShow.videoUrl) { mutableStateOf(true) }
    val errorToasted = remember(videoShow.mediaId, videoShow.videoUrl) { AtomicBoolean(false) }

    fun toastPlaybackError() {
        if (!errorToasted.compareAndSet(false, true)) return
        Toast.makeText(
            context,
            context.getString(R.string.home_video_show_play_failed),
            Toast.LENGTH_SHORT,
        ).show()
    }

    val player = remember(videoShow.mediaId, videoShow.videoUrl) {
        val cacheFactory = MediaViewerVideoCache.createCacheDataSourceFactory(context)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .build()
            .apply {
                if (videoShow.videoUrl.isNotBlank()) {
                    setMediaItem(MediaItem.fromUri(videoShow.videoUrl))
                    prepare()
                }
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isLoading = playbackState == Player.STATE_BUFFERING ||
                    playbackState == Player.STATE_IDLE ||
                    (playbackState == Player.STATE_READY && player.isLoading)
            }

            override fun onIsLoadingChanged(loading: Boolean) {
                if (player.playbackState == Player.STATE_READY) {
                    isLoading = loading
                }
            }

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
                toastPlaybackError()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            val duration = player.duration
            onProgress(
                if (duration > 0L) {
                    (player.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                },
            )
            delay(200)
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
            update = {
                it.player = player
                it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (showCover && !videoShow.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(videoShow.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (isLoading) {
            val loadingCd = stringResource(R.string.home_video_show_cd_loading)
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(IconSize.lg)
                    .semantics { contentDescription = loadingCd },
                color = DemoColors.onPrimaryButton,
            )
        }
    }
}

@Composable
private fun VideoShowProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(ComponentSize.videoShowProgress)
            .clip(RoundedCornerShape(Radius.pill))
            .background(DemoColors.videoShowProgressTrack),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(ComponentSize.videoShowProgress)
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoColors.onPrimaryButton),
        )
    }
}

@Composable
private fun VideoShowTopBar(
    user: OnlineUserUi,
    onClose: () -> Unit,
    onOpenProfile: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeCd = stringResource(R.string.home_video_show_cd_close)
    val reportCd = stringResource(R.string.home_online_cd_report)
    val profileCd = stringResource(R.string.home_video_show_cd_profile)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.video_show_ic_close),
            contentDescription = closeCd,
            modifier = Modifier
                .size(ComponentSize.videoShowClose)
                .clickable(
                    role = Role.Button,
                    onClick = onClose,
                ),
        )
        BoxWithConstraints(
            modifier = Modifier
                .padding(start = Spacing.chipGap)
                .weight(1f)
                .padding(end = Spacing.videoShowUserToReport),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(DemoColors.videoShowUserChip)
                    .clickable(
                        role = Role.Button,
                        onClick = onOpenProfile,
                    )
                    .padding(
                        start = Spacing.xxs,
                        top = Spacing.xxs,
                        end = Spacing.sm,
                        bottom = 3.dp,
                    )
                    .semantics { contentDescription = profileCd },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(ComponentSize.videoShowAvatar)
                        .clip(CircleShape)
                        .background(DemoColors.skeleton),
                )
                Text(
                    text = user.displayNameAge,
                    color = DemoColors.onPrimaryButton,
                    fontSize = TextSize.md,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = Spacing.sm)
                        .weight(1f, fill = false),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(ComponentSize.videoShowReport)
                .clip(CircleShape)
                .background(DemoColors.profileNavScrim)
                .clickable(role = Role.Button, onClick = onReport),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.video_show_ic_report),
                contentDescription = reportCd,
                modifier = Modifier.size(ComponentSize.videoShowReportIcon),
            )
        }
    }
}

@Composable
private fun VideoShowBottomChrome(
    showFreeBadge: Boolean,
    onGift: () -> Unit,
    onVideoChat: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val giftCd = stringResource(R.string.home_video_show_cd_gift)
    val nextCd = stringResource(R.string.home_video_show_cd_next)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.video_show_ic_gift),
                contentDescription = giftCd,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(ComponentSize.videoShowGift)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onGift,
                    ),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.videoShowCta)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(DemoGradients.videoShowCta)
                    .clickable(role = Role.Button, onClick = onVideoChat),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_video_show_video_chat),
                    color = DemoColors.onPrimaryButton,
                    fontSize = TextSize.md,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showFreeBadge) {
                Text(
                    text = stringResource(R.string.home_online_free),
                    color = DemoColors.onPrimaryButton,
                    fontSize = TextSize.xs,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = Spacing.sm)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(DemoColors.freeBadge)
                        .padding(horizontal = Spacing.chipGap, vertical = Spacing.xs),
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.home_video_show_next),
            color = DemoColors.onPrimaryButton,
            fontSize = TextSize.md,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    onClick = onNext,
                )
                .semantics { contentDescription = nextCd }
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun VideoShowContentPreview() {
    DemoTheme {
        VideoShowContent(
            user = OnlineUserUi(
                id = "1",
                externalUserId = "demo",
                nickname = "Giana",
                age = 24,
                avatarUrl = null,
                presence = OnlinePresence.Online,
                showFreeBadge = true,
                videoShow = VideoShowUi(
                    mediaId = "1",
                    videoUrl = "",
                    coverUrl = null,
                ),
            ),
            videoShow = VideoShowUi(
                mediaId = "1",
                videoUrl = "",
                coverUrl = null,
            ),
            onClose = {},
            onOpenProfile = {},
            onReport = {},
            onGift = {},
            onVideoChat = {},
            onNext = {},
        )
    }
}
