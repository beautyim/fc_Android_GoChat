package com.example.demoproject.ui.designsystem.media

import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.demoproject.ui.designsystem.R
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToLong

@Composable
fun MediaViewer(
    items: List<MediaViewerItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    showVideoChat: Boolean = false,
    onVideoChat: () -> Unit = {},
) {
    if (items.isEmpty()) return
    val startIndex = initialIndex.coerceIn(0, items.lastIndex)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        MediaViewerContent(
            items = items,
            initialIndex = startIndex,
            onDismiss = onDismiss,
            showVideoChat = showVideoChat,
            onVideoChat = onVideoChat,
        )
    }
}

@Composable
internal fun MediaViewerContent(
    items: List<MediaViewerItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    showVideoChat: Boolean,
    onVideoChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, items.lastIndex),
        pageCount = { items.size },
    )
    var allowPagerScroll by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.profileVideoScrim),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = allowPagerScroll,
        ) { page ->
            val item = items[page]
            val pageActive = pagerState.settledPage == page
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (item.isVideo) {
                    MediaViewerVideoPage(
                        item = item,
                        pageActive = pageActive,
                        reserveBottomChrome = showVideoChat,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    MediaViewerImagePage(
                        item = item,
                        onZoomChanged = { zoomed ->
                            if (pageActive) allowPagerScroll = !zoomed
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        MediaViewerCloseButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopStart),
        )

        if (showVideoChat) {
            MediaViewerVideoChatButton(
                onClick = onVideoChat,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm + Spacing.xs)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MediaViewerVideoChatButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(ComponentSize.profileMediaCta)
            .clip(RoundedCornerShape(Radius.pill))
            .background(DemoGradients.primaryButton)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xxs),
        ) {
            Image(
                painter = painterResource(R.drawable.media_viewer_ic_video),
                contentDescription = null,
                modifier = Modifier.size(IconSize.sm),
            )
            Text(
                text = stringResource(R.string.media_viewer_start_video_chat),
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.md,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MediaViewerImagePage(
    item: MediaViewerItem,
    onZoomChanged: (zoomed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoomableState = rememberZoomableState()
    val imageState = rememberZoomableImageState(zoomableState)
    val transformation = zoomableState.contentTransformation
    LaunchedEffect(transformation) {
        // Use userZoom (gesture zoom), not total scale — ContentScale.Fit can set
        // initialScale > 1 for small images, which would falsely disable pager swipe.
        onZoomChanged(
            transformation.isSpecified && transformation.scaleMetadata.userZoom > 1.01f,
        )
    }
    ZoomableAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(item.imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = null,
        state = imageState,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun MediaViewerVideoPage(
    item: MediaViewerItem,
    pageActive: Boolean,
    reserveBottomChrome: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showCover by remember(item.id) { mutableStateOf(true) }
    var isLoading by remember(item.id) { mutableStateOf(true) }
    var isPlaying by remember(item.id) { mutableStateOf(false) }
    var positionMs by remember(item.id) { mutableLongStateOf(0L) }
    var durationMs by remember(item.id) { mutableLongStateOf(0L) }
    var scrubbing by remember(item.id) { mutableStateOf(false) }
    var scrubFraction by remember(item.id) { mutableFloatStateOf(0f) }
    val errorToasted = remember(item.id) { AtomicBoolean(false) }

    fun toastPlaybackError(messageRes: Int) {
        if (!errorToasted.compareAndSet(false, true)) return
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    val player = remember(item.id) {
        val cacheFactory = MediaViewerVideoCache.createCacheDataSourceFactory(context)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .build()
            .apply {
                val url = item.videoUrl
                if (!url.isNullOrBlank()) {
                    setMediaItem(MediaItem.fromUri(url))
                    prepare()
                }
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = false
            }
    }

    DisposableEffect(player) {
        fun syncLoading() {
            isLoading = when (player.playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_IDLE,
                -> true
                Player.STATE_READY,
                Player.STATE_ENDED,
                -> player.isLoading
                else -> player.isLoading
            }
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                syncLoading()
                if (playbackState == Player.STATE_READY) {
                    durationMs = player.duration.coerceAtLeast(0L)
                }
            }

            override fun onIsLoadingChanged(loading: Boolean) {
                syncLoading()
            }

            override fun onRenderedFirstFrame() {
                // Keep the cover until the first frame is on the surface so taps
                // during buffering do not flash the black PlayerView/scrim.
                showCover = false
                isLoading = false
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    showCover = false
                    isLoading = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = false
                isPlaying = false
                toastPlaybackError(videoPlaybackErrorMessageRes(error))
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (!scrubbing) {
                    positionMs = player.currentPosition.coerceAtLeast(0L)
                    val d = player.duration
                    if (d > 0) durationMs = d
                }
            }
        }
        player.addListener(listener)
        syncLoading()
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(pageActive, item.videoUrl) {
        if (!pageActive) {
            player.pause()
            player.seekTo(0)
            showCover = true
            isLoading = true
            positionMs = 0L
            isPlaying = false
            return@LaunchedEffect
        }
        if (item.videoUrl.isNullOrBlank()) {
            isLoading = false
            toastPlaybackError(R.string.media_viewer_video_play_failed)
            return@LaunchedEffect
        }
        errorToasted.set(false)
        isLoading = true
        player.playWhenReady = true
        player.play()
    }

    LaunchedEffect(player, pageActive, scrubbing) {
        while (isActive) {
            if (pageActive && !scrubbing) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                val d = player.duration
                if (d > 0) durationMs = d
            }
            delay(200)
        }
    }

    val displayFraction = when {
        scrubbing -> scrubFraction
        durationMs > 0L -> (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }
    val displayPositionMs = if (scrubbing && durationMs > 0L) {
        (scrubFraction * durationMs).roundToLong()
    } else {
        positionMs
    }
    val controlsBottomPadding = if (reserveBottomChrome) {
        ComponentSize.profileMediaCta + Spacing.sm + Spacing.xs
    } else {
        0.dp
    }

    Box(modifier = modifier.background(DemoColors.profileVideoScrim)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    this.player = player
                }
            },
            update = {
                it.player = player
                it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (showCover && !item.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (item.videoUrl.isNullOrBlank()) {
                            toastPlaybackError(R.string.media_viewer_video_play_failed)
                            return@clickable
                        }
                        // Do not clear the cover here — wait for first frame /
                        // isPlaying so the surface is not shown as black while buffering.
                        errorToasted.set(false)
                        if (player.playbackState == Player.STATE_IDLE) {
                            player.prepare()
                        }
                        player.playWhenReady = true
                        player.play()
                    },
            )
        }

        if (isLoading) {
            val loadingCd = stringResource(R.string.media_viewer_cd_loading)
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(IconSize.lg)
                    .semantics { contentDescription = loadingCd },
                color = DemoColors.onPrimaryButton,
                strokeWidth = ComponentSize.profileProgressStroke,
            )
        }

        MediaViewerVideoControls(
            isPlaying = isPlaying,
            positionMs = displayPositionMs,
            durationMs = durationMs,
            progress = displayFraction,
            onTogglePlay = {
                if (player.isPlaying) {
                    player.pause()
                } else if (item.videoUrl.isNullOrBlank()) {
                    toastPlaybackError(R.string.media_viewer_video_play_failed)
                } else {
                    // Keep cover until first frame; avoid re-prepare while buffering.
                    errorToasted.set(false)
                    if (player.playbackState == Player.STATE_IDLE) {
                        player.prepare()
                    }
                    player.playWhenReady = true
                    player.play()
                }
            },
            onScrubStart = {
                scrubbing = true
                scrubFraction = displayFraction
            },
            onScrub = { fraction ->
                scrubFraction = fraction.coerceIn(0f, 1f)
            },
            onScrubEnd = { fraction ->
                val target = if (durationMs > 0L) {
                    (fraction.coerceIn(0f, 1f) * durationMs).roundToLong()
                } else {
                    0L
                }
                player.seekTo(target)
                positionMs = target
                scrubbing = false
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.md - Spacing.xxs, vertical = Spacing.sm)
                .padding(bottom = controlsBottomPadding),
        )
    }
}

@StringRes
private fun videoPlaybackErrorMessageRes(error: PlaybackException): Int {
    return when (error.errorCode) {
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        -> R.string.media_viewer_video_unsupported
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        -> R.string.media_viewer_video_network_failed
        else -> R.string.media_viewer_video_play_failed
    }
}

@Composable
private fun MediaViewerVideoControls(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    progress: Float,
    onTogglePlay: () -> Unit,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xxs),
    ) {
        Image(
            painter = painterResource(
                if (isPlaying) R.drawable.media_viewer_ic_pause else R.drawable.media_viewer_ic_play,
            ),
            contentDescription = stringResource(
                if (isPlaying) R.string.media_viewer_cd_pause else R.string.media_viewer_cd_play,
            ),
            modifier = Modifier
                .width(ComponentSize.profileMediaPlayWidth)
                .height(ComponentSize.profileMediaPlayHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onTogglePlay,
                ),
        )
        Text(
            text = formatMediaTime(positionMs),
            color = DemoColors.profileMediaTime,
            fontSize = TextSize.sm,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        MediaViewerProgressBar(
            progress = progress,
            onScrubStart = onScrubStart,
            onScrub = onScrub,
            onScrubEnd = onScrubEnd,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatMediaTime(durationMs),
            color = DemoColors.profileMediaTime,
            fontSize = TextSize.sm,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MediaViewerProgressBar(
    progress: Float,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragFraction by remember { mutableFloatStateOf(progress) }
    LaunchedEffect(progress) {
        dragFraction = progress
    }
    BoxWithConstraints(
        modifier = modifier
            .height(ComponentSize.profileMediaProgressKnob)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onScrubStart()
                    onScrub(fraction)
                    onScrubEnd(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        onScrubStart()
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = fraction
                        onScrub(fraction)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = fraction
                        onScrub(fraction)
                    },
                    onDragEnd = {
                        onScrubEnd(dragFraction)
                    },
                    onDragCancel = {
                        onScrubEnd(dragFraction)
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackHeight = ComponentSize.profileMediaProgressTrack
        val knob = ComponentSize.profileMediaProgressKnob
        val trackWidth = maxWidth
        val filled = trackWidth * progress.coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(Radius.xxs))
                .background(DemoColors.profileMediaProgressTrack),
        )
        Box(
            modifier = Modifier
                .width(filled.coerceAtLeast(0.dp))
                .height(trackHeight)
                .clip(RoundedCornerShape(Radius.xxs))
                .background(DemoColors.onPrimaryButton),
        )
        Box(
            modifier = Modifier
                .padding(
                    start = (filled - knob / 2)
                        .coerceIn(0.dp, (trackWidth - knob).coerceAtLeast(0.dp)),
                )
                .size(knob)
                .clip(CircleShape)
                .background(DemoColors.onPrimaryButton),
        )
    }
}

private fun formatMediaTime(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun MediaViewerImagePreview() {
    DemoTheme {
        MediaViewerContent(
            items = listOf(
                MediaViewerItem(
                    id = 1,
                    isVideo = false,
                    imageUrl = "",
                    videoUrl = null,
                    coverUrl = null,
                ),
            ),
            initialIndex = 0,
            onDismiss = {},
            showVideoChat = true,
            onVideoChat = {},
        )
    }
}

@Preview
@Composable
private fun MediaViewerVideoControlsPreview() {
    DemoTheme {
        Column(
            modifier = Modifier
                .background(DemoColors.profileVideoScrim)
                .padding(Spacing.md),
        ) {
            MediaViewerVideoControls(
                isPlaying = true,
                positionMs = 20_000,
                durationMs = 30_000,
                progress = 20f / 30f,
                onTogglePlay = {},
                onScrubStart = {},
                onScrub = {},
                onScrubEnd = {},
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            MediaViewerVideoChatButton(onClick = {})
        }
    }
}
