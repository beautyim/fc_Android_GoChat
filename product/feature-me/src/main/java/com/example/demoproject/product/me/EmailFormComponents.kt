package com.example.demoproject.product.me

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoNavIconButton
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.IconSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

private val EmailInputLineHeight = 14.sp
private val EmailFieldStroke = 1.dp
private val SendCodeProgressStroke = 2.dp

private val TightLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

@Composable
internal fun EmailTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.navHeaderHeight)
            .background(DemoColors.sheet),
    ) {
        DemoNavIconButton(
            icon = painterResource(R.drawable.settings_ic_back),
            contentDescription = stringResource(R.string.settings_cd_back),
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = Spacing.md),
            enabled = enabled,
            mirrorInRtl = true,
        )
        Text(
            text = stringResource(R.string.email_nav_title),
            color = DemoColors.navTitle,
            fontSize = TextSize.md,
            fontWeight = FontWeight.SemiBold,
            lineHeight = TextSize.mdLine,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = TightLineHeightStyle,
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
        )
    }
}

@Composable
internal fun EmailSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = DemoColors.textPrimary,
        fontSize = TextSize.md,
        fontWeight = FontWeight.SemiBold,
        lineHeight = TextSize.mdLine,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = TightLineHeightStyle,
        ),
        modifier = modifier,
    )
}

@Composable
internal fun EmailSectionSubtitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = DemoColors.textSecondary,
        fontSize = TextSize.xs,
        textAlign = TextAlign.Center,
        lineHeight = TextSize.emailSubtitleLine,
        style = TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = TightLineHeightStyle,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun EmailLabeledField(
    label: String,
    @StringRes errorRes: Int?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.emailLabelGap),
    ) {
        Text(
            text = label,
            color = DemoColors.textPrimary,
            fontSize = TextSize.sm,
            fontWeight = FontWeight.Medium,
            lineHeight = TextSize.smLine,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = TightLineHeightStyle,
            ),
        )
        content()
        if (errorRes != null) {
            Text(
                text = stringResource(errorRes),
                color = DemoColors.error,
                fontSize = TextSize.xs,
                lineHeight = TextSize.emailSubtitleLine,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = TightLineHeightStyle,
                ),
            )
        }
    }
}

@Composable
internal fun EmailReadonlyField(
    value: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.sm)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.emailReadonlyHeight)
            .clip(shape)
            .background(DemoColors.sheet)
            .border(EmailFieldStroke, DemoColors.emailFieldBorder, shape)
            .padding(horizontal = Spacing.emailFieldPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = value,
            color = DemoColors.textPrimary,
            fontSize = TextSize.xs,
            lineHeight = EmailInputLineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = TightLineHeightStyle,
            ),
        )
    }
}

@Composable
internal fun EmailInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    @DrawableRes leadingIconRes: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onFocused: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(Radius.sm)
    val borderColor = if (isError) DemoColors.error else DemoColors.emailFieldBorder
    val fieldTextStyle = TextStyle(
        color = DemoColors.textPrimary,
        fontSize = TextSize.xs,
        lineHeight = EmailInputLineHeight,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = TightLineHeightStyle,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.emailInputHeight)
            .clip(shape)
            .background(DemoColors.sheet)
            .border(EmailFieldStroke, borderColor, shape)
            .padding(horizontal = Spacing.emailFieldPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.emailFieldIconGap),
    ) {
        Image(
            painter = painterResource(leadingIconRes),
            contentDescription = null,
            modifier = Modifier.size(ComponentSize.emailFieldIcon),
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = DemoColors.inputPlaceholder,
                    fontSize = TextSize.xs,
                    lineHeight = EmailInputLineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = TightLineHeightStyle,
                    ),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = fieldTextStyle,
                cursorBrush = SolidColor(DemoColors.link),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) onFocused?.invoke()
                    },
            )
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
internal fun EmailSendCodeAction(
    cooldownSec: Int,
    enabled: Boolean,
    isSending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isSending) {
        CircularProgressIndicator(
            modifier = modifier.size(IconSize.sm),
            color = DemoColors.link,
            strokeWidth = SendCodeProgressStroke,
        )
        return
    }
    Text(
        text = if (cooldownSec > 0) {
            stringResource(R.string.email_action_send_code_countdown, cooldownSec)
        } else {
            stringResource(R.string.email_action_send_code)
        },
        color = DemoColors.link.copy(alpha = if (enabled) 1f else 0.5f),
        fontSize = TextSize.xs,
        fontWeight = FontWeight.Medium,
        lineHeight = EmailInputLineHeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = TightLineHeightStyle,
        ),
        modifier = modifier.clickable(
            enabled = enabled,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Button,
            onClick = onClick,
        ),
    )
}
