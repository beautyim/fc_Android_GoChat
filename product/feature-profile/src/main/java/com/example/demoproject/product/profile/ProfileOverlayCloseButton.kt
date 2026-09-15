package com.example.demoproject.product.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.demoproject.ui.designsystem.media.MediaViewerCloseButton

/**
 * Profile overlay close control — shared [MediaViewerCloseButton] styling.
 */
@Composable
fun ProfileOverlayCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MediaViewerCloseButton(onClick = onClick, modifier = modifier)
}
