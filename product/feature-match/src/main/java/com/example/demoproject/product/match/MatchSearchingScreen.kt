package com.example.demoproject.product.match

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-bleed "Matching…" state shown while `/match/start` is in flight
 * (Figma 1:1442). Per the design annotation (Figma 203:6836) the outer bands
 * ripple outwards and a bright head sweeps the core ring, painting a fading trail.
 */
@Composable
fun MatchSearchingOverlay(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onCancel)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.matchHeroFallback)
            // Swallow taps so the match screen underneath stays inert.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.match_searching_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DemoColors.matchSearchingScrim),
        )
        MatchSearchingRadar(
            modifier = Modifier
                .align(Alignment.Center)
                // widthIn has to cap the incoming max before fillMaxWidth locks
                // the width, otherwise a tablet stretches the emblem.
                .padding(horizontal = Spacing.matchSearchingFieldInset)
                .widthIn(max = ComponentSize.matchSearchingField)
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        Text(
            text = stringResource(R.string.match_searching_cancel),
            color = DemoColors.matchSearchingCancel,
            fontSize = TextSize.md,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = Spacing.matchSearchingCancelBottom - Spacing.sm)
                .clickable(onClick = onCancel)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        )
    }
}

@Composable
private fun MatchSearchingRadar(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = maxWidth
        // The radar is one emblem: its parts keep the Figma proportions no
        // matter how much room the phone gives the square field.
        val scale = side / ComponentSize.matchSearchingField
        val transition = rememberInfiniteTransition(label = "matchSearching")
        val waveProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = WaveDurationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "matchSearchingWave",
        )
        val spin by transition.animateFloat(
            initialValue = 0f,
            targetValue = FullTurnDegrees,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = SpinDurationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "matchSearchingSpin",
        )

        val waveFill = DemoColors.matchSearchingWaveFill
        val waveStroke = DemoColors.matchSearchingWaveStroke
        val coreStroke = DemoColors.matchSearchingCoreStroke
        val innerStroke = DemoColors.matchSearchingInnerStroke
        val coreOuter = DemoColors.matchSearchingCoreOuter
        val coreMiddle = DemoColors.matchSearchingCoreMiddle
        val coreInner = DemoColors.matchSearchingCoreInner
        val hairline = ComponentSize.matchSearchingRingStroke

        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val baseStroke = hairline.toPx() * scale
            repeat(WaveCount) { index ->
                val phase = (waveProgress + index.toFloat() / WaveCount) % 1f
                val ratio = CoreOuterRatio + (1f - CoreOuterRatio) * phase
                // Fade in off the core rim and out at the field edge so waves
                // never pop into or out of existence.
                val fade = sin(phase * PI).toFloat()
                drawBand(
                    radius = radius * ratio,
                    fill = waveFill.copy(alpha = waveFill.alpha * fade),
                    stroke = waveStroke.copy(alpha = waveStroke.alpha * fade),
                    strokeWidth = baseStroke * ratio,
                )
            }
            drawBand(
                radius = radius * CoreOuterRatio,
                fill = coreOuter,
                stroke = coreStroke,
                strokeWidth = baseStroke * CoreOuterRatio,
            )
            drawBand(
                radius = radius * CoreMiddleRatio,
                fill = coreMiddle,
                stroke = coreStroke,
                strokeWidth = baseStroke * CoreMiddleRatio,
            )
            drawBand(
                radius = radius * CoreInnerRatio,
                fill = coreInner,
                stroke = innerStroke,
                strokeWidth = baseStroke * CoreInnerRatio,
            )
        }

        MatchSearchingArc(
            scale = scale,
            headAngleDegrees = ArcAnchorDegrees + spin,
            modifier = Modifier.size(ComponentSize.matchSearchingArcRing * scale),
        )

        Image(
            painter = painterResource(R.drawable.match_searching_hearts),
            contentDescription = null,
            modifier = Modifier
                .offset(y = -Spacing.matchSearchingEmblemLift * scale)
                .size(ComponentSize.matchSearchingHearts * scale),
            contentScale = ContentScale.Fit,
        )

        Column(
            modifier = Modifier.offset(y = Spacing.matchSearchingCaptionDrop * scale),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.match_searching_title),
                color = DemoColors.matchSearchingTitle,
                fontSize = TextSize.matchSearchingTitle,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(Spacing.matchSearchingTitleToCaption * scale))
            Text(
                text = stringResource(R.string.match_searching_caption),
                color = DemoColors.matchSearchingCaption,
                fontSize = TextSize.xs,
                lineHeight = TextSize.matchSearchingCaptionLine,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }

        MatchSearchingSparks.forEach { (xRatio, yRatio) ->
            Image(
                painter = painterResource(R.drawable.match_searching_spark),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = side * xRatio, y = side * yRatio)
                    .size(ComponentSize.matchSearchingSpark * scale),
                contentScale = ContentScale.Fit,
            )
        }
        MatchSearchingWhiteHearts.forEach { (xRatio, yRatio) ->
            MatchSearchingHeart(
                iconRes = R.drawable.match_ic_heart_white,
                width = ComponentSize.matchSearchingHeartWhiteWidth * scale,
                height = ComponentSize.matchSearchingHeartWhiteHeight * scale,
                offsetX = side * xRatio,
                offsetY = side * yRatio,
            )
        }
        MatchSearchingGlowHearts.forEach { (xRatio, yRatio) ->
            MatchSearchingHeart(
                iconRes = R.drawable.match_searching_heart_glow,
                width = ComponentSize.matchSearchingHeartGlowWidth * scale,
                height = ComponentSize.matchSearchingHeartGlowHeight * scale,
                offsetX = side * xRatio,
                offsetY = side * yRatio,
            )
        }
    }
}

