package com.example.demoproject.ui.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

/** Figma primary CTA: 0px 6px 10px rgba(10,29,240,0.32). */
private val PrimaryButtonShadow = Shadow(
    radius = 10.dp,
    offset = DpOffset(x = 0.dp, y = 6.dp),
    color = DemoColors.primaryShadow,
)

/** Figma outlined CTA: 0px 2px 7.55px rgba(10,29,240,0.12). */
private val SecondaryButtonShadow = Shadow(
    radius = 7.55.dp,
    offset = DpOffset(x = 0.dp, y = 2.dp),
    color = DemoColors.secondaryShadow,
)

private val PillProgressStroke = 2.dp

@Composable
fun DemoGradientPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: Painter? = null,
    height: Dp = ComponentSize.pillButton,
) {
    val shape = RoundedCornerShape(Radius.pill)
    val clickEnabled = enabled && !isLoading
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .dropShadow(shape = shape, shadow = PrimaryButtonShadow)
            .clip(shape)
            .background(DemoGradients.primaryButton)
            .clickable(
                enabled = clickEnabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(IconSize.sm),
                color = DemoColors.onPrimaryButton,
                strokeWidth = PillProgressStroke,
            )
        } else {
            PillContent(
                text = text,
                textColor = DemoColors.onPrimaryButton,
                icon = icon,
                enabled = enabled,
            )
        }
    }
}

@Composable
fun DemoOutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: Painter? = null,
    height: Dp = ComponentSize.pillButton,
    containerColor: Color = DemoColors.sheet,
    contentColor: Color = DemoColors.textPrimary,
) {
    val shape = RoundedCornerShape(Radius.pill)
    val clickEnabled = enabled && !isLoading
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .dropShadow(shape = shape, shadow = SecondaryButtonShadow)
            .clip(shape)
            .background(containerColor)
            .clickable(
                enabled = clickEnabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = DemoColors.link),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(IconSize.sm),
                color = contentColor,
                strokeWidth = PillProgressStroke,
            )
        } else {
            PillContent(
                text = text,
                textColor = contentColor,
                icon = icon,
                enabled = enabled,
            )
        }
    }
}

@Composable
fun DemoPrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    DemoGradientPillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading,
        height = ComponentSize.pillButtonCompact,
    )
}

@Composable
private fun PillContent(
    text: String,
    textColor: Color,
    icon: Painter?,
    enabled: Boolean,
) {
    val alpha = if (enabled) 1f else 0.5f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(IconSize.sm),
                alpha = alpha,
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
        }
        Text(
            text = text,
            color = textColor.copy(alpha = alpha),
            fontSize = TextSize.md,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
