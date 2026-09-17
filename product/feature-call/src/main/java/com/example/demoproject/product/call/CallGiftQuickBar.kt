package com.example.demoproject.product.call

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoTextAutoSize
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

@Composable
fun CallGiftQuickBar(
    gifts: List<CallGiftUi>,
    enabled: Boolean,
    onSendGift: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (gifts.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.chipGap),
        horizontalArrangement = Arrangement.spacedBy(Spacing.giftCardGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(items = gifts, key = { it.id }) { gift ->
            CallGiftQuickItem(
                gift = gift,
                enabled = enabled,
                onClick = { onSendGift(gift.id) },
            )
        }
    }
}

@Composable
private fun CallGiftQuickItem(
    gift: CallGiftUi,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val cd = stringResource(
        R.string.call_incall_cd_quick_gift,
        gift.title.ifBlank { gift.price.toString() },
    )
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.chatBanner))
            .semantics { contentDescription = cd }
            .clickable(
                enabled = enabled && gift.id > 0L,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(Spacing.xs + Spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(gift.iconUrl.takeIf { it.isNotBlank() })
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            placeholder = painterResource(R.drawable.call_incall_ic_gift),
            error = painterResource(R.drawable.call_incall_ic_gift),
            modifier = Modifier.size(ComponentSize.callInCallGiftQuickIcon),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            Text(
                text = gift.price.toString(),
                color = DemoColors.callInCallGiftQuickPrice,
                fontSize = TextSize.xs,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = DemoTextAutoSize.price(TextSize.xs),
            )
            Image(
                painter = painterResource(R.drawable.call_incall_ic_coin),
                contentDescription = null,
                modifier = Modifier.size(ComponentSize.callInCallGiftQuickCoin),
            )
        }
    }
}

@Preview(name = "CallGiftQuickBar", showBackground = true, backgroundColor = 0xFF333333)
@Composable
private fun CallGiftQuickBarPreview() {
    DemoTheme {
        CallGiftQuickBar(
            gifts = listOf(
                CallGiftUi(id = 1, title = "Lollipop", price = 50, iconUrl = ""),
                CallGiftUi(id = 2, title = "Lips", price = 50, iconUrl = ""),
                CallGiftUi(id = 3, title = "Berry", price = 50, iconUrl = ""),
            ),
            enabled = true,
            onSendGift = {},
        )
    }
}
