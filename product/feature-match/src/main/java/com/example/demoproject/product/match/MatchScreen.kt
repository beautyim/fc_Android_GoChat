package com.example.demoproject.product.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.R as FoundationR

@Composable
fun MatchScreen(viewModel: MatchViewModel, onBack: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.match_title))
        Text(state.status)
        Text(state.infoSummary)
        Text(state.startSummary)
        Button(onClick = { viewModel.onIntent(MatchIntent.LoadInfo) }) {
            Text(stringResource(R.string.match_action_get_info))
        }
        Button(onClick = { viewModel.onIntent(MatchIntent.Start) }) {
            Text(stringResource(R.string.match_action_start))
        }
        Button(onClick = onBack) { Text(stringResource(FoundationR.string.action_back)) }
    }
}
