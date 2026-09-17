package com.example.demoproject.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.example.demoproject.ui.foundation.ComponentSize
import com.example.demoproject.ui.foundation.Radius
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

data class DemoActionSheetItem(
    val label: String,
    val bold: Boolean = false,
    val onClick: () -> Unit,
)

private val ActionSheetShape = RoundedCornerShape(Radius.actionSheet)
private val ActionSheetLabelSize = 16.sp
private val ActionSheetTracking = (-0.4).sp

@Composable
fun DemoActionSheetContent(
    actions: List<DemoActionSheetItem>,
    cancelLabel: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ActionGroup(actions)
        ActionGroup(
            listOf(
                DemoActionSheetItem(
                    label = cancelLabel,
                    bold = true,
                    onClick = onCancel,
                ),
            ),
        )
    }
}

@Composable
private fun ActionGroup(actions: List<DemoActionSheetItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ActionSheetShape)
            .background(DemoColors.actionSheetSurface),
    ) {
        actions.forEachIndexed { index, action ->
            if (index > 0) {
                HorizontalDivider(
                    thickness = ComponentSize.actionSheetDivider,
                    color = DemoColors.actionSheetSeparator,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.actionSheetRow)
                    .clickable(role = Role.Button, onClick = action.onClick)
                    .padding(horizontal = Spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = action.label,
                    color = DemoColors.actionSheetAction,
                    fontSize = ActionSheetLabelSize,
                    fontWeight = if (action.bold) FontWeight.SemiBold else FontWeight.Normal,
                    letterSpacing = ActionSheetTracking,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = DemoTextAutoSize.label(ActionSheetLabelSize),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
