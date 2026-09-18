package com.example.demoproject.notification

data class NotificationPermissionGuideUiState(
    val visible: Boolean = false,
    val peerAvatarUrl: String = "",
    val peerNickname: String = "",
)

sealed interface NotificationPermissionGuideIntent {
    data object TurnOn : NotificationPermissionGuideIntent
    data object Dismiss : NotificationPermissionGuideIntent
    data object PermissionResult : NotificationPermissionGuideIntent
}

sealed interface NotificationPermissionGuideEffect {
    data object RequestNotificationPermission : NotificationPermissionGuideEffect
}
