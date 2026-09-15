package com.example.demoproject.ui.designsystem.gift

import android.graphics.Color as AndroidColor
import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.opensource.svgaplayer.SVGACallback
import com.opensource.svgaplayer.SVGAImageView
import com.opensource.svgaplayer.SVGAVideoEntity
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ScrimFadeMs = 200

/** Gifts warmed by [GiftSvgaPreloader] start instantly; only a slower decode shows a loader. */
private const val LoaderDelayMs = 220L

/**
 * Full-screen gift SVGA overlay with scrim fade in/out.
 * Uses [GiftSvgaPreloader] so a warm gift plays without a network wait.
 * Tap anywhere to skip (with fade-out); [closeButton] can add an explicit skip affordance.
 */
@Composable
fun GiftSvgaOverlay(
    svgaUrl: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    closeButton: @Composable BoxScope.(onClose: () -> Unit) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrimAlpha = remember { Animatable(0f) }
    val targetScrim = DemoColors.scrim.alpha
    val finished = remember(svgaUrl) { AtomicBoolean(false) }
    val started = remember(svgaUrl) { AtomicBoolean(false) }
    val playerRef = remember(svgaUrl) { AtomicReference<SVGAImageView?>(null) }
    val latestOnFinished = rememberUpdatedState(onFinished)
    var videoItem by remember(svgaUrl) { mutableStateOf(GiftSvgaPreloader.cached(svgaUrl)) }
    var showLoader by remember(svgaUrl) { mutableStateOf(false) }

    val fadeOutAndFinish = remember(svgaUrl, scope, scrimAlpha) {
        {
            dismissGiftOverlay(
                finished = finished,
                playerRef = playerRef,
                scope = scope,
                scrimAlpha = scrimAlpha,
                onFinished = { latestOnFinished.value() },
            )
        }
    }

    LaunchedEffect(svgaUrl) {
        finished.set(false)
        scrimAlpha.snapTo(0f)
        launch { scrimAlpha.animateTo(targetScrim, animationSpec = tween(ScrimFadeMs)) }
        if (videoItem != null) return@LaunchedEffect
        val loaderJob = launch {
            delay(LoaderDelayMs)
            showLoader = true
        }
        val loaded = GiftSvgaPreloader.load(context, svgaUrl)
        loaderJob.cancel()
        showLoader = false
        if (loaded == null) fadeOutAndFinish() else videoItem = loaded
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(10f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = fadeOutAndFinish,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha.value)),
            )
            AndroidView(
                factory = { viewContext ->
                    SVGAImageView(viewContext).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        loops = 1
                        clearsAfterStop = true
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        callback = object : SVGACallback {
                            override fun onPause() = Unit
                            override fun onFinished() = fadeOutAndFinish()
                            override fun onRepeat() = Unit
                            override fun onStep(frame: Int, percentage: Double) = Unit
                        }
                        playerRef.set(this)
                    }
                },
                update = { view -> view.play(videoItem, started, finished) },
                onRelease = { view ->
                    view.stopAnimation(true)
                    view.clear()
                    view.callback = null
                    playerRef.compareAndSet(view, null)
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (showLoader) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(IconSize.md),
                        color = DemoColors.onPrimaryButton,
                        strokeWidth = ComponentSize.profileProgressStroke,
                    )
                }
            }
        }

        closeButton(fadeOutAndFinish)
    }
}

private fun SVGAImageView.play(
    videoItem: SVGAVideoEntity?,
    started: AtomicBoolean,
    finished: AtomicBoolean,
) {
    val item = videoItem ?: return
    if (finished.get() || !started.compareAndSet(false, true)) return
    setVideoItem(item)
    startAnimation()
}

private fun dismissGiftOverlay(
    finished: AtomicBoolean,
    playerRef: AtomicReference<SVGAImageView?>,
    scope: CoroutineScope,
    scrimAlpha: Animatable<Float, *>,
    onFinished: () -> Unit,
) {
    if (!finished.compareAndSet(false, true)) return
    scope.launch {
        playerRef.get()?.stopAnimation(true)
        scrimAlpha.animateTo(0f, animationSpec = tween(ScrimFadeMs))
        onFinished()
    }
}
