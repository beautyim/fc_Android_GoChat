package com.example.demoproject.product.call

import com.example.demoproject.platform.data.repository.CallRepository

data class CallRecordsUiState(
    val coinBalance: Int = 0,
    val selectedTab: CallRecordsTab = CallRecordsTab.All,
    val pages: Map<CallRecordsTab, CallRecordsTabPage> = CallRecordsTab.entries.associateWith {
        CallRecordsTabPage()
    },
) {
    val currentPage: CallRecordsTabPage
        get() = pages[selectedTab] ?: CallRecordsTabPage()
}

data class CallRecordsTabPage(
    val records: List<CallRecordUi> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val hasLoaded: Boolean = false,
)

data class CallRecordUi(
    val id: Long,
    val peerId: String,
    val peerExternalUserId: String,
    val nickname: String,
    val age: Int,
    val avatarUrl: String?,
    val statusText: String,
    val tone: CallRecordTone,
) {
    val profileRouteUserId: String
        get() = peerExternalUserId.ifBlank { peerId }

    val title: String
        get() = if (age > 0) "$nickname, $age" else nickname
}

enum class CallRecordTone {
    Negative,
    Positive,
}

enum class CallRecordsTab(
    val apiType: Int,
) {
    All(CallRepository.CALL_RECORD_TYPE_ALL),
    Unanswered(CallRepository.CALL_RECORD_TYPE_MISSED),
    Matches(CallRepository.CALL_RECORD_TYPE_MATCH),
}

sealed interface CallRecordsIntent {
    data object Refresh : CallRecordsIntent
    data object LoadMore : CallRecordsIntent
    data class SelectTab(val tab: CallRecordsTab) : CallRecordsIntent
    data class OpenProfile(val record: CallRecordUi) : CallRecordsIntent
    data class StartCall(val record: CallRecordUi) : CallRecordsIntent
    data object OpenCoins : CallRecordsIntent
    data object StartVideoChat : CallRecordsIntent
}

sealed interface CallRecordsEffect {
    data class OpenProfile(val externalUserId: String) : CallRecordsEffect
    data class StartVideoCall(val userId: String, val nickname: String) : CallRecordsEffect
    data object OpenStore : CallRecordsEffect
    data object OpenMatch : CallRecordsEffect
    data class ShowMessage(val message: String) : CallRecordsEffect
}
