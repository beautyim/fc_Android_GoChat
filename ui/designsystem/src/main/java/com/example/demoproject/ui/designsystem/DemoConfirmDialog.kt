package com.example.demoproject.ui.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

private val ConfirmBodyLineHeight = 18.sp
private val ConfirmProgressStroke = 2.dp

/**
 * Centered two-action confirm dialog.
 *
 * Default styling matches Figma node 80:4527 (lavender secondary + purple primary).
 * Pass [illustrationRes] + [positiveContainerColor] for destructive variants
 * such as Figma 80:3679 (delete conversation).
 */
@Composable
fun DemoConfirmDialog(
    title: String,
    negativeText: String,
    positiveText: String,
    onNegative: () -> Unit,
    onPositive: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    isPositiveLoading: Boolean = false,
    dismissOnScrim: Boolean = true,
    @DrawableRes illustrationRes: Int? = null,
    positiveContainerColor: Color = DemoColors.link,
    titleFontSize: TextUnit = TextSize.md,
    bodyFontSize: TextUnit = TextSize.sm,
    textWidth: Dp = ComponentSize.confirmDialogTextWidth,
    minHeight: Dp = ComponentSize.confirmDialogMinHeight,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(20f)
            .background(DemoColors.scrim)
            .pointerInput(dismissOnScrim, isPositiveLoading) {
                detectTapGestures(
                    onTap = {
                        if (dismissOnScrim && !isPositiveLoading) {
                            onNegative()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(ComponentSize.confirmDialogWidth)
                .heightIn(min = minHeight)
                .clip(RoundedCornerShape(Radius.dialog))
                .background(DemoColors.sheet)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(
                    horizontal = ComponentSize.confirmDialogHorizontalInset,
                    vertical = Spacing.md,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.width(ComponentSize.confirmDialogContentWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Column(
                    modifier = Modifier.width(textWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    if (illustrationRes != null) {
                        Box(
                            modifier = Modifier.size(ComponentSize.confirmDialogIllustration),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        color = DemoColors.dialogDestructiveGlow,
                                        shape = CircleShape,
                                    ),
                            )
                            Image(
                                painter = painterResource(illustrationRes),
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.xl),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                    Text(
                        text = title,
                        color = DemoColors.textPrimary,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!body.isNullOrBlank()) {
                        Text(
                            text = body,
                            color = DemoColors.textSecondary,
                            fontSize = bodyFontSize,
                            lineHeight = ConfirmBodyLineHeight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        ComponentSize.confirmDialogActionsGap,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ConfirmDialogAction(
                        text = negativeText,
                        onClick = onNegative,
                        containerColor = DemoColors.dialogSecondary,
                        contentColor = DemoColors.textPrimary,
                        enabled = !isPositiveLoading,
                    )
                    ConfirmDialogAction(
                        text = positiveText,
                        onClick = onPositive,
                        containerColor = positiveContainerColor,
                        contentColor = DemoColors.onPrimaryButton,
                        enabled = !isPositiveLoading,
                        isLoading = isPositiveLoading,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialogAction(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Box(
        modifier = modifier
            .width(ComponentSize.confirmDialogActionWidth)
            .height(ComponentSize.dialogAction)
            .clip(RoundedCornerShape(Radius.pill))
            .background(containerColor)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(IconSize.sm),
                color = contentColor,
                strokeWidth = ConfirmProgressStroke,
            )
        } else {
            Text(
                text = text,
                color = contentColor,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun DemoConfirmDialogPreview() {
    DemoTheme {
        DemoConfirmDialog(
            title = "Block Dulce?",
            body = "You won’t be able to send or receive messages from her after blocking.",
            negativeText = "Cancel",
            positiveText = "Block",
            onNegative = {},
            onPositive = {},
        )
    }
}
