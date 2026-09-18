package com.example.demoproject.product.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoGradients
import com.example.demoproject.ui.designsystem.DemoTextAutoSize
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

/**
 * Inline VIP unlock / subscribe prompt between the message list and composer
 * (Figma 1:2694 WaitingReply / 1:2787 UserInitiated).
 *
 * Not a LazyColumn row — fixed bottom chrome above the input bar.
 */
@Composable
fun ChatVipUnlockPromptCard(
    type: ChatUnlockPromptType,
    peerAvatarUrl: String?,
    peerNickname: String,
    onSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(Radius.sm)
    val (titleRes, subtitleRes) = when (type) {
        ChatUnlockPromptType.WaitingReply ->
            R.string.chat_detail_unlock_waiting_title to
                R.string.chat_detail_unlock_waiting_subtitle
        ChatUnlockPromptType.UserInitiated ->
            R.string.chat_detail_unlock_user_title to
                R.string.chat_detail_unlock_user_subtitle
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ComponentSize.chatListHorizontalInset)
            .height(ComponentSize.chatVipUnlockCardHeight)
            .shadow(
                elevation = ComponentSize.chatVipUnlockElevation,
                shape = cardShape,
                ambientColor = DemoColors.chatVipUnlockShadow,
                spotColor = DemoColors.chatVipUnlockShadow,
            )
            .clip(cardShape)
            .background(DemoColors.sheet)
            .testTag("chat_detail_vip_unlock_prompt"),
    ) {
        Image(
            painter = painterResource(R.drawable.chat_vip_unlock_decor),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = ChatVipUnlockDecorAlpha,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(ComponentSize.chatVipUnlockDecorWidth)
                .fillMaxHeight(),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = Spacing.sm, end = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            ChatVipUnlockAvatar(
                url = peerAvatarUrl,
                contentDescription = peerNickname,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(
                    ComponentSize.chatVipUnlockTitleToSubtitle,
                ),
            ) {
                Text(
                    text = stringResource(titleRes),
                    color = DemoColors.textPrimary,
                    fontSize = TextSize.chatVipUnlockTitle,
                    lineHeight = TextSize.chatVipUnlockTitleLine,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(subtitleRes),
                    color = DemoColors.textAuxiliary,
                    fontSize = TextSize.chatVipUnlockSubtitle,
                    lineHeight = TextSize.chatVipUnlockSubtitleLine,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ChatVipUnlockSubscribeButton(onClick = onSubscribe)
        }
    }
}

@Composable
private fun ChatVipUnlockAvatar(
    url: String?,
    contentDescription: String?,
) {
    val shape = CircleShape
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(ComponentSize.chatVipUnlockAvatar)
            .clip(shape)
            .background(DemoColors.chip),
    )
}

@Composable
private fun ChatVipUnlockSubscribeButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.sm)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .widthIn(min = ComponentSize.chatVipUnlockCtaWidth)
            .height(ComponentSize.chatVipUnlockCtaHeight)
            .clip(shape)
            .background(DemoGradients.primaryButton)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = DemoColors.onPrimaryButton),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.sm)
            .testTag("chat_detail_vip_unlock_subscribe"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.chat_detail_unlock_cta),
            color = DemoColors.onPrimaryButton,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontSize = TextSize.chatVipUnlockCta,
                lineHeight = TextSize.chatVipUnlockCtaLine,
                shadow = Shadow(
                    color = DemoColors.chatVipUnlockCtaTextShadow,
                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                    blurRadius = 1f,
                ),
            ),
            autoSize = DemoTextAutoSize.label(TextSize.chatVipUnlockCta),
        )
    }
}

/** Figma image 233 opacity on the unlock card. */
private const val ChatVipUnlockDecorAlpha = 0.3f

@Preview(showBackground = true, backgroundColor = 0xFFF9F9F9, widthDp = 375)
@PreviewFontScale
@Composable
private fun ChatVipUnlockWaitingPreview() {
    DemoTheme {
        ChatVipUnlockPromptCard(
            type = ChatUnlockPromptType.WaitingReply,
            peerAvatarUrl = null,
            peerNickname = "Terry",
            onSubscribe = {},
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF9F9F9, widthDp = 375, locale = "ar")
@Composable
private fun ChatVipUnlockUserInitiatedPreview() {
    DemoTheme {
        ChatVipUnlockPromptCard(
            type = ChatUnlockPromptType.UserInitiated,
            peerAvatarUrl = null,
            peerNickname = "Terry",
            onSubscribe = {},
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}
