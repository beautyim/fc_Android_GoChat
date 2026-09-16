package com.example.demoproject.ui.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.example.demoproject.ui.foundation.IconSize

/**
 * Nav-bar glyph button (back / close / more) whose press feedback is a circle.
 *
 * The touch target is clipped to [CircleShape] so the bounded ripple stays round
 * instead of taking the glyph's rectangular bounds.
 *
 * @param size circular touch target, defaults to the glyph size.
 * @param mirrorInRtl flips the glyph horizontally in RTL, for directional arrows.
 */
@Composable
fun DemoNavIconButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = IconSize.md,
    size: Dp = iconSize,
    containerColor: Color = Color.Transparent,
    mirrorInRtl: Boolean = false,
) {
    val mirror = mirrorInRtl && LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer { scaleX = if (mirror) -1f else 1f },
            contentScale = ContentScale.Fit,
        )
    }
}
