package com.example.demoproject.product.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing

private val ActionSheetShape = RoundedCornerShape(Radius.actionSheet)
private val ActionSheetLabelSize = 16.sp
private val ActionSheetTracking = (-0.4).sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMediaTypeSheet(
    onSendImage: () -> Unit,
    onSendVideo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RectangleShape,
        containerColor = Color.Transparent,
        scrimColor = DemoColors.scrim,
        dragHandle = null,
        tonalElevation = 0.dp,
    ) {
        ChatMediaTypeSheetContent(
            onSendImage = onSendImage,
            onSendVideo = onSendVideo,
            onCancel = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.chipGap)
                .padding(bottom = Spacing.lg),
        )
    }
}

@Composable
internal fun ChatMediaTypeSheetContent(
    onSendImage: () -> Unit,
    onSendVideo: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ActionSheetShape)
                .background(DemoColors.actionSheetSurface),
        ) {
            ActionSheetRow(
                label = stringResource(R.string.chat_detail_media_send_image),
                bold = false,
                onClick = onSendImage,
            )
            ActionSheetDivider()
            ActionSheetRow(
                label = stringResource(R.string.chat_detail_media_send_video),
                bold = false,
                onClick = onSendVideo,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ActionSheetShape)
                .background(DemoColors.actionSheetSurface),
        ) {
            ActionSheetRow(
                label = stringResource(R.string.chat_detail_media_cancel),
                bold = true,
                onClick = onCancel,
            )
        }
    }
}

@Composable
private fun ActionSheetDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = DemoColors.actionSheetSeparator,
    )
}

@Composable
private fun ActionSheetRow(
    label: String,
    bold: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ComponentSize.actionSheetRow)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = DemoColors.actionSheetAction,
            fontSize = ActionSheetLabelSize,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = ActionSheetTracking,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun ChatMediaTypeSheetPreview() {
    DemoTheme {
        ChatMediaTypeSheetContent(
            onSendImage = {},
            onSendVideo = {},
            onCancel = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.chipGap),
        )
    }
}
