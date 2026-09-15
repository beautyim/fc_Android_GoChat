package com.example.demoproject.product.me

import com.example.demoproject.platform.data.model.User

data class BlockedUsersUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val errorMessage: String? = null,
    /** Ids with an in-flight unblock call, so the row can show progress. */
    val unblockingIds: Set<String> = emptySet(),
) {
    val isEmpty: Boolean get() = users.isEmpty() && !isLoading
}

sealed interface BlockedUsersIntent {
    data object Back : BlockedUsersIntent
    data object Refresh : BlockedUsersIntent
    data object LoadMore : BlockedUsersIntent
    data class Unblock(val user: User) : BlockedUsersIntent
}

sealed interface BlockedUsersEffect {
    data object NavigateBack : BlockedUsersEffect
    data class ShowMessage(val message: String) : BlockedUsersEffect
}
