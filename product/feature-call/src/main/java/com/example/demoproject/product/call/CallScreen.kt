package com.example.demoproject.product.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun CallScreen(viewModel: CallViewModel, onBack: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.call_title))
        Text(state.mediaHint)
        Text(stringResource(R.string.call_label_coordinator_fmt, state.coordinatorState))
        Text(state.status)
        Button(onClick = { viewModel.onIntent(CallIntent.LoadRecords) }) {
            Text(stringResource(R.string.call_action_reload_records))
        }
        Button(onClick = { viewModel.onIntent(CallIntent.StartVideoCall) }) {
            Text(stringResource(R.string.call_action_start_video))
        }
        Button(onClick = { viewModel.onIntent(CallIntent.StartVoiceCall) }) {
            Text(stringResource(R.string.call_action_start_voice))
        }
        Button(onClick = { viewModel.onIntent(CallIntent.Hangup) }) {
            Text(stringResource(R.string.call_action_hangup))
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.records) { Text(it) }
        }
        Button(onClick = onBack) { Text(stringResource(FoundationR.string.action_back)) }
    }
}
