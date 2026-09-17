package com.example.demoproject.ui.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.zIndex
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

private val WelfareTightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private fun welfareTextStyle(
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontWeight: FontWeight,
    color: Color,
): TextStyle = TextStyle(
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    lineHeight = lineHeight,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = WelfareTightLineHeight,
)

/**
 * Free video-chat welfare popup — Figma 1:5469 / 1:5509.
 *
 * [count] is the remaining free video-chat quota shown as `Video Chat × N`.
 */
@Composable
fun FreeVideoWelfareDialog(
    count: Int,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnScrim: Boolean = true,
) {
    WelfareDialogScrim(
        onDismiss = onDismiss,
        dismissOnScrim = dismissOnScrim,
        modifier = modifier,
    ) {
        FreeVideoWelfareCard(
            count = count,
            onStart = onStart,
            onDismiss = onDismiss,
        )
    }
}

/**
 * Free video-match welfare popup — Figma 1:5521 / 1:5561.
 *
 * [count] is the remaining free match quota shown as `Video Match × N`.
 */
@Composable
fun FreeMatchWelfareDialog(
    count: Int,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnScrim: Boolean = true,
) {
    WelfareDialogScrim(
        onDismiss = onDismiss,
        dismissOnScrim = dismissOnScrim,
        modifier = modifier,
    ) {
        FreeMatchWelfareCard(
            count = count,
            onStart = onStart,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun WelfareDialogScrim(
    onDismiss: () -> Unit,
    dismissOnScrim: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(20f)
            .background(DemoColors.scrim)
            .pointerInput(dismissOnScrim) {
                detectTapGestures(
                    onTap = {
                        if (dismissOnScrim) onDismiss()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = ComponentSize.welfareDialogWidth)
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun FreeVideoWelfareCard(
    count: Int,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeCd = stringResource(R.string.welfare_cd_close)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ComponentSize.welfareFreeVideoAspect),
    ) {
        Image(
            painter = painterResource(R.drawable.welfare_free_video_hero),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ComponentSize.welfareFreeVideoHeroAspect)
                .align(Alignment.TopCenter)
                .clip(
                    RoundedCornerShape(
                        topStart = Radius.lg,
                        topEnd = Radius.lg,
                    ),
                ),
            contentScale = ContentScale.Crop,
        )
        Image(
            painter = painterResource(R.drawable.welfare_free_video_footer),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ComponentSize.welfareFreeVideoFooterAspect)
                .align(Alignment.BottomCenter)
                .clip(
                    RoundedCornerShape(
                        bottomStart = Radius.lg,
                        bottomEnd = Radius.lg,
                    ),
                ),
            contentScale = ContentScale.Crop,
        )
        // Title / reward sit above the CTA; CTA itself is pinned to the footer art.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = ComponentSize.welfareFreeVideoTextBottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WelfareFreeTitle(color = DemoColors.welfareFreeTitle)
            Spacer(modifier = Modifier.height(ComponentSize.welfareFreeVideoTitleToReward))
            WelfareRewardLine(
                label = stringResource(R.string.welfare_free_video_reward),
                count = count,
                labelColor = DemoColors.welfareFreeVideoReward,
                timesColor = DemoColors.welfareFreeVideoReward,
                countColor = DemoColors.welfareFreeVideoReward,
                labelToTimesGap = ComponentSize.welfareFreeVideoRewardLabelToTimes,
            )
        }
        // Soft-edged underlay hides baked-in button art without a hard band.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = ComponentSize.welfareFreeVideoCtaCoverHorizontal,
                    end = ComponentSize.welfareFreeVideoCtaCoverHorizontal,
                    bottom = ComponentSize.welfareFreeVideoCtaCoverBottom,
                )
                .height(ComponentSize.welfareFreeVideoCtaCoverHeight)
                .background(DemoGradients.welfareFreeVideoCtaCover),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = ComponentSize.welfareFreeVideoCtaHorizontal,
                    end = ComponentSize.welfareFreeVideoCtaHorizontal,
                    bottom = ComponentSize.welfareFreeVideoCtaBottom,
                )
                .height(ComponentSize.welfareFreeVideoCtaHeight)
                .clip(RoundedCornerShape(Radius.pill))
                .background(DemoGradients.welfareFreeVideoCta)
                .clickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color.White),
                    onClick = onStart,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.welfare_free_video_cta),
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.welfareFreeVideoCta,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                autoSize = DemoTextAutoSize.label(TextSize.welfareFreeVideoCta),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm),
            )
        }
        Image(
            painter = painterResource(R.drawable.welfare_ic_close),
            contentDescription = closeCd,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(ComponentSize.welfareFreeVideoCloseInset)
                .size(ComponentSize.welfareClose)
                .clip(RoundedCornerShape(Radius.md))
                .clickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onDismiss,
                )
                .semantics { contentDescription = closeCd },
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun FreeMatchWelfareCard(
    count: Int,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeCd = stringResource(R.string.welfare_cd_close)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ComponentSize.welfareFreeMatchAspect),
    ) {
        Image(
            painter = painterResource(R.drawable.welfare_free_match_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            // FillBounds keeps the baked-in CTA art aligned with our overlay hit target.
            contentScale = ContentScale.FillBounds,
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = ComponentSize.welfareFreeMatchTextBottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WelfareFreeTitle(color = DemoColors.welfareFreeTitle)
            Spacer(modifier = Modifier.height(ComponentSize.welfareFreeMatchTitleToReward))
            WelfareRewardLine(
                label = stringResource(R.string.welfare_free_match_reward),
                count = count,
                labelColor = DemoColors.welfareFreeMatchReward,
                timesColor = DemoColors.welfareFreeMatchTimes,
                countColor = DemoColors.welfareFreeMatchCount,
                labelToTimesGap = ComponentSize.welfareFreeMatchRewardLabelToTimes,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = ComponentSize.welfareFreeMatchCtaHorizontal,
                    end = ComponentSize.welfareFreeMatchCtaHorizontal,
                    bottom = ComponentSize.welfareFreeMatchCtaBottom,
                )
                .height(ComponentSize.welfareFreeMatchCtaHeight)
                .clickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color.White),
                    onClick = onStart,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.welfare_free_match_btn),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            Text(
                text = stringResource(R.string.welfare_free_match_cta),
                color = DemoColors.onPrimaryButton,
                fontSize = TextSize.welfareFreeMatchCta,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                autoSize = DemoTextAutoSize.label(TextSize.welfareFreeMatchCta),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm),
            )
        }
        // Close glyph is baked into the hero; this is the hit target only.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(ComponentSize.welfareFreeMatchCloseInset)
                .size(ComponentSize.welfareClose)
                .clickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onDismiss,
                )
                .semantics { contentDescription = closeCd },
        )
    }
}

