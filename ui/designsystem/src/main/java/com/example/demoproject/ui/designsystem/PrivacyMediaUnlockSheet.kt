package com.example.demoproject.ui.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

private val PrivacyUnlockSheetShape = RoundedCornerShape(
    topStart = Radius.privacyUnlockSheet,
    topEnd = Radius.privacyUnlockSheet,
)

private val PrivacyUnlockCtaShape = RoundedCornerShape(Radius.privacyUnlockCta)

private val PrivacyUnlockRemindCheckShape =
    RoundedCornerShape(Radius.privacyUnlockRemindCheck)

private val PrivacyUnlockCtaShadow = Shadow(
    radius = ComponentSize.privacyUnlockCtaShadowBlur,
    offset = DpOffset(x = 0.dp, y = ComponentSize.privacyUnlockCtaShadowY),
    color = DemoColors.privacyUnlockCtaShadow,
)

private val PrivacyUnlockTightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private fun privacyUnlockTextStyle(
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
    lineHeightStyle = PrivacyUnlockTightLineHeight,
)

/**
 * Privacy media unlock sheet — Figma 502:3650.
 *
 * Hero wash carries title / price / CTA / don't-remind; close uses a hit target
 * over the baked-in X on [R.drawable.privacy_unlock_hero].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyMediaUnlockSheet(
    price: Int,
    isVideo: Boolean,
    dontRemind: Boolean,
    onDontRemindChange: (Boolean) -> Unit,
    onUnlock: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isUnlocking: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = PrivacyUnlockSheetShape,
        containerColor = DemoColors.privacyUnlockSheet,
        scrimColor = DemoColors.scrim,
        dragHandle = null,
        tonalElevation = 0.dp,
    ) {
        PrivacyMediaUnlockSheetContent(
            price = price,
            isVideo = isVideo,
            dontRemind = dontRemind,
            onDontRemindChange = onDontRemindChange,
            onUnlock = onUnlock,
            onDismiss = onDismiss,
            isUnlocking = isUnlocking,
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
    }
}

@Composable
internal fun PrivacyMediaUnlockSheetContent(
    price: Int,
    isVideo: Boolean,
    dontRemind: Boolean,
    onDontRemindChange: (Boolean) -> Unit,
    onUnlock: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isUnlocking: Boolean = false,
) {
    val closeCd = stringResource(R.string.privacy_unlock_cd_close)
    val title = stringResource(
        if (isVideo) R.string.privacy_unlock_title_video
        else R.string.privacy_unlock_title_photo,
    )
    val pricePrefix = stringResource(R.string.privacy_unlock_price_prefix, price)
    val priceSuffix = stringResource(
        if (isVideo) R.string.privacy_unlock_price_suffix_video
        else R.string.privacy_unlock_price_suffix_photo,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DemoColors.privacyUnlockSheet),
    ) {
        Image(
            painter = painterResource(R.drawable.privacy_unlock_hero),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ComponentSize.privacyUnlockHeroAspect),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = ComponentSize.privacyUnlockCloseTop,
                    end = ComponentSize.privacyUnlockCloseEnd,
                )
                .size(ComponentSize.privacyUnlockClose)
                .clip(CircleShape)
                .clickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onDismiss,
                )
                .semantics { contentDescription = closeCd },
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = ComponentSize.privacyUnlockTitleTop),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ComponentSize.privacyUnlockCtaInset)
                    .widthIn(max = ComponentSize.privacyUnlockBoardWidth),
                style = privacyUnlockTextStyle(
                    fontSize = TextSize.privacyUnlockTitle,
                    lineHeight = TextSize.privacyUnlockTitleLine,
                    fontWeight = FontWeight.Bold,
                    color = DemoColors.textPrimary,
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                autoSize = DemoTextAutoSize.label(TextSize.privacyUnlockTitle),
            )
            Spacer(modifier = Modifier.height(ComponentSize.privacyUnlockTitleToPrice))
            PrivacyUnlockPriceRow(
                prefix = pricePrefix,
                suffix = priceSuffix,
            )
            Spacer(modifier = Modifier.height(ComponentSize.privacyUnlockPriceToCta))
            PrivacyUnlockCtaButton(
                onClick = onUnlock,
                isLoading = isUnlocking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ComponentSize.privacyUnlockCtaInset)
                    .widthIn(max = ComponentSize.privacyUnlockBoardWidth),
            )
            Spacer(modifier = Modifier.height(ComponentSize.privacyUnlockCtaToRemind))
            PrivacyUnlockDontRemindRow(
                checked = dontRemind,
                onCheckedChange = onDontRemindChange,
            )
            Spacer(modifier = Modifier.height(ComponentSize.privacyUnlockRemindBottom))
        }
    }
}

@Composable
private fun PrivacyUnlockPriceRow(
    prefix: String,
    suffix: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ComponentSize.privacyUnlockCtaInset),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prefix,
            style = privacyUnlockTextStyle(
                fontSize = TextSize.privacyUnlockPrice,
                lineHeight = TextSize.privacyUnlockPriceLine,
                fontWeight = FontWeight.Normal,
                color = DemoColors.textSecondary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(Spacing.xxs))
        Image(
            painter = painterResource(R.drawable.treasure_ic_coin),
            contentDescription = null,
            modifier = Modifier.size(ComponentSize.privacyUnlockPriceCoin),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.width(Spacing.xxs))
        Text(
            text = suffix,
            style = privacyUnlockTextStyle(
                fontSize = TextSize.privacyUnlockPrice,
                lineHeight = TextSize.privacyUnlockPriceLine,
                fontWeight = FontWeight.Normal,
                color = DemoColors.textSecondary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = DemoTextAutoSize.label(TextSize.privacyUnlockPrice),
        )
    }
}

@Composable
private fun PrivacyUnlockCtaButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val clickEnabled = !isLoading
    Box(
        modifier = modifier
            .height(ComponentSize.privacyUnlockCtaHeight)
            .dropShadow(shape = PrivacyUnlockCtaShape, shadow = PrivacyUnlockCtaShadow)
            .clip(PrivacyUnlockCtaShape)
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
                modifier = Modifier.size(ComponentSize.privacyUnlockCtaLock),
                color = DemoColors.onPrimaryButton,
                strokeWidth = Spacing.xxs,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.privacy_unlock_ic_lock),
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.privacyUnlockCtaLock),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = stringResource(R.string.privacy_unlock_cta),
                    style = privacyUnlockTextStyle(
                        fontSize = TextSize.privacyUnlockCta,
                        lineHeight = TextSize.privacyUnlockCta,
                        fontWeight = FontWeight.SemiBold,
                        color = DemoColors.onPrimaryButton,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = DemoTextAutoSize.label(TextSize.privacyUnlockCta),
                )
            }
        }
    }
}

@Composable
private fun PrivacyUnlockDontRemindRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(
                role = Role.Checkbox,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = { onCheckedChange(!checked) },
            )
            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(ComponentSize.privacyUnlockRemindCheck)
                .clip(PrivacyUnlockRemindCheckShape)
                .border(
                    width = ComponentSize.privacyUnlockRemindStroke,
                    color = if (checked) DemoColors.link else DemoColors.privacyUnlockRemindBorder,
                    shape = PrivacyUnlockRemindCheckShape,
                )
                .background(
                    if (checked) DemoColors.link else Color.Transparent,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Image(
                    painter = painterResource(R.drawable.privacy_unlock_ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.privacyUnlockRemindCheckIcon),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = stringResource(R.string.privacy_unlock_dont_remind),
            style = privacyUnlockTextStyle(
                fontSize = TextSize.privacyUnlockRemind,
                lineHeight = TextSize.privacyUnlockRemind,
                fontWeight = FontWeight.Medium,
                color = DemoColors.textSecondary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(showBackground = true, name = "PrivacyUnlock-Photo")
@Preview(locale = "ar", showBackground = true, name = "PrivacyUnlock-RTL")
@Composable
private fun PrivacyMediaUnlockSheetContentPreview() {
    DemoTheme {
        PrivacyMediaUnlockSheetContent(
            price = 110,
            isVideo = false,
            dontRemind = false,
            onDontRemindChange = {},
            onUnlock = {},
            onDismiss = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true, name = "PrivacyUnlock-Video")
@Composable
private fun PrivacyMediaUnlockSheetVideoPreview() {
    DemoTheme {
        PrivacyMediaUnlockSheetContent(
            price = 100,
            isVideo = true,
            dontRemind = true,
            onDontRemindChange = {},
            onUnlock = {},
            onDismiss = {},
        )
    }
}
