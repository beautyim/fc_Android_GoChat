package com.example.demoproject.notification

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.demoproject.ui.designsystem.NotificationPermissionGuideDialog

/**
 * Global host for the notification-permission guide (sibling of PromotionPopupHost).
 */
@Composable
fun NotificationPermissionGuideHost(
    navController: NavHostController,
    viewModel: NotificationPermissionGuideViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.onIntent(NotificationPermissionGuideIntent.PermissionResult)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.updateHostResumed(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.updateHostResumed(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            viewModel.updateCurrentRoute(entry.destination.route.orEmpty())
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                NotificationPermissionGuideEffect.RequestNotificationPermission -> {
                    val act = activity ?: return@collect
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        act.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, act.packageName)
                            },
                        )
                        viewModel.onIntent(NotificationPermissionGuideIntent.PermissionResult)
                    }
                }
            }
        }
    }

    if (state.visible) {
        NotificationPermissionGuideDialog(
            onTurnOn = { viewModel.onIntent(NotificationPermissionGuideIntent.TurnOn) },
            onDismiss = { viewModel.onIntent(NotificationPermissionGuideIntent.Dismiss) },
        )
    }
}
