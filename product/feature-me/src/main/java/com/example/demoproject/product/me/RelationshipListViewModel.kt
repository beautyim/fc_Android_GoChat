package com.example.demoproject.product.me

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RelationshipListViewModel(
    application: Application,
    type: RelationshipListType,
    expectedCount: Int,
) : AndroidViewModel(application) {

    private val repository = NetworkRuntime.get(application).profileRepository
    private val _uiState = MutableStateFlow(
        RelationshipListUiState(type = type, expectedCount = expectedCount.coerceAtLeast(0)),
    )
    val uiState: StateFlow<RelationshipListUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RelationshipListEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var page = 1

    init {
        load(reset = true)
    }

    fun onIntent(intent: RelationshipListIntent) {
        when (intent) {
            RelationshipListIntent.Refresh -> load(reset = true)
            RelationshipListIntent.LoadMore -> load(reset = false)
            is RelationshipListIntent.OpenProfile -> openProfile(intent.user)
            is RelationshipListIntent.OpenAction -> openAction(intent.user)
        }
    }

    private fun load(reset: Boolean) {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || state.isLoadingMore) return
        if (!reset && !state.hasMore) return

        val requestedPage = if (reset) 1 else page
        _uiState.update {
            it.copy(
                isLoading = reset && it.users.isEmpty(),
                isRefreshing = reset && it.users.isNotEmpty(),
                isLoadingMore = !reset,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            val result = when (_uiState.value.type) {
                RelationshipListType.Following ->
                    repository.getFollowingUsers(targetUserId = "", page = requestedPage)
                RelationshipListType.Followers ->
                    repository.getFollowerUsers(targetUserId = "", page = requestedPage)
            }
            when (result) {
                is AppResult.Success -> {
                    val incoming = result.data
                    page = requestedPage + 1
                    _uiState.update { current ->
                        val users = if (reset) {
                            incoming.users
                        } else {
                            (current.users + incoming.users).distinctBy(User::id)
                        }
                        current.copy(
                            users = users,
                            expectedCount = incoming.total.takeIf { it > 0 }
                                ?: current.expectedCount,
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            hasMore = incoming.hasMore,
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    private fun openProfile(user: User) {
        val id = user.externalUserId.ifBlank { user.id }
        viewModelScope.launch { _effects.send(RelationshipListEffect.OpenProfile(id)) }
    }

    private fun openAction(user: User) {
        val effect = if (user.onlinePresence().prefersMessageAction) {
            RelationshipListEffect.OpenChat(
                conversationId = user.id,
                nickname = user.nickname,
            )
        } else {
            RelationshipListEffect.StartVideoCall(user.id)
        }
        viewModelScope.launch { _effects.send(effect) }
    }
}
