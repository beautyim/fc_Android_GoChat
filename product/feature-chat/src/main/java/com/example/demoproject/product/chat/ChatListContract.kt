package com.example.demoproject.product.chat

data class ChatListUiState(
    val conversations: List<ChatConversationUi> = emptyList(),
    val totalUnread: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val page: Int = 1,
    val hasLoaded: Boolean = false,
    val errorMessage: String? = null,
    val showNotificationBanner: Boolean = false,
    val revealedConversationId: String? = null,
    val pendingDeleteId: String? = null,
    val isDeleting: Boolean = false,
)

data class ChatConversationUi(
    val id: String,
    val peerId: String,
    val peerExternalUserId: String,
    val nickname: String,
    val age: Int,
    val avatarUrl: String?,
    val preview: String,
    val timestampLabel: String,
    val unreadCount: Int,
    val isPinned: Boolean,
) {
    val title: String
        get() = if (age > 0) "$nickname, $age" else nickname

    val profileRouteUserId: String
        get() = peerExternalUserId.ifBlank { peerId }
}

sealed interface ChatListIntent {
    data object Refresh : ChatListIntent
    data object LoadMore : ChatListIntent
    data object Sync : ChatListIntent
    data class OpenConversation(val conversation: ChatConversationUi) : ChatListIntent
    data class RevealActions(val conversationId: String?) : ChatListIntent
    data class TogglePin(val conversationId: String) : ChatListIntent
    data class RequestDelete(val conversationId: String) : ChatListIntent
    data object ConfirmDelete : ChatListIntent
    data object CancelDelete : ChatListIntent
    data object DismissNotificationBanner : ChatListIntent
    data object TurnOnNotifications : ChatListIntent
    data object NotificationPermissionResult : ChatListIntent
    data object OpenSupport : ChatListIntent
    data object StartChatting : ChatListIntent
}

sealed interface ChatListEffect {
    data class OpenChatDetail(val conversationId: String, val nickname: String) : ChatListEffect
    data object OpenHome : ChatListEffect
    data object OpenSupport : ChatListEffect
    data object RequestNotificationPermission : ChatListEffect
    data class ShowMessage(val message: String) : ChatListEffect
}
