package com.example.demoproject.product.home

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.model.GiftFromType
import com.example.demoproject.platform.data.model.OnlinePresence
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.product.profile.ProfileGiftUi
import com.example.demoproject.ui.designsystem.gift.GiftSvgaPreloader
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
    /** After LoadMore succeeds, advance video-show to the next user with a show. */
    private var pendingVideoShowNext: Boolean = false

    init {
        viewModelScope.launch {
            runtime.accountBalanceStore.coins.collect { coins ->
                _uiState.update { it.copy(coinBalance = coins) }
            }
        }
        viewModelScope.launch {
            runtime.callFreeMinStore.callFreeMin.collect { callFreeMin ->
                applyCallFreeMin(callFreeMin)
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
            is HomeIntent.OpenUserProfile -> openUserProfile(intent.user)
            is HomeIntent.OpenUserAction -> openUserAction(intent.user)
            HomeIntent.OpenCoins -> viewModelScope.launch { _effects.send(HomeEffect.OpenStore) }
            is HomeIntent.ReportUser -> {
                val user = _uiState.value.currentPage.users.firstOrNull { it.id == intent.userId }
                    ?: _uiState.value.videoShowUser?.takeIf { it.id == intent.userId }
                viewModelScope.launch {
                    if (user == null) {
                        _effects.send(HomeEffect.ShowMessage(str(R.string.home_online_report_coming_soon)))
                    } else {
                        _effects.send(
                            HomeEffect.OpenReport(
                                userId = user.profileRouteUserId,
                                age = user.age,
                                isOnline = user.presence == OnlinePresence.Online,
                            ),
                        )
                    }
                }
            }
            HomeIntent.CloseVideoShow -> closeVideoShow()
            HomeIntent.NextVideoShow -> nextVideoShow()
            HomeIntent.OpenVideoShowProfile -> openVideoShowProfile()
            HomeIntent.OpenVideoShowGift -> openVideoShowGift()
            HomeIntent.DismissVideoShowGiftSheet -> _uiState.update {
                it.copy(isGiftSheetVisible = false, isGiftSending = false)
            }
            is HomeIntent.SelectVideoShowGift -> {
                _uiState.update { it.copy(selectedGiftId = intent.giftId) }
                val url = _uiState.value.gifts
                    .firstOrNull { it.id == intent.giftId }
                    ?.svgaUrl
                GiftSvgaPreloader.prefetch(getApplication(), url)
            }
            HomeIntent.SendVideoShowGift -> sendVideoShowGift()
            HomeIntent.DismissVideoShowGiftAnimation -> _uiState.update {
                it.copy(giftAnimationUrl = null)
            }
            HomeIntent.StartVideoShowCall -> startVideoShowCall()
        }
    }


    private fun openUserProfile(user: OnlineUserUi) {
        if (user.videoShow != null) {
            pendingVideoShowNext = false
            _uiState.update {
                it.copy(
                    videoShowUserId = user.id,
                    isGiftSheetVisible = false,
                    isGiftSending = false,
                    giftAnimationUrl = null,
                )
            }
            return
        }
        viewModelScope.launch {
            _effects.send(HomeEffect.OpenProfile(user.profileRouteUserId))
        }
    }

    private fun closeVideoShow() {
        pendingVideoShowNext = false
        _uiState.update {
            it.copy(
                videoShowUserId = null,
                isGiftSheetVisible = false,
                isGiftSending = false,
                giftAnimationUrl = null,
            )
        }
    }

    private fun openVideoShowProfile() {
        val user = _uiState.value.videoShowUser ?: return
        pendingVideoShowNext = false
        _uiState.update {
            it.copy(
                videoShowUserId = null,
                isGiftSheetVisible = false,
                isGiftSending = false,
                giftAnimationUrl = null,
            )
        }
        viewModelScope.launch {
            _effects.send(HomeEffect.OpenProfile(user.profileRouteUserId))
        }
    }

    private fun openVideoShowGift() {
        val user = _uiState.value.videoShowUser ?: return
        if (user.id.isBlank()) return
        _uiState.update {
            it.copy(
                isGiftSheetVisible = true,
                isGiftCatalogLoading = it.gifts.isEmpty(),
            )
        }
        if (_uiState.value.gifts.isEmpty()) {
            loadGiftCatalog()
        }
    }

    private fun loadGiftCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGiftCatalogLoading = true) }
            when (val result = runtime.messageRepository.getGiftConfig()) {
                is AppResult.Success -> {
                    val gifts = result.data.gifts.map { gift ->
                        ProfileGiftUi(
                            id = gift.id,
                            title = gift.title,
                            price = gift.price,
                            iconUrl = gift.iconUrl,
                            svgaUrl = gift.svgaUrl,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isGiftCatalogLoading = false,
                            gifts = gifts,
                            selectedGiftId = it.selectedGiftId ?: gifts.firstOrNull()?.id,
                        )
                    }
                    val selectedId = _uiState.value.selectedGiftId
                    val url = gifts.firstOrNull { it.id == selectedId }?.svgaUrl
                    GiftSvgaPreloader.prefetch(getApplication(), url)
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isGiftCatalogLoading = false) }
                    _effects.send(HomeEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun sendVideoShowGift() {
        val state = _uiState.value
        val user = state.videoShowUser ?: return
        val giftId = state.selectedGiftId ?: return
        val selected = state.gifts.firstOrNull { it.id == giftId } ?: return
        if (user.id.isBlank() || state.isGiftSending) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGiftSending = true) }
            GiftSvgaPreloader.prefetch(getApplication(), selected.svgaUrl)
            when (
                val result = runtime.messageRepository.sendGift(
                    conversationId = user.id,
                    giftId = giftId,
                    fromType = GiftFromType.CHAT,
                )
            ) {
                is AppResult.Success -> {
                    val animationUrl = selected.svgaUrl
                        .takeIf { it.isNotBlank() }
                        ?: result.data.gift?.svgaUrl?.takeIf { it.isNotBlank() }
                    _uiState.update {
                        it.copy(
                            isGiftSending = false,
                            isGiftSheetVisible = false,
                            giftAnimationUrl = animationUrl,
                        )
                    }
                    if (animationUrl == null) {
                        _effects.send(
                            HomeEffect.ShowMessage(str(R.string.home_video_show_gift_send_success)),
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isGiftSending = false) }
                    _effects.send(HomeEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun startVideoShowCall() {
        val user = _uiState.value.videoShowUser ?: return
        val show = user.videoShow
        viewModelScope.launch {
            _effects.send(
                HomeEffect.StartVideoCall(
                    userId = user.id,
                    nickname = user.nickname,
                    avatarUrl = user.avatarUrl.orEmpty(),
                    age = user.age,
                    videoUrl = show?.videoUrl.orEmpty(),
                    coverUrl = show?.coverUrl.orEmpty(),
                ),
            )
        }
    }

    private fun nextVideoShow() {
        // Keep gift sheet closed when switching users.
        _uiState.update {
            it.copy(isGiftSheetVisible = false, isGiftSending = false, giftAnimationUrl = null)
        }
        val state = _uiState.value
        val currentId = state.videoShowUserId ?: return
        val shows = state.videoShowUsers
        if (shows.isEmpty()) {
            closeVideoShow()
            return
        }
        val index = shows.indexOfFirst { it.id == currentId }
        if (index >= 0 && index < shows.lastIndex) {
            pendingVideoShowNext = false
            _uiState.update { it.copy(videoShowUserId = shows[index + 1].id) }
            return
        }
        val page = state.currentPage
        if (page.hasMore && !page.isLoadingMore && !page.isLoading && !page.isRefreshing) {
            pendingVideoShowNext = true
            loadMore()
            return
        }
        val first = shows.firstOrNull() ?: return
        if (first.id != currentId) {
            pendingVideoShowNext = false
            _uiState.update { it.copy(videoShowUserId = first.id) }
        }
    }

    private fun selectFilter(filter: OnlineFilter) {
        if (filter != _uiState.value.selectedFilter) {
            pendingVideoShowNext = false
            _uiState.update {
                it.copy(
                    selectedFilter = filter,
                    videoShowUserId = null,
                    isGiftSheetVisible = false,
                    isGiftSending = false,
                    giftAnimationUrl = null,
                )
            }
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
                    // Refresh closes video show if the current user disappeared.
                    reconcileVideoShowAfterListChange(filter)
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
                    if (pendingVideoShowNext) {
                        pendingVideoShowNext = false
                        advanceVideoShowAfterLoadMore()
                    }
                }
                is AppResult.Failure -> {
                    pendingVideoShowNext = false
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

    private fun advanceVideoShowAfterLoadMore() {
        val state = _uiState.value
        val currentId = state.videoShowUserId ?: return
        val shows = state.videoShowUsers
        val index = shows.indexOfFirst { it.id == currentId }
        if (index >= 0 && index < shows.lastIndex) {
            _uiState.update { it.copy(videoShowUserId = shows[index + 1].id) }
            return
        }
        // Still at end after load — wrap if possible.
        val first = shows.firstOrNull() ?: return
        if (first.id != currentId) {
            _uiState.update { it.copy(videoShowUserId = first.id) }
        }
    }

    private fun reconcileVideoShowAfterListChange(filter: OnlineFilter) {
        val state = _uiState.value
        if (state.selectedFilter != filter) return
        val currentId = state.videoShowUserId ?: return
        if (state.videoShowUsers.none { it.id == currentId }) {
            _uiState.update { it.copy(videoShowUserId = null) }
        }
    }

    private suspend fun refreshSessionMeta() {
        when (val open = runtime.appSessionRepository.openApp(hasProxy = false, hasVpn = false)) {
            is AppResult.Success -> {
                val data = open.data
                if (data != null) {
                    runtime.accountBalanceStore.update(data.accountMoney)
                    runtime.matchQuotaStore.update(matchFreeCount = data.matchFreeCount)
                    runtime.callFreeMinStore.update(data.callFreeMin)
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
                        avatarUrl = user.avatarUrl.orEmpty(),
                        age = user.age,
                        videoUrl = user.videoShow?.videoUrl.orEmpty(),
                        coverUrl = user.videoShow?.coverUrl.orEmpty(),
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
