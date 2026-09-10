package com.example.demoproject.product.store

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demoproject.ui.foundation.Spacing
import com.example.demoproject.ui.foundation.R as FoundationR

@Composable
fun StoreScreen(viewModel: StoreViewModel, onBack: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        viewModel.bindActivity(activity)
        onDispose { viewModel.bindActivity(null) }
    }
    Column(Modifier.fillMaxSize().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.store_title))
        Text(state.status)
        Text(state.lastOrder)
        Button(onClick = { viewModel.onIntent(StoreIntent.LoadCatalog) }) {
            Text(stringResource(R.string.store_action_reload_catalog))
        }
        Button(onClick = { viewModel.onIntent(StoreIntent.CreateOrder) }) {
            Text(stringResource(R.string.store_action_create_order_http))
        }
        Button(onClick = { viewModel.onIntent(StoreIntent.LaunchGooglePlayPurchase) }) {
            Text(stringResource(R.string.store_action_buy_play))
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.products) { Text(it) }
        }
        Button(onClick = onBack) { Text(stringResource(FoundationR.string.action_back)) }
    }
}
