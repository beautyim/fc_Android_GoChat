package com.example.demoproject.product.profile.gift

import android.graphics.Color as AndroidColor
import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.demoproject.product.profile.ProfileOverlayCloseButton
import com.example.demoproject.ui.designsystem.DemoColors
import com.opensource.svgaplayer.SVGACallback
import com.opensource.svgaplayer.SVGAImageView
import com.opensource.svgaplayer.SVGAParser
import com.opensource.svgaplayer.SVGAVideoEntity
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val ScrimFadeMs = 200

/**
 * Full-screen gift SVGA overlay with scrim fade in/out.
 * Uses [GiftSvgaPreloader] when warm so playback can start without a network wait.
 * Close button or tap anywhere to skip (with fade-out).
 */
@Composable
fun GiftSvgaOverlay(
    svgaUrl: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val scrimAlpha = remember { Animatable(0f) }
    val targetScrim = DemoColors.scrim.alpha
    val finished = remember(svgaUrl) { AtomicBoolean(false) }
    val playerRef = remember(svgaUrl) { AtomicReference<SVGAImageView?>(null) }
    val latestOnFinished = rememberUpdatedState(onFinished)

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
        scrimAlpha.animateTo(targetScrim, animationSpec = tween(ScrimFadeMs))
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

                        fun play(item: SVGAVideoEntity) {
                            if (finished.get()) return
                            setVideoItem(item)
                            startAnimation()
                        }

                        val cached = GiftSvgaPreloader.get(svgaUrl)
                        if (cached != null) {
                            play(cached)
                        } else {
                            val parser = SVGAParser(viewContext.applicationContext)
                            runCatching { URL(svgaUrl) }
                                .onSuccess { url ->
                                    parser.decodeFromURL(
                                        url,
                                        object : SVGAParser.ParseCompletion {
                                            override fun onComplete(videoItem: SVGAVideoEntity) {
                                                GiftSvgaPreloader.put(svgaUrl, videoItem)
                                                play(videoItem)
                                            }

                                            override fun onError() = fadeOutAndFinish()
                                        },
                                    )
                                }
                                .onFailure { fadeOutAndFinish() }
                        }
                    }
                },
                onRelease = { view ->
                    view.stopAnimation(true)
                    view.clear()
                    view.callback = null
                    playerRef.compareAndSet(view, null)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        ProfileOverlayCloseButton(
            onClick = fadeOutAndFinish,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
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
