package com.example.demoproject.platform.data.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cross-feature triggers for the notification-permission guide dialog.
 * Collected by the app-layer host; features only emit.
 */
object NotificationPermissionGuideTriggerBus {
    private val _events = MutableSharedFlow<NotificationPermissionGuideTrigger>(
        extraBufferCapacity = 16,
    )
    val events: SharedFlow<NotificationPermissionGuideTrigger> = _events.asSharedFlow()

    fun emit(event: NotificationPermissionGuideTrigger) {
        _events.tryEmit(event)
    }
}

sealed interface NotificationPermissionGuideTrigger {
    val peerAvatarUrl: String
    val peerNickname: String

    /** Connected call ended by local or remote hangup (not insufficient-balance). */
    data class CallEndedSuccessfully(
        override val peerAvatarUrl: String = "",
        override val peerNickname: String = "",
    ) : NotificationPermissionGuideTrigger

    /** User successfully followed someone. */
    data class FollowSuccess(
        override val peerAvatarUrl: String = "",
        override val peerNickname: String = "",
    ) : NotificationPermissionGuideTrigger

    /** User sent at least one outbound message this visit, then left the chat detail. */
    data class ChatSentThenLeft(
        override val peerAvatarUrl: String = "",
        override val peerNickname: String = "",
    ) : NotificationPermissionGuideTrigger
}

/**
 * Whether Match is in an immersive searching/matched session.
 * Written by [MatchViewModel]; read by notification + promotion gates.
 */
object MatchImmersiveStore {
    private val _immersive = MutableStateFlow(false)
    val immersive: StateFlow<Boolean> = _immersive.asStateFlow()

    fun setImmersive(value: Boolean) {
        _immersive.value = value
    }
}

/**
 * Cross-feature overlay visibility for mutual exclusion between
 * notification-permission guide and commercial promotion popups.
 */
object FeatureOverlayPresence {
    private val _notificationGuideVisible = MutableStateFlow(false)
    private val _promotionPopupVisible = MutableStateFlow(false)

    val notificationGuideVisible: StateFlow<Boolean> = _notificationGuideVisible.asStateFlow()
    val promotionPopupVisible: StateFlow<Boolean> = _promotionPopupVisible.asStateFlow()

    fun setNotificationGuideVisible(visible: Boolean) {
        _notificationGuideVisible.value = visible
    }

    fun setPromotionPopupVisible(visible: Boolean) {
        _promotionPopupVisible.value = visible
    }
}
