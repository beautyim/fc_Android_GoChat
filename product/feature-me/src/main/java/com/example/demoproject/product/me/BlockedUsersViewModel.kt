package com.example.demoproject.product.me

import android.app.Application
import androidx.annotation.StringRes
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

class BlockedUsersViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = NetworkRuntime.get(application).blockRepository
    private val _uiState = MutableStateFlow(BlockedUsersUiState())
    val uiState: StateFlow<BlockedUsersUiState> = _uiState.asStateFlow()

    private val _effects = Channel<BlockedUsersEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var page = 1

    init {
        load(reset = true)
    }

    fun onIntent(intent: BlockedUsersIntent) {
        when (intent) {
            BlockedUsersIntent.Back -> viewModelScope.launch {
                _effects.send(BlockedUsersEffect.NavigateBack)
            }
            BlockedUsersIntent.Refresh -> load(reset = true)
            BlockedUsersIntent.LoadMore -> load(reset = false)
            is BlockedUsersIntent.Unblock -> unblock(intent.user)
        }
    }

    private fun load(reset: Boolean) {
        val state = _uiState.value
        if (state.isLoading && !reset) return
        if (state.isRefreshing || state.isLoadingMore) return
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
            when (val result = repository.getBlockedUsersPage(requestedPage)) {
                is AppResult.Success -> {
                    page = requestedPage + 1
                    _uiState.update { current ->
                        val users = if (reset) {
                            result.data.users
                        } else {
                            (current.users + result.data.users).distinctBy(User::id)
                        }
                        current.copy(
                            users = users,
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            hasMore = result.data.hasMore && result.data.users.isNotEmpty(),
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

    private fun unblock(user: User) {
        val uid = user.id.toLongOrNull()
        if (uid == null || uid <= 0L) {
            emitMessage(str(R.string.blocked_unlock_failed))
            return
        }
        if (user.id in _uiState.value.unblockingIds) return

        _uiState.update { it.copy(unblockingIds = it.unblockingIds + user.id) }
        viewModelScope.launch {
            val result = repository.unblockUser(uid)
            _uiState.update { current ->
                current.copy(
                    unblockingIds = current.unblockingIds - user.id,
                    users = if (result is AppResult.Success) {
                        current.users.filterNot { it.id == user.id }
                    } else {
                        current.users
                    },
                )
            }
            if (result is AppResult.Failure) {
                emitMessage(result.message.ifBlank { str(R.string.blocked_unlock_failed) })
            }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch { _effects.send(BlockedUsersEffect.ShowMessage(message)) }
    }

    private fun str(@StringRes id: Int): String =
        getApplication<Application>().getString(id)
}
