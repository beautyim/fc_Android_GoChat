package com.example.demoproject.product.store

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradientPillButton
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

/**
 * After hangup-recharge purchase success — continue video call or open chat.
 *
 * Background: `store_hangup_continue_bg` (Figma `1 1113`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHangupContinueSheet(
    state: CallHangupContinueUiState,
    onDismiss: () -> Unit,
    onContinueVideo: () -> Unit,
    onOpenChat: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RectangleShape,
        containerColor = Color.Transparent,
        scrimColor = DemoColors.scrim,
        dragHandle = null,
        sheetGesturesEnabled = false,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CallHangupContinueContent(
                state = state,
                onDismiss = onDismiss,
                onContinueVideo = onContinueVideo,
                onOpenChat = onOpenChat,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(DemoColors.callHangupSheetBottom),
            )
        }
    }
}

@Composable
internal fun CallHangupContinueContent(
    state: CallHangupContinueUiState,
    onDismiss: () -> Unit,
    onContinueVideo: () -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeCd = stringResource(R.string.store_guide_cd_close)
    val shape = RoundedCornerShape(Radius.pill)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = ComponentSize.callHangupContinueMaxHeight),
    ) {
        Image(
            painter = painterResource(R.drawable.store_hangup_continue_bg),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(top = Spacing.callHangupContinueAvatarTop)
                .padding(bottom = Spacing.callHangupContinueBottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CallHangupPeerAvatar(
                avatarUrl = state.peerAvatarUrl,
                modifier = Modifier.size(ComponentSize.callHangupAvatar),
            )
            Spacer(modifier = Modifier.height(Spacing.callHangupContinueAvatarToName))
            Text(
                text = state.displayName,
                color = DemoColors.textPrimary,
                fontSize = TextSize.md,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Spacing.callHangupContinueNameToTitle))
            Text(
                text = stringResource(R.string.call_hangup_continue_waiting),
                color = DemoColors.textPrimary,
                fontSize = TextSize.callHangupContinueTitle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Spacing.callHangupContinueTitleToSubtitle))
            Text(
                text = stringResource(R.string.call_hangup_continue_subtitle),
                color = DemoColors.textSecondary,
                fontSize = TextSize.sm,
                lineHeight = TextSize.callHangupContinueSubtitleLine,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(Spacing.callHangupContinueSubtitleToCta))
            DemoGradientPillButton(
                text = stringResource(R.string.call_hangup_continue_video_cta),
                onClick = onContinueVideo,
                height = ComponentSize.callHangupContinueCtaHeight,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(Spacing.callHangupContinueCtaGap))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.callHangupContinueCtaHeight)
                    .border(
                        width = ComponentSize.callHangupSecondaryStroke,
                        color = DemoColors.callHangupSecondaryBorder,
                        shape = shape,
                    )
                    .clip(shape)
                    .clickable(
                        role = Role.Button,
                        onClick = onOpenChat,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.call_hangup_continue_chat_cta),
                    color = DemoColors.link,
                    fontSize = TextSize.md,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.md),
                )
            }
        }
        CallHangupCloseChip(
            contentDescription = closeCd,
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = Spacing.coinGuideCloseTop, end = Spacing.coinGuideCloseEnd),
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(locale = "ar")
@Composable
private fun CallHangupContinuePreview() {
    DemoTheme {
        CallHangupContinueContent(
            state = CallHangupContinueUiState(
                peerNickname = "Isabella",
                peerAge = 23,
            ),
            onDismiss = {},
            onContinueVideo = {},
            onOpenChat = {},
        )
    }
}
