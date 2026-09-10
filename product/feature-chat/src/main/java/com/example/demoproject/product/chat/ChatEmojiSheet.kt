package com.example.demoproject.product.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

/**
 * Unicode smileys from Figma 消息详情-表情 (176:5014).
 * Rendered as text glyphs — system emoji font, not custom assets.
 */
internal val ChatEmojiCatalog: List<String> = listOf(
    "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
    "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
    "😘", "😗", "😚", "😙", "🥲", "😋", "😛", "😜",
    "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐",
    "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬",
    "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒",
    "🤕", "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵",
    "🤯", "🤠", "🥳", "🥸", "😎", "🤓", "🧐",
)

private val EmojiGlyphStyle = TextStyle(
    fontSize = TextSize.chatEmoji,
    lineHeight = TextSize.chatEmojiLine,
    textAlign = TextAlign.Center,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

/** Inline emoji panel under the chat composer (keyboard-replacement). */
@Composable
fun ChatEmojiSheet(
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    emojis: List<String> = ChatEmojiCatalog,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DemoColors.sheet),
    ) {
        HorizontalDivider(
            thickness = ComponentSize.chatEmojiPanelHairline,
            color = DemoColors.giftCardBorder,
        )
        ChatEmojiSheetContent(
            emojis = emojis,
            onSelect = onSelect,
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.chatEmojiPanelHeight),
        )
    }
}

@Composable
internal fun ChatEmojiSheetContent(
    emojis: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(ComponentSize.chatEmojiColumns),
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = Spacing.chatEmojiGap,
            vertical = Spacing.chipGap,
        ),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.chatEmojiGap),
    ) {
        items(emojis, key = { it }) { emoji ->
            EmojiCell(emoji = emoji, onClick = { onSelect(emoji) })
        }
    }
}

@Composable
private fun EmojiCell(
    emoji: String,
    onClick: () -> Unit,
) {
    val cd = stringResource(R.string.chat_detail_cd_emoji_item, emoji)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ComponentSize.chatEmojiCell)
            .clickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            style = EmojiGlyphStyle,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Preview(name = "ChatEmoji", locale = "en")
@Preview(name = "ChatEmoji RTL", locale = "ar")
@Composable
private fun ChatEmojiSheetPreview() {
    DemoTheme {
        ChatEmojiSheet(onSelect = {})
    }
}
