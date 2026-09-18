package com.example.demoproject.ui.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.TextSize

private val NotificationGuideSheetShape = RoundedCornerShape(
    topStart = Radius.lg,
    topEnd = Radius.lg,
)

private val NotificationGuideTightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private fun notificationGuideTextStyle(
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
    lineHeightStyle = NotificationGuideTightLineHeight,
)

/**
 * Notification permission guide — Figma 83:4565.
 *
 * [ModalBottomSheet] with hero illustration + title / body / primary CTA / dismiss.
 * Figma frame labels the CTA "Send Gifts" (likely a leftover); product copy uses
 * [R.string.notification_guide_cta] ("Turn On").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPermissionGuideDialog(
    onTurnOn: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = NotificationGuideSheetShape,
        containerColor = DemoColors.sheet,
        scrimColor = DemoColors.scrim,
        dragHandle = null,
        tonalElevation = 0.dp,
    ) {
        NotificationPermissionGuideSheetContent(
            onTurnOn = onTurnOn,
            onDismiss = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
    }
}

@Composable
internal fun NotificationPermissionGuideSheetContent(
    onTurnOn: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DemoColors.sheet),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero + copy/CTA share one box so title/body sit on the illustration wash
        // and the pill button straddles the image bottom (Figma 83:4567–83:4571).
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(R.drawable.notification_guide_hero),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ComponentSize.notificationGuideHeroAspect),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(ComponentSize.notificationGuideButtonOverhang))
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = ComponentSize.notificationGuideHorizontalInset),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.notification_guide_title),
                    style = notificationGuideTextStyle(
                        fontSize = TextSize.notificationGuideTitle,
                        lineHeight = TextSize.notificationGuideTitleLine,
                        fontWeight = FontWeight.SemiBold,
                        color = DemoColors.textPrimary,
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(ComponentSize.notificationGuideTitleToBody))
                Text(
                    text = stringResource(R.string.notification_guide_body),
                    modifier = Modifier.widthIn(max = ComponentSize.notificationGuideBodyWidth),
                    style = notificationGuideTextStyle(
                        fontSize = TextSize.notificationGuideBody,
                        lineHeight = TextSize.notificationGuideBodyLine,
                        fontWeight = FontWeight.Normal,
                        color = DemoColors.textSecondary,
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(ComponentSize.notificationGuideBodyToCta))
                DemoGradientPillButton(
                    text = stringResource(R.string.notification_guide_cta),
                    onClick = onTurnOn,
                    height = ComponentSize.pillButtonCompact,
                )
            }
        }
        Spacer(modifier = Modifier.height(ComponentSize.notificationGuideCtaToDismiss))
        Text(
            text = stringResource(R.string.notification_guide_next_time),
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false),
                    onClick = onDismiss,
                )
                .padding(
                    horizontal = ComponentSize.notificationGuideHorizontalInset,
                    vertical = ComponentSize.notificationGuideCtaToDismiss,
                ),
            style = notificationGuideTextStyle(
                fontSize = TextSize.notificationGuideDismiss,
                lineHeight = TextSize.notificationGuideDismissLine,
                fontWeight = FontWeight.Normal,
                color = DemoColors.textAuxiliary,
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(ComponentSize.notificationGuideDismissBottom))
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(showBackground = true, name = "NotificationGuide")
@Preview(locale = "ar", showBackground = true, name = "NotificationGuide-RTL")
@Composable
private fun NotificationPermissionGuideSheetContentPreview() {
    DemoTheme {
        NotificationPermissionGuideSheetContent(
            onTurnOn = {},
            onDismiss = {},
        )
    }
}
