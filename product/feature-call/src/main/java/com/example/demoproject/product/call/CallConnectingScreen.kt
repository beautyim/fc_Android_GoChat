package com.example.demoproject.product.call

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

/**
 * Random-match handoff while the accepted peer is joining RTC.
 *
 * [peerAvatarUrl] is the matched user's avatar, so the card never shows the
 * example portrait that Figma exported with the frame.
 */
@Composable
internal fun CallConnectingContent(
    peerName: String,
    peerAvatarUrl: String,
    notice: CallConnectingNotice?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.callConnectingBackground),
    ) {
        Image(
            painter = painterResource(R.drawable.call_connecting_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Exported already clipped to the top of the board, so it hangs off the
        // very top of the window rather than below the status bar.
        Image(
            painter = painterResource(R.drawable.call_connecting_title_glow),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .aspectRatio(ComponentSize.callConnectingTitleGlowAspect),
        )

        // The 48dp touch target is centred on the 24dp glyph, so back the
        // Figma inset off by half the surplus to keep the glyph at 16dp.
        val closeTouchInset =
            (ComponentSize.callConnectingCloseTouch - ComponentSize.callConnectingClose) / 2
        CallConnectingClose(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(
                    start = Spacing.callConnectingCloseHorizontal - closeTouchInset,
                    top = Spacing.callConnectingCloseTop - closeTouchInset,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = Spacing.callConnectingHeroTop),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CallConnectingTitle()
            Text(
                text = stringResource(
                    R.string.call_connecting_liked_fmt,
                    peerName.ifBlank { stringResource(R.string.call_ringing_unknown_peer) },
                ),
                color = DemoColors.callConnectingSubtitle,
                fontSize = TextSize.callConnectingSubtitle,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = Spacing.callConnectingSubtitleTop)
                    .padding(horizontal = Spacing.md),
            )
            CallConnectingAvatar(
                peerAvatarUrl = peerAvatarUrl,
                modifier = Modifier
                    .padding(top = Spacing.callConnectingAvatarTop)
                    .weight(1f, fill = false)
                    .widthIn(max = ComponentSize.callConnectingAvatarWidth)
                    // Height-first so a short window shrinks the card instead of
                    // squashing it; tall windows fall back to the Figma 246dp.
                    .aspectRatio(
                        ratio = ComponentSize.callConnectingAvatarAspect,
                        matchHeightConstraintsFirst = true,
                    ),
            )
            Text(
                text = stringResource(R.string.call_connecting_status),
                color = DemoColors.callConnectingStatus,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.Medium,
                letterSpacing = TextSize.callConnectingStatusTracking,
                modifier = Modifier.padding(top = Spacing.callConnectingStatusTop),
            )
        }

        notice?.let {
            val messageRes = when (it) {
                CallConnectingNotice.PeerLeft -> R.string.call_connecting_peer_left
                CallConnectingNotice.PoorNetwork -> R.string.call_connecting_poor_network
            }
            Text(
                text = stringResource(messageRes),
                color = DemoColors.callConnectingNoticeText,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = Spacing.callConnectingNoticeBottom)
                    .widthIn(max = ComponentSize.callConnectingNoticeWidth)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(DemoColors.sheet)
                    .padding(
                        horizontal = Spacing.callConnectingNoticeHorizontal,
                        vertical = Spacing.callConnectingNoticeVertical,
                    ),
            )
        }
    }
}

@Composable
private fun CallConnectingTitle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.callConnectingTitleGroupHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.call_connecting_lead),
            color = DemoColors.callRingingOnVideo,
            fontSize = TextSize.callConnectingLead,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { rotationZ = LeadRotationDegrees },
        )
        Text(
            text = stringResource(R.string.call_connecting_title),
            maxLines = 1,
            style = TextStyle(
                brush = DemoGradients.callConnectingTitle,
                fontSize = TextSize.callConnectingTitle,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { rotationZ = TitleRotationDegrees },
        )
    }
}

@Composable
private fun CallConnectingAvatar(
    peerAvatarUrl: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.sm)
    Box(
        modifier = modifier
            .shadow(
                elevation = ComponentSize.callConnectingAvatarGlow,
                shape = shape,
                ambientColor = DemoColors.callConnectingAvatarGlow,
                spotColor = DemoColors.callConnectingAvatarGlow,
            )
            .clip(shape)
            .background(DemoColors.callConnectingBackground)
            .border(
                width = ComponentSize.callConnectingAvatarStroke,
                color = DemoColors.callConnectingAvatarBorder,
                shape = shape,
            ),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(peerAvatarUrl.takeIf { it.isNotBlank() })
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(ComponentSize.callConnectingAvatarScrimHeight)
                .background(DemoGradients.callConnectingAvatarScrim),
        )
    }
}

@Composable
private fun CallConnectingClose(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cd = stringResource(R.string.call_connecting_cd_close)
    Box(
        modifier = modifier
            .size(ComponentSize.callConnectingCloseTouch)
            .clip(CircleShape)
            .semantics {
                contentDescription = cd
                role = Role.Button
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.call_connecting_ic_close),
            contentDescription = null,
            modifier = Modifier.size(ComponentSize.callConnectingClose),
        )
    }
}

private const val LeadRotationDegrees = -3.14f
private const val TitleRotationDegrees = -2.71f

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "Call connecting", widthDp = 375, heightDp = 812)
@Preview(name = "Call connecting RTL", locale = "ar", widthDp = 375, heightDp = 812)
@Composable
private fun CallConnectingPreview() {
    DemoTheme {
        CallConnectingContent(
            peerName = "Emma",
            peerAvatarUrl = "",
            notice = null,
            onClose = {},
        )
    }
}

@Preview(name = "Call connecting peer left", widthDp = 375, heightDp = 812)
@Composable
private fun CallConnectingPeerLeftPreview() {
    DemoTheme {
        CallConnectingContent(
            peerName = "Emma",
            peerAvatarUrl = "",
            notice = CallConnectingNotice.PeerLeft,
            onClose = {},
        )
    }
}

@Preview(name = "Call connecting poor network", widthDp = 375, heightDp = 812)
@Composable
private fun CallConnectingPoorNetworkPreview() {
    DemoTheme {
        CallConnectingContent(
            peerName = "Emma",
            peerAvatarUrl = "",
            notice = CallConnectingNotice.PoorNetwork,
            onClose = {},
        )
    }
}