/**
 * Core-ring sweep: a bright head orbits and paints a trail that fades out until
 * the head sweeps that stretch again.
 *
 * Stroke is centred on the box edge (overflows by half its width) — same as the
 * Figma vector. Kept in its own square so the sweep gradient is ring-local.
 */
@Composable
private fun MatchSearchingArc(
    scale: Float,
    headAngleDegrees: Float,
    modifier: Modifier = Modifier,
) {
    val arcHead = DemoColors.matchSearchingArcStart
    val arcTail = DemoColors.matchSearchingArcEnd
    val arcStroke = ComponentSize.matchSearchingArcStroke
    val leadingDot = ComponentSize.matchSearchingArcDotLeading * scale
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val ringRadius = maxWidth / 2
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = arcStroke.toPx() * scale
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            val trailFraction = ArcTrailDegrees / FullTurnDegrees
            // Sweep gradient runs clockwise from 3 o'clock; rotate so 0 sits at
            // the faded tail and [trailFraction] lands on the moving head.
            rotate(
                degrees = headAngleDegrees - ArcTrailDegrees,
                pivot = center,
            ) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colorStops = arrayOf(
                            0f to arcTail.copy(alpha = 0f),
                            trailFraction * 0.45f to arcTail.copy(alpha = 0.35f),
                            trailFraction to arcHead,
                            (trailFraction + 0.002f).coerceAtMost(1f) to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                        center = center,
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.match_searching_arc_dot_leading),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = ringRadius * cos(headAngleDegrees * PI / 180.0).toFloat(),
                    y = ringRadius * sin(headAngleDegrees * PI / 180.0).toFloat(),
                )
                .size(leadingDot),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun MatchSearchingHeart(
    @DrawableRes iconRes: Int,
    width: Dp,
    height: Dp,
    offsetX: Dp,
    offsetY: Dp,
) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(width = width, height = height),
        contentScale = ContentScale.Fit,
    )
}

private fun DrawScope.drawBand(
    radius: Float,
    fill: Color,
    stroke: Color,
    strokeWidth: Float,
) {
    drawCircle(color = fill, radius = radius)
    drawCircle(color = stroke, radius = radius, style = Stroke(width = strokeWidth))
}

/** Figma band radii as a fraction of the field radius (Ellipse 24 / 25 / 26). */
private const val CoreOuterRatio = 0.7507f
private const val CoreMiddleRatio = 0.6406f
private const val CoreInnerRatio = 0.6f
private const val WaveCount = 2
private const val WaveDurationMillis = 2600
private const val SpinDurationMillis = 3600
private const val FullTurnDegrees = 360f

/** Compose angles start at 3 o'clock; the head starts at the top and sweeps clockwise. */
private const val ArcAnchorDegrees = -90f
/** How far behind the head the painted trail stays visible before it fades out. */
private const val ArcTrailDegrees = 270f

/** Sparkle centres as a fraction of the field side, from its centre (172:3635…3639). */
private val MatchSearchingSparks = listOf(
    -0.1928f to -0.1073f,
    0.1493f to 0.0232f,
    0.1377f to 0.0348f,
    0.1435f to -0.1391f,
    0.1435f to -0.1536f,
)

/** Scattered heart centres as a fraction of the field side (172:3640…3643). */
private val MatchSearchingWhiteHearts = listOf(
    -0.4638f to -0.0884f,
    0.2899f to 0.5174f,
)
private val MatchSearchingGlowHearts = listOf(
    -0.3014f to 0.4130f,
    0.3739f to -0.3580f,
)

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "Match searching", showBackground = true, widthDp = 375, heightDp = 812)
@Preview(name = "Match searching RTL", locale = "ar", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MatchSearchingOverlayPreview() {
    DemoTheme {
        MatchSearchingOverlay(onCancel = {})
    }
}
