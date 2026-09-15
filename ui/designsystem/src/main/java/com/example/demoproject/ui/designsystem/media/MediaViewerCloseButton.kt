package com.example.demoproject.ui.designsystem.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.R
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Spacing

/**
 * Figma media / gift overlay close — 44dp circle, black @ 30%, top-start inset 14dp.
 */
@Composable
fun MediaViewerCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(
                start = Spacing.md - Spacing.xxs,
                top = Spacing.md - Spacing.xxs,
            )
            .size(ComponentSize.profileMediaClose)
            .clip(CircleShape)
            .background(DemoColors.profileNavScrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.media_viewer_ic_close),
            contentDescription = stringResource(R.string.media_viewer_cd_close),
            modifier = Modifier.size(ComponentSize.profileMediaCloseIcon),
        )
    }
}
