package com.example.demoproject.product.call

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.designsystem.DemoTheme
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

private val MoreSheetShape = RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallMoreSheet(
    micEnabled: Boolean,
    cameraEnabled: Boolean,
    onDismiss: () -> Unit,
    onMicChanged: (Boolean) -> Unit,
    onCameraChanged: (Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MoreSheetShape,
        containerColor = DemoColors.sheet,
        scrimColor = DemoColors.scrim,
        dragHandle = null,
    ) {
        CallMoreSheetContent(
            micEnabled = micEnabled,
            cameraEnabled = cameraEnabled,
            onMicChanged = onMicChanged,
            onCameraChanged = onCameraChanged,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
    }
}

@Composable
internal fun CallMoreSheetContent(
    micEnabled: Boolean,
    cameraEnabled: Boolean,
    onMicChanged: (Boolean) -> Unit,
    onCameraChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = Spacing.callInCallMoreTop),
    ) {
        CallMoreToggleRow(
            iconRes = R.drawable.call_incall_ic_mic,
            title = stringResource(R.string.call_incall_mic),
            enabled = micEnabled,
            showDivider = true,
            onCheckedChange = onMicChanged,
        )
        CallMoreToggleRow(
            iconRes = R.drawable.call_incall_ic_camera,
            title = stringResource(R.string.call_incall_camera),
            enabled = cameraEnabled,
            showDivider = false,
            onCheckedChange = onCameraChanged,
        )
    }
}

@Composable
private fun CallMoreToggleRow(
    iconRes: Int,
    title: String,
    enabled: Boolean,
    showDivider: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Spacing.md,
                    vertical = Spacing.callInCallMoreRowVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DemoColors.callInCallMoreIconBg)
                        .padding(Spacing.xs)
                        .size(ComponentSize.callInCallMoreIcon),
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xxs)) {
                    Text(
                        text = title,
                        color = DemoColors.textPrimary,
                        fontSize = TextSize.sm,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(
                            if (enabled) R.string.call_incall_toggle_on else R.string.call_incall_toggle_off,
                        ),
                        color = if (enabled) DemoColors.callInCallAccent else DemoColors.textAuxiliary,
                        fontSize = TextSize.xs,
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DemoColors.onPrimaryButton,
                    checkedTrackColor = DemoColors.callInCallAccent,
                    uncheckedThumbColor = DemoColors.onPrimaryButton,
                    uncheckedTrackColor = DemoColors.textAuxiliary,
                    uncheckedBorderColor = DemoColors.textAuxiliary,
                ),
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = DemoColors.callInCallMoreDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = Spacing.md),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CallMoreSheetPreview() {
    DemoTheme {
        CallMoreSheetContent(
            micEnabled = true,
            cameraEnabled = false,
            onMicChanged = {},
            onCameraChanged = {},
        )
    }
}
