package com.example.demoproject.product.call

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.blocked.excludingBlockedCallRecords
import com.example.demoproject.platform.data.model.CallRecord
import com.example.demoproject.platform.data.model.CallRecordStatus
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

class CallRecordsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(CallRecordsUiState())
    val uiState: StateFlow<CallRecordsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CallRecordsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val tabJobs = mutableMapOf<CallRecordsTab, Job>()

    init {
        viewModelScope.launch {
            runtime.accountBalanceStore.coins.collect { coins ->
                _uiState.update { it.copy(coinBalance = coins) }
            }
        }
        ensureTabLoaded(CallRecordsTab.All, force = false)
    }

    fun onIntent(intent: CallRecordsIntent) {
        when (intent) {
            CallRecordsIntent.Refresh -> ensureTabLoaded(_uiState.value.selectedTab, force = true)
            CallRecordsIntent.LoadMore -> loadMore()
            is CallRecordsIntent.SelectTab -> selectTab(intent.tab)
            is CallRecordsIntent.OpenProfile -> viewModelScope.launch {
                _effects.send(CallRecordsEffect.OpenProfile(intent.record.profileRouteUserId))
            }
            is CallRecordsIntent.StartCall -> viewModelScope.launch {
                _effects.send(
                    CallRecordsEffect.StartVideoCall(
                        userId = intent.record.peerId,
                        nickname = intent.record.nickname,
                        avatarUrl = intent.record.avatarUrl.orEmpty(),
                        age = intent.record.age,
                    ),
                )
            }
            CallRecordsIntent.OpenCoins -> viewModelScope.launch {
                _effects.send(CallRecordsEffect.OpenStore)
            }
            CallRecordsIntent.StartVideoChat -> viewModelScope.launch {
                _effects.send(CallRecordsEffect.OpenMatch)
            }
        }
    }

    private fun selectTab(tab: CallRecordsTab) {
        if (tab != _uiState.value.selectedTab) {
            _uiState.update { it.copy(selectedTab = tab) }
        }
        ensureTabLoaded(tab, force = false)
    }

    private fun ensureTabLoaded(tab: CallRecordsTab, force: Boolean) {
        val cached = _uiState.value.pages[tab] ?: CallRecordsTabPage()
        if (!force && cached.hasLoaded) return
        if (tabJobs[tab]?.isActive == true) return

        tabJobs[tab]?.cancel()
        tabJobs[tab] = viewModelScope.launch {
            // Keep empty/list UI during pull-to-refresh; only first load uses skeleton.
            val keepContent = force && cached.hasLoaded
            _uiState.update { state ->
                state.copyPage(tab) {
                    copy(
                        records = if (keepContent) records else emptyList(),
                        isLoading = !keepContent,
                        isRefreshing = keepContent,
                        isLoadingMore = false,
                        errorMessage = null,
                        page = if (keepContent) this.page else 1,
                        hasMore = if (keepContent) hasMore else false,
                    )
                }
            }
            when (
                val result = runtime.callRepository.getCallRecords(
                    page = 1,
                    type = tab.apiType,
                )
            ) {
                is AppResult.Success -> {
                    val page = result.data.excludingBlockedCallRecords(runtime.blockedUsersStore)
                    _uiState.update { state ->
                        state.copyPage(tab) {
                            copy(
                                records = page.records.map { it.toUi() },
                                page = 1,
                                hasMore = page.hasMore,
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
                        state.copyPage(tab) {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = result.message,
                                hasLoaded = keepContent,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadMore() {
        val tab = _uiState.value.selectedTab
        val page = _uiState.value.pages[tab] ?: return
        if (!page.hasMore || page.isLoadingMore || page.isLoading || page.isRefreshing) return
        if (tabJobs[tab]?.isActive == true) return

        tabJobs[tab] = viewModelScope.launch {
            val nextPage = page.page + 1
            _uiState.update { state ->
                state.copyPage(tab) { copy(isLoadingMore = true, errorMessage = null) }
            }
            when (
                val result = runtime.callRepository.getCallRecords(
                    page = nextPage,
                    type = tab.apiType,
                )
            ) {
                is AppResult.Success -> {
                    val loaded = result.data.excludingBlockedCallRecords(runtime.blockedUsersStore)
                    _uiState.update { state ->
                        state.copyPage(tab) {
                            copy(
                                records = records + loaded.records.map { it.toUi() },
                                page = nextPage,
                                hasMore = loaded.hasMore,
                                isLoadingMore = false,
                            )
                        }
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copyPage(tab) {
                            copy(isLoadingMore = false, errorMessage = result.message)
                        }
                    }
                }
            }
        }
    }

    private fun CallRecord.toUi(): CallRecordUi {
        val tone = when (status) {
            CallRecordStatus.Missed,
            CallRecordStatus.Declined,
            CallRecordStatus.Cancelled,
            -> CallRecordTone.Negative
            CallRecordStatus.Connected,
            CallRecordStatus.Match,
            CallRecordStatus.Unknown,
            -> CallRecordTone.Positive
        }
        val fallback = when (status) {
            CallRecordStatus.Missed -> str(R.string.call_records_status_missed)
            CallRecordStatus.Declined -> str(R.string.call_records_status_rejected)
            CallRecordStatus.Cancelled -> str(R.string.call_records_status_cancelled)
            else -> str(R.string.call_records_status_match_default)
        }
        return CallRecordUi(
            id = id,
            peerId = peer.id,
            peerExternalUserId = peer.externalUserId.ifBlank { peer.id },
            nickname = peer.nickname.ifBlank { peer.id },
            age = peer.age,
            avatarUrl = peer.avatar,
            statusText = description.ifBlank { fallback },
            tone = tone,
        )
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)
}

private fun CallRecordsUiState.copyPage(
    tab: CallRecordsTab,
    transform: CallRecordsTabPage.() -> CallRecordsTabPage,
): CallRecordsUiState {
    val current = pages[tab] ?: CallRecordsTabPage()
    return copy(pages = pages + (tab to current.transform()))
}
