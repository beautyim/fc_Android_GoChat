package com.example.demoproject.product.call

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.demoproject.ui.designsystem.DemoColors
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.TextSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CallReportSheet(
    state: CallReportUiState?,
    onDismiss: () -> Unit,
    onToggleReason: (Int) -> Unit,
    onSubmit: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DemoColors.sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.call_report_title),
                color = DemoColors.textTitle,
                fontSize = TextSize.title,
            )
            when {
                state == null || state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(Spacing.lg),
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        state.reasons.forEach { reason ->
                            val selected = reason.id in state.selectedReasonIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Checkbox) {
                                        onToggleReason(reason.id)
                                    }
                                    .padding(vertical = Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { onToggleReason(reason.id) },
                                )
                                Text(
                                    text = reason.title,
                                    color = DemoColors.textPrimary,
                                    fontSize = TextSize.md,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = state.selectedReasonIds.isNotEmpty() && !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(Spacing.xs),
                                color = DemoColors.onPrimaryButton,
                            )
                        } else {
                            Text(text = stringResource(R.string.call_report_submit))
                        }
                    }
                }
            }
        }
    }
}
