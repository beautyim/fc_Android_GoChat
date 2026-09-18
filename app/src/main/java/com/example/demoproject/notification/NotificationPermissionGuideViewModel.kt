package com.example.demoproject.notification

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.DemoRoutes
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.notification.FeatureOverlayPresence
import com.example.demoproject.platform.data.notification.MatchImmersiveStore
import com.example.demoproject.platform.data.notification.NotificationPermissionGuideTrigger
import com.example.demoproject.platform.data.notification.NotificationPermissionGuideTriggerBus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Orchestrates the notification-permission guide dialog (docs/通知权限引导.md).
 *
 * Independent of commercial popup queue; shares mutual-exclusion + present gates.
 */
class NotificationPermissionGuideViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val runtime = NetworkRuntime.get(application)

    private val _uiState = MutableStateFlow(NotificationPermissionGuideUiState())
    val uiState: StateFlow<NotificationPermissionGuideUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<NotificationPermissionGuideEffect>(extraBufferCapacity = 4)
    val effects = _effects.asSharedFlow()

    private var hostResumed: Boolean = false
    private var matchImmersive: Boolean = false
    private var promotionBlocking: Boolean = false
    private var currentRoute: String = ""

    private var pendingJob: Job? = null

    init {
        viewModelScope.launch {
            NotificationPermissionGuideTriggerBus.events.collect { trigger ->
                onTrigger(trigger)
            }
        }
        viewModelScope.launch {
            MatchImmersiveStore.immersive.collect { immersive ->
                matchImmersive = immersive
            }
        }
        viewModelScope.launch {
            FeatureOverlayPresence.promotionPopupVisible.collect { promoVisible ->
                promotionBlocking = promoVisible
            }
        }
        viewModelScope.launch {
            runtime.sessionManager.sessionFlow.collect { session ->
                if (session == null) {
                    pendingJob?.cancel()
                    dismissInternal()
                }
            }
        }
    }

    fun updateHostResumed(resumed: Boolean) {
        hostResumed = resumed
    }

    fun updateCurrentRoute(route: String) {
        currentRoute = route
        matchImmersive = MatchImmersiveStore.immersive.value
        promotionBlocking = FeatureOverlayPresence.promotionPopupVisible.value
    }

    fun onIntent(intent: NotificationPermissionGuideIntent) {
        when (intent) {
            NotificationPermissionGuideIntent.TurnOn -> viewModelScope.launch {
                _effects.emit(NotificationPermissionGuideEffect.RequestNotificationPermission)
            }
            NotificationPermissionGuideIntent.Dismiss -> dismissInternal()
            NotificationPermissionGuideIntent.PermissionResult -> dismissInternal()
        }
    }

    private fun onTrigger(trigger: NotificationPermissionGuideTrigger) {
        pendingJob?.cancel()
        val delayMs = when (trigger) {
            is NotificationPermissionGuideTrigger.CallEndedSuccessfully,
            is NotificationPermissionGuideTrigger.FollowSuccess,
            -> TRIGGER_DELAY_MS
            // Let nav settle onto the previous page after chat detail is destroyed.
            is NotificationPermissionGuideTrigger.ChatSentThenLeft -> CHAT_LEAVE_SETTLE_MS
        }
        pendingJob = viewModelScope.launch {
            if (delayMs > 0L) delay(delayMs)
            tryPresent(trigger)
        }
    }

    private suspend fun tryPresent(trigger: NotificationPermissionGuideTrigger) {
        if (_uiState.value.visible) return
        if (areSystemNotificationsEnabled()) return
        if (!canPresentNow()) return
        // Prefer notification over commercial when both are eligible and neither is showing.
        if (FeatureOverlayPresence.promotionPopupVisible.value) return

        val userId = runtime.sessionManager.currentUserId.orEmpty()
        if (userId.isBlank()) return
        val lastShown = runtime.appPrefs.notificationPermissionGuideShownAtMs(userId)
        val now = System.currentTimeMillis()
        if (lastShown > 0L && now - lastShown < FREQUENCY_WINDOW_MS) return

        // Mark frequency only when actually displayed.
        runtime.appPrefs.markNotificationPermissionGuideShown(userId, now)
        FeatureOverlayPresence.setNotificationGuideVisible(true)
        _uiState.value = NotificationPermissionGuideUiState(
            visible = true,
            peerAvatarUrl = trigger.peerAvatarUrl,
            peerNickname = trigger.peerNickname,
        )
    }

    /**
     * Mirrors commercial「不弹出场景」for immersive / payment / call surfaces,
     * but unlike commercial auto-popups we still allow profile / chat-detail
     * (the three triggers often fire there).
     */
    private fun canPresentNow(): Boolean {
        if (!hostResumed) return false
        if (matchImmersive || MatchImmersiveStore.immersive.value) return false
        if (promotionBlocking || FeatureOverlayPresence.promotionPopupVisible.value) return false
        if (isBlockingRoute(currentRoute)) return false
        return true
    }

    private fun isBlockingRoute(route: String): Boolean {
        if (route.isBlank()) return true
        if (route == DemoRoutes.Auth) return true
        if (route == DemoRoutes.Store || route == DemoRoutes.Vip) return true
        // In-call page only — not call-records.
        if (route.startsWith("call/")) return true
        return false
    }

    private fun dismissInternal() {
        pendingJob?.cancel()
        pendingJob = null
        FeatureOverlayPresence.setNotificationGuideVisible(false)
        _uiState.update { it.copy(visible = false) }
    }

    private fun areSystemNotificationsEnabled(): Boolean {
        val app = getApplication<Application>()
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val FREQUENCY_WINDOW_MS = 24L * 60L * 60L * 1000L
        private const val TRIGGER_DELAY_MS = 2_000L
        private const val CHAT_LEAVE_SETTLE_MS = 300L
    }
}
