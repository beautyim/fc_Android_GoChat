package com.example.demoproject.product.home

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.model.OnlinePresence
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val tabJobs = mutableMapOf<OnlineFilter, Job>()

    init {
        viewModelScope.launch {
            runtime.accountBalanceStore.coins.collect { coins ->
                _uiState.update { it.copy(coinBalance = coins) }
            }
        }
        viewModelScope.launch { refreshSessionMeta() }
        ensureTabLoaded(OnlineFilter.All, force = false)
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Refresh -> ensureTabLoaded(_uiState.value.selectedFilter, force = true)
            HomeIntent.LoadMore -> loadMore()
            is HomeIntent.SelectFilter -> selectFilter(intent.filter)
            is HomeIntent.OpenUserProfile -> viewModelScope.launch {
                _effects.send(HomeEffect.OpenProfile(intent.user.profileRouteUserId))
            }
            is HomeIntent.OpenUserAction -> openUserAction(intent.user)
            HomeIntent.OpenCoins -> viewModelScope.launch { _effects.send(HomeEffect.OpenStore) }
            is HomeIntent.ReportUser -> viewModelScope.launch {
                _effects.send(HomeEffect.ShowMessage(str(R.string.home_online_report_coming_soon)))
            }
        }
    }

    private fun selectFilter(filter: OnlineFilter) {
        if (filter != _uiState.value.selectedFilter) {
            _uiState.update { it.copy(selectedFilter = filter) }
        }
        ensureTabLoaded(filter, force = false)
    }

    /**
     * @param force true for pull-to-refresh / retry — always hits network and updates cache.
     *              false uses cache when [OnlineTabPage.hasLoaded] is already true.
     */
    private fun ensureTabLoaded(filter: OnlineFilter, force: Boolean) {
        val cached = _uiState.value.pages[filter] ?: OnlineTabPage()
        if (!force && cached.hasLoaded) return
        // Guard on the in-flight job, not isLoading — All starts with isLoading=true for
        // first-frame skeleton, and a cancelled job can leave those flags stuck.
        if (tabJobs[filter]?.isActive == true) return

        tabJobs[filter]?.cancel()
        tabJobs[filter] = viewModelScope.launch {
            val keepList = force && cached.users.isNotEmpty()
            _uiState.update { state ->
                state.copyPage(filter) {
                    copy(
                        users = if (keepList) users else emptyList(),
                        isLoading = !keepList,
                        isRefreshing = keepList,
                        isLoadingMore = false,
                        errorMessage = null,
                        page = if (keepList) this.page else 1,
                        hasMore = if (keepList) hasMore else false,
                    )
                }
            }

            val result = runtime.profileRepository.discoverUsers(
                page = 1,
                flag = filter.flag,
                language = filter.language,
            )
            val free = _uiState.value.callFreeMin > 0
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copyPage(filter) {
                            copy(
                                users = result.data.users.map { user ->
                                    user.toOnlineUserUi(showFreeBadge = free)
                                },
                                hasMore = result.data.hasMore,
                                page = 1,
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = null,
                                hasLoaded = true,
                            )
                        }
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copyPage(filter) {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = result.message,
                                // Keep hasLoaded so a failed refresh doesn't wipe a good cache on next select.
                                hasLoaded = hasLoaded || users.isNotEmpty(),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadMore() {
        val filter = _uiState.value.selectedFilter
        val page = _uiState.value.pages[filter] ?: return
        if (!page.hasMore || page.isLoadingMore || page.isRefreshing || page.isLoading) return

        viewModelScope.launch {
            val nextPage = page.page + 1
            _uiState.update { state ->
                state.copyPage(filter) { copy(isLoadingMore = true) }
            }
            when (
                val result = runtime.profileRepository.discoverUsers(
                    page = nextPage,
                    flag = filter.flag,
                    language = filter.language,
                )
            ) {
                is AppResult.Success -> {
                    val free = _uiState.value.callFreeMin > 0
                    _uiState.update { state ->
                        state.copyPage(filter) {
                            copy(
                                users = users + result.data.users.map { user ->
                                    user.toOnlineUserUi(showFreeBadge = free)
                                },
                                hasMore = result.data.hasMore,
                                page = nextPage,
                                isLoadingMore = false,
                            )
                        }
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copyPage(filter) {
                            copy(
                                isLoadingMore = false,
                                errorMessage = result.message,
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun refreshSessionMeta() {
        when (val open = runtime.appSessionRepository.openApp(hasProxy = false, hasVpn = false)) {
            is AppResult.Success -> {
                val data = open.data
                if (data != null) {
                    runtime.accountBalanceStore.update(data.accountMoney)
                    applyCallFreeMin(data.callFreeMin)
                }
            }
            is AppResult.Failure -> {
                when (val init = runtime.appSessionRepository.initApp()) {
                    is AppResult.Success -> {
                        runtime.accountBalanceStore.update(init.data.accountMoney)
                        runtime.chatUnreadStore.update(init.data.messageUnread)
                    }
                    is AppResult.Failure -> Unit
                }
            }
        }
    }

    private fun applyCallFreeMin(callFreeMin: Int) {
        val free = callFreeMin > 0
        _uiState.update { state ->
            state.copy(
                callFreeMin = callFreeMin,
                pages = state.pages.mapValues { (_, page) ->
                    page.copy(
                        users = page.users.map { user ->
                            user.copy(
                                showFreeBadge = free && user.presence == OnlinePresence.Online,
                            )
                        },
                    )
                },
            )
        }
    }

    private fun openUserAction(user: OnlineUserUi) {
        viewModelScope.launch {
            if (user.prefersMessageAction) {
                _effects.send(
                    HomeEffect.OpenChatDetail(
                        conversationId = user.id,
                        nickname = user.nickname,
                    ),
                )
            } else {
                _effects.send(
                    HomeEffect.StartVideoCall(
                        userId = user.id,
                        nickname = user.nickname,
                    ),
                )
            }
        }
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)
}

private fun HomeUiState.copyPage(
    filter: OnlineFilter,
    transform: OnlineTabPage.() -> OnlineTabPage,
): HomeUiState {
    val current = pages[filter] ?: OnlineTabPage()
    return copy(pages = pages + (filter to current.transform()))
}
