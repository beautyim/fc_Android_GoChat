package com.example.demoproject.product.profile.more

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.demoproject.product.profile.R
import com.example.demoproject.ui.designsystem.DemoActionSheetContent
import com.example.demoproject.ui.designsystem.DemoActionSheetItem
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMoreSheet(
    isFollowing: Boolean,
    isBlocked: Boolean,
    onFollow: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
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
        ProfileMoreSheetContent(
            isFollowing = isFollowing,
            isBlocked = isBlocked,
            onFollow = onFollow,
            onBlock = onBlock,
            onReport = onReport,
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
internal fun ProfileMoreSheetContent(
    isFollowing: Boolean,
    isBlocked: Boolean,
    onFollow: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DemoActionSheetContent(
        modifier = modifier,
        actions = listOf(
            DemoActionSheetItem(
                label = stringResource(
                    if (isFollowing) R.string.profile_more_unfollow else R.string.profile_follow,
                ),
                onClick = onFollow,
            ),
            DemoActionSheetItem(
                label = stringResource(
                    if (isBlocked) R.string.profile_more_unblock else R.string.profile_more_block,
                ),
                onClick = onBlock,
            ),
            DemoActionSheetItem(
                label = stringResource(R.string.profile_more_report),
                onClick = onReport,
            ),
        ),
        cancelLabel = stringResource(R.string.profile_more_cancel),
        onCancel = onCancel,
    )
}

@Preview
@Composable
private fun ProfileMoreSheetPreview() {
    DemoTheme {
        ProfileMoreSheetContent(
            isFollowing = false,
            isBlocked = false,
            onFollow = {},
            onBlock = {},
            onReport = {},
            onCancel = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.chipGap),
        )
    }
}
