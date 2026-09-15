package com.example.demoproject.product.me

import com.example.demoproject.platform.data.model.User

enum class RelationshipListType {
    Following,
    Followers,
}

data class RelationshipListUiState(
    val type: RelationshipListType,
    val expectedCount: Int = 0,
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface RelationshipListIntent {
    data object Refresh : RelationshipListIntent
    data object LoadMore : RelationshipListIntent
    data class OpenProfile(val user: User) : RelationshipListIntent
    data class OpenAction(val user: User) : RelationshipListIntent
}

sealed interface RelationshipListEffect {
    data class OpenProfile(val externalUserId: String) : RelationshipListEffect
    data class OpenChat(val conversationId: String, val nickname: String) : RelationshipListEffect
    data class StartVideoCall(val userId: String) : RelationshipListEffect
}