@Composable
private fun WelfareFreeTitle(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.welfare_free_title)
    val strokeWidthPx = with(LocalDensity.current) {
        ComponentSize.welfareFreeTitleStroke.toPx()
    }
    val fillStyle = welfareTextStyle(
        fontSize = TextSize.welfareFreeTitle,
        lineHeight = TextSize.welfareFreeTitleLine,
        fontWeight = FontWeight.Black,
        color = color,
    )
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = fillStyle.copy(
                color = DemoColors.onPrimaryButton,
                drawStyle = Stroke(
                    width = strokeWidthPx,
                    join = StrokeJoin.Round,
                ),
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = title,
            style = fillStyle,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WelfareRewardLine(
    label: String,
    count: Int,
    labelColor: Color,
    timesColor: Color,
    countColor: Color,
    labelToTimesGap: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = welfareTextStyle(
                fontSize = TextSize.welfareFreeReward,
                lineHeight = TextSize.welfareFreeRewardLine,
                fontWeight = FontWeight.Black,
                color = labelColor,
            ),
            modifier = Modifier.alignByBaseline(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = DemoTextAutoSize.label(TextSize.welfareFreeReward),
        )
        Text(
            text = stringResource(R.string.welfare_free_times_prefix),
            style = welfareTextStyle(
                fontSize = TextSize.welfareFreeReward,
                lineHeight = TextSize.welfareFreeRewardLine,
                fontWeight = FontWeight.Black,
                color = timesColor,
            ),
            modifier = Modifier
                .alignByBaseline()
                .padding(start = labelToTimesGap),
            maxLines = 1,
        )
        Text(
            text = count.coerceAtLeast(0).toString(),
            style = welfareTextStyle(
                fontSize = TextSize.welfareFreeCount,
                lineHeight = TextSize.welfareFreeCountLine,
                fontWeight = FontWeight.Black,
                color = countColor,
            ),
            modifier = Modifier
                .alignByBaseline()
                .padding(start = ComponentSize.welfareFreeRewardTimesToCount),
            maxLines = 1,
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun FreeVideoWelfareDialogPreview() {
    DemoTheme {
        FreeVideoWelfareDialog(
            count = 2,
            onStart = {},
            onDismiss = {},
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun FreeMatchWelfareDialogPreview() {
    DemoTheme {
        FreeMatchWelfareDialog(
            count = 3,
            onStart = {},
            onDismiss = {},
        )
    }
}
