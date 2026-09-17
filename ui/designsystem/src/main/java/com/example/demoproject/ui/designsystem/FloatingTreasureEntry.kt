package com.example.demoproject.ui.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize
import java.util.Locale
import kotlin.math.max

/**
 * Floating Online treasure entry — Figma 37:4005 / asset 46:3031.
 * Unpaid: countdown overlay. Paid: SPECIAL OFFER label (no timer style).
 * Label sits in the capsule band (Figma 37:4004).
 */
@Composable
fun FloatingTreasureEntry(
    remainSeconds: Long,
    showSpecialOffer: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cd = stringResource(R.string.treasure_entry_cd)
    Box(
        modifier = modifier
            .size(
                width = ComponentSize.onlineTreasureEntryWidth,
                height = ComponentSize.onlineTreasureEntryHeight,
            )
            .semantics { contentDescription = cd }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.treasure_ic_entry),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            // Fill so capsule band maps 1:1 to Figma 37:4004 coordinates.
            contentScale = ContentScale.FillBounds,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = ComponentSize.onlineTreasureCountdownTop)
                .fillMaxWidth()
                .height(ComponentSize.onlineTreasureCountdownHeight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (showSpecialOffer) {
                    stringResource(R.string.treasure_entry_special_offer)
                } else {
                    formatTreasureCountdown(remainSeconds)
                },
                style = TreasureEntryLabelStyle,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = DemoTextAutoSize.label(TextSize.onlineTreasureCountdown),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xxs),
            )
        }
    }
}

/** Figma 37:4004 — SF Pro Bold 7.35 / line box 9, white, no font padding. */
private val TreasureEntryLabelStyle = TextStyle(
    color = DemoColors.onPrimaryButton,
    fontSize = TextSize.onlineTreasureCountdown,
    lineHeight = TextSize.onlineTreasureCountdownLine,
    fontWeight = FontWeight.Bold,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

fun formatTreasureCountdown(remainSeconds: Long): String {
    val total = max(0L, remainSeconds)
    val hours = total / 3_600L
    val minutes = (total % 3_600L) / 60L
    val seconds = total % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

@Preview(name = "Treasure entry countdown", showBackground = true, backgroundColor = 0xFF1A1028)
@Composable
private fun FloatingTreasureEntryCountdownPreview() {
    FloatingTreasureEntry(
        remainSeconds = 3_599L,
        showSpecialOffer = false,
        onClick = {},
    )
}

@Preview(name = "Treasure entry special offer", showBackground = true, backgroundColor = 0xFF1A1028)
@Composable
private fun FloatingTreasureEntrySpecialOfferPreview() {
    FloatingTreasureEntry(
        remainSeconds = 0L,
        showSpecialOffer = true,
        onClick = {},
    )
}
