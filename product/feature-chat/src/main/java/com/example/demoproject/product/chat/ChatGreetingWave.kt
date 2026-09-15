package com.example.demoproject.product.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize

private const val GreetingWaveDegrees = 14f
private const val GreetingWaveMillis = 450

/**
 * First-visit waving-hand greeting (Figma 179:5296). Tap sends a fixed hi text, then fades out.
 */
@Composable
fun ChatGreetingWave(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cd = stringResource(R.string.chat_detail_cd_greeting)
    val transition = rememberInfiniteTransition(label = "chatGreetingWave")
    val angle by transition.animateFloat(
        initialValue = -GreetingWaveDegrees,
        targetValue = GreetingWaveDegrees,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = GreetingWaveMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "chatGreetingWaveAngle",
    )
    Image(
        painter = painterResource(R.drawable.chat_ill_greeting_wave),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(
                width = ComponentSize.chatGreetingWaveWidth,
                height = ComponentSize.chatGreetingWaveHeight,
            )
            .graphicsLayer {
                rotationZ = angle
                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.85f)
            }
            .semantics { contentDescription = cd }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick,
            ),
    )
}

@Preview(name = "GreetingWave", showBackground = true)
@Composable
private fun ChatGreetingWavePreview() {
    DemoTheme {
        ChatGreetingWave(enabled = true, onClick = {})
    }
}
