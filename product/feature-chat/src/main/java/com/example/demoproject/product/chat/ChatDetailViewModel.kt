package com.example.demoproject.product.chat

import android.app.Activity
import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.analytics.AnalyticsEvent
import com.example.demoproject.platform.analytics.AnalyticsHolder
import com.example.demoproject.platform.data.billing.StorePurchaseLauncherHolder
import com.example.demoproject.platform.data.model.AlbumPhoto
import com.example.demoproject.platform.data.model.GiftFromType
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.model.MessageStatus
import com.example.demoproject.platform.data.model.MessageType
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.data.model.giftMessageContent
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.network.mapper.toRechargePageDataOrNull
import com.example.demoproject.platform.data.network.mapper.toVipGuidePageDataOrNull
import com.example.demoproject.platform.data.repository.BillingPaymentType
import com.example.demoproject.platform.data.repository.BillingProductType
import com.example.demoproject.platform.data.repository.RechargePageData
import com.example.demoproject.platform.data.repository.StorePurchaseRequest
import com.example.demoproject.platform.data.repository.StorePurchaseResult
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.isInsufficientBalance
import com.example.demoproject.platform.network.result.isMsgSendRequireVip
import com.example.demoproject.product.store.CoinPayGuideUiState
import com.example.demoproject.product.store.R as StoreR
import com.example.demoproject.product.store.toCoinPayGuideUiState
import com.example.demoproject.product.vip.R as VipR
import com.example.demoproject.product.vip.VipPayGuideUiState
import com.example.demoproject.product.vip.toPayGuideUiState
import com.example.demoproject.ui.designsystem.gift.GiftSvgaPreloader
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class ChatDetailViewModel(
    application: Application,
    private val conversationId: String,
    initialNickname: String,
) : AndroidViewModel(application) {
    private val runtime = NetworkRuntime.get(application)
    private val messageOrder = ChatDetailMessageOrder()
    private val _uiState = MutableStateFlow(
        ChatDetailUiState(
            conversationId = conversationId,
            currentUserId = runtime.sessionManager.currentUserId.orEmpty(),
            nickname = initialNickname,
        ),
    )
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ChatDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var observeJob: Job? = null
    private var loadMoreJob: Job? = null
    private var followFlashJob: Job? = null
    private var giftCatalogJob: Job? = null
    private var giftAssets: ChatGiftAssetIndex = ChatGiftAssetIndex.Empty
    private var giftAssetPrefetchStarted = false
    private var introResolved = false
    private var greetingSendInFlight = false
    private var greetingAutoHideJob: Job? = null
    private var cachedMessages: List<Message> = emptyList()
    private var profilePhotos: List<String> = emptyList()
    private var extraPhotoCount: Int = 0

    @Volatile
    private var hostActivity: Activity? = null

    private val strings = object : ChatDetailStringResolver {
        override val callMissed: String get() = str(R.string.chat_detail_call_missed)
        override val callDeclined: String get() = str(R.string.chat_detail_call_declined)
        override val callCanceled: String get() = str(R.string.chat_detail_call_canceled)
        override val giftFallback: String get() = str(R.string.chat_detail_gift_fallback)
        override val voiceFallback: String get() = str(R.string.chat_preview_voice)
        override fun callDuration(clock: String): String =
            str(R.string.chat_detail_call_duration, clock)
        override fun todaySeparator(time: String): String =
            str(R.string.chat_detail_today_separator, time)
        override fun controlNotice(key: Int?): String = when (key) {
            101 -> str(R.string.chat_detail_notice_blocked_by_me)
            102 -> str(R.string.chat_detail_notice_blocked_by_peer)
            103 -> str(R.string.chat_detail_notice_deleted_by_peer)
            104 -> str(R.string.chat_detail_notice_self_banned)
            105 -> str(R.string.chat_detail_notice_peer_banned)
            106 -> str(R.string.chat_detail_notice_forbidden)
            107 -> str(R.string.chat_detail_send_limit)
            108 -> str(R.string.chat_detail_send_limit_lifted)
            else -> str(R.string.chat_detail_notice_generic)
        }
        override fun freeMessagesLeft(count: Int): String =
            str(R.string.chat_detail_free_messages_left, count)
        override fun giftSentLabel(): String = str(R.string.chat_detail_gift_sent)
        override fun giftRequestLabel(name: String): String =
            str(R.string.chat_detail_gift_request, name)
        override fun sendGiftAction(): String = str(R.string.chat_detail_send_gift)
        override fun onlineLabel(): String = str(R.string.chat_detail_online)
        override fun extraPhotos(count: Int): String =
            str(R.string.chat_detail_extra_photos, count)
    }

    init {
        AnalyticsHolder.tracker?.track(
            AnalyticsEvent.FirstDialog(userId = runtime.sessionManager.currentUserId.orEmpty()),
        )
        viewModelScope.launch {
            runtime.accountBalanceStore.coins.collect { coins ->
                _uiState.update {
                    it.copy(
                        coinBalance = coins,
                        coinPayGuide = it.coinPayGuide?.copy(balance = coins),
                    )
                }
            }
        }
        bootstrap()
    }

    fun bindActivity(activity: Activity?) {
        hostActivity = activity
    }

    fun onIntent(intent: ChatDetailIntent) {
        when (intent) {
            ChatDetailIntent.RetryLoad -> bootstrap()
            ChatDetailIntent.LoadMore -> loadMore()
            is ChatDetailIntent.DraftChanged -> _uiState.update { it.copy(draft = intent.value) }
            ChatDetailIntent.SendText -> sendText()
            is ChatDetailIntent.Resend -> resend(intent.messageId)
            is ChatDetailIntent.Translate -> translate(intent.messageId)
            ChatDetailIntent.Follow -> followPeer()
            ChatDetailIntent.OpenPeerProfile -> viewModelScope.launch {
                val id = _uiState.value.peerExternalUserId.ifBlank { _uiState.value.peerId }
                if (id.isNotBlank()) _effects.send(ChatDetailEffect.OpenPeerProfile(id))
            }
            ChatDetailIntent.OpenMore -> viewModelScope.launch {
                _effects.send(ChatDetailEffect.OpenMoreMenu)
            }
            ChatDetailIntent.OpenGiftPanel,
            is ChatDetailIntent.SendRequestedGift,
            -> openGiftSheet()
            ChatDetailIntent.DismissGiftSheet -> _uiState.update {
                it.copy(isGiftSheetVisible = false, isGiftSending = false)
            }
            is ChatDetailIntent.SelectGift -> {
                _uiState.update { it.copy(selectedGiftId = intent.giftId) }
                // Warm the pick so its bubble animation is ready right after sending.
                val selected = _uiState.value.gifts.firstOrNull { it.id == intent.giftId }
                GiftSvgaPreloader.prefetch(getApplication(), selected?.svgaUrl)
            }
            ChatDetailIntent.SendGift -> sendSelectedGift()
            is ChatDetailIntent.SendQuickGift -> sendQuickGift(intent.giftId)
            ChatDetailIntent.SendGreeting -> sendGreeting()
            ChatDetailIntent.DismissGreeting -> dismissGreeting()
            ChatDetailIntent.OpenCoins -> viewModelScope.launch {
                _effects.send(ChatDetailEffect.OpenStore)
            }
            ChatDetailIntent.OpenPlusMenu -> _uiState.update {
                it.copy(
                    isMediaTypeSheetVisible = true,
                    isEmojiSheetVisible = false,
                    isGiftSheetVisible = false,
                )
            }
            ChatDetailIntent.DismissMediaTypeSheet -> _uiState.update {
                it.copy(isMediaTypeSheetVisible = false)
            }
            ChatDetailIntent.SelectSendImage -> viewModelScope.launch {
                _uiState.update { it.copy(isMediaTypeSheetVisible = false) }
                _effects.send(ChatDetailEffect.PickSendImage)
            }
            ChatDetailIntent.SelectSendVideo -> viewModelScope.launch {
                _uiState.update { it.copy(isMediaTypeSheetVisible = false) }
                _effects.send(ChatDetailEffect.PickSendVideo)
            }
            is ChatDetailIntent.SendPickedImage -> sendImage(intent.localUri)
            is ChatDetailIntent.SendPickedVideo -> sendVideo(intent.localUri)
            ChatDetailIntent.OpenEmoji -> _uiState.update {
                it.copy(
                    isEmojiSheetVisible = true,
                    isGiftSheetVisible = false,
                    isMediaTypeSheetVisible = false,
                )
            }
            ChatDetailIntent.DismissEmojiSheet -> _uiState.update {
                it.copy(isEmojiSheetVisible = false)
            }
            is ChatDetailIntent.InsertEmoji -> _uiState.update {
                it.copy(draft = it.draft + intent.emoji)
            }
            ChatDetailIntent.StartVideoCall -> viewModelScope.launch {
                val peer = _uiState.value.peerId.ifBlank { conversationId }
                _effects.send(ChatDetailEffect.StartVideoCall(peer))
            }
            is ChatDetailIntent.OpenMedia -> openMediaPreview(intent.messageId)
            ChatDetailIntent.DismissMediaPreview -> _uiState.update {
                it.copy(mediaViewerItems = emptyList(), mediaViewerIndex = null)
            }
            is ChatDetailIntent.PlayGiftAnimation -> playGiftAnimation(intent.messageId)
            ChatDetailIntent.DismissGiftAnimation -> _uiState.update {
                it.copy(giftAnimationUrl = null)
            }
            ChatDetailIntent.DismissVipPayGuide -> _uiState.update {
                it.copy(vipPayGuide = null)
            }
            ChatDetailIntent.PurchaseVipPayGuide -> purchaseVipPayGuide()
            ChatDetailIntent.DismissCoinPayGuide -> _uiState.update {
                it.copy(coinPayGuide = null)
            }
            is ChatDetailIntent.PurchaseCoinPayGuideCoin -> purchaseCoinPayGuide(
                offerId = intent.offerId,
                isSale = false,
            )
            is ChatDetailIntent.PurchaseCoinPayGuideSale -> purchaseCoinPayGuide(
                offerId = intent.offerId,
                isSale = true,
            )
        }
    }

    private fun bootstrap() {
        observeJob?.cancel()
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, hasLoaded = false)
            }
            runtime.messageRepository.failInterruptedSendingMessages()
            // Bind Room first so local Failed/Sending rows stay visible while sync runs.
            observeJob = viewModelScope.launch {
                var lastMarkedPeerMessageId: String? = null
                runtime.messageRepository.observeMessages(conversationId).collect { messages ->
                    cachedMessages = messages
                    publishItems()
                    _uiState.update {
                        it.copy(isLoading = false, hasLoaded = true, errorMessage = null)
                    }
                    val selfId = runtime.sessionManager.currentUserId.orEmpty()
                    val latestPeer = messages
                        .asReversed()
                        .firstOrNull { it.senderId.isNotBlank() && it.senderId != selfId }
                    if (latestPeer != null && latestPeer.id != lastMarkedPeerMessageId) {
                        lastMarkedPeerMessageId = latestPeer.id
                        // Stay authoritative with backend while this chat is open (MQTT may
                        // have refreshed unread via /msg/get-unread).
                        runtime.messageRepository.markConversationRead(conversationId)
                    }
                }
            }
            when (val detail = runtime.messageRepository.getConversationDetail(conversationId)) {
                is AppResult.Success -> applyDetail(detail.data.conversation.peer, detail.data)
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = detail.message,
                            hasLoaded = true,
                        )
                    }
                }
            }
            launch {
                when (val me = runtime.profileRepository.getMyProfile()) {
                    is AppResult.Success -> _uiState.update { it.copy(selfAvatarUrl = me.data.avatar) }
                    is AppResult.Failure -> Unit
                }
            }
            launch { loadAlbumPhotos() }
            when (val sync = runtime.messageRepository.syncLatestMessages(conversationId)) {
                is AppResult.Success -> _uiState.update { it.copy(hasMore = sync.data) }
                is AppResult.Failure -> _uiState.update {
                    it.copy(errorMessage = sync.message, hasMore = true)
                }
            }
            runtime.messageRepository.markConversationRead(conversationId)
        }
    }

    private fun applyDetail(
        peer: User,
        detail: com.example.demoproject.platform.data.model.ConversationDetail,
    ) {
        _uiState.update {
            it.copy(
                peerId = peer.id,
                peerExternalUserId = peer.externalUserId.ifBlank { peer.id },
                nickname = peer.nickname.ifBlank { it.nickname },
                age = peer.age,
                peerAvatarUrl = peer.avatar,
                isOnline = peer.isOnline || peer.onlineStatusCode > 0,
                countryFlag = countryCodeToFlagEmoji(peer.countryCode),
                countryName = peer.countryName.orEmpty(),
                peerHasReplied = detail.peerHasReplied,
                isFollowing = detail.friendStatus == 1 || peer.isFollowing,
                freeMessageCount = detail.freeMessageCount,
                unlockFromType = detail.unlockFromType,
                currentUserId = runtime.sessionManager.currentUserId.orEmpty(),
            )
        }
        maybeShowFirstVisitIntro(
            peerKey = peer.id.ifBlank { peer.externalUserId }.ifBlank { conversationId },
        )
    }

    /**
     * First open of this peer's chat detail shows the gift quick bar + waving greeting once.
     * Marked seen immediately so a re-entry without interaction does not show them again.
     */
    private fun maybeShowFirstVisitIntro(peerKey: String) {
        if (introResolved || peerKey.isBlank()) return
        introResolved = true
        viewModelScope.launch {
            if (runtime.appPrefs.hasSeenChatDetailIntro(peerKey)) return@launch
            runtime.appPrefs.markChatDetailIntroSeen(peerKey)
            _uiState.update {
                it.copy(showGiftQuickBar = true, showGreetingGesture = true)
            }
            scheduleGreetingAutoHide()
            loadGiftCatalog(notifyFailure = false)
        }
    }

    private fun scheduleGreetingAutoHide() {
        greetingAutoHideJob?.cancel()
        greetingAutoHideJob = viewModelScope.launch {
            delay(GREETING_AUTO_HIDE_MS)
            dismissGreeting()
        }
    }

    private fun dismissGreeting() {
        greetingAutoHideJob?.cancel()
        greetingAutoHideJob = null
        if (!_uiState.value.showGreetingGesture) return
        _uiState.update { it.copy(showGreetingGesture = false) }
    }

    private suspend fun loadAlbumPhotos() {
        val peerId = _uiState.value.peerId.ifBlank { conversationId }
        when (
            val result = runtime.profileRepository.getAlbumPhotos(
                targetUserId = peerId,
                page = 1,
                pageCount = 8,
            )
        ) {
            is AppResult.Success -> {
                val urls = result.data.mapNotNull(AlbumPhoto::displayUrl)
                profilePhotos = urls.take(4)
                extraPhotoCount = (urls.size - 4).coerceAtLeast(0)
                publishItems()
            }
            is AppResult.Failure -> Unit
        }
    }

    private fun loadMore() {
        val state = _uiState.value
        if (!state.hasMore || state.isLoadingMore || loadMoreJob?.isActive == true) return
        loadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = runtime.messageRepository.loadMoreMessages(conversationId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(hasMore = result.data, isLoadingMore = false)
                    }
                    // Server pages ~[ComponentSize.chatDetailPageSize] messages per cursor hop.
                    publishItems()
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoadingMore = false) }
                    _effects.send(ChatDetailEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun sendText() {
        val text = _uiState.value.draft.trim()
        if (text.isBlank() || _uiState.value.isSending) return
        // Clear the composer immediately so the caret resets with an empty field.
        _uiState.update { it.copy(draft = "", isSending = true) }
        viewModelScope.launch {
            // Cap sync at pre-send latest so tips near the outbound mtime are not skipped
            // when Success advances the success cursor past them.
            val syncCeiling = latestSuccessCreatedAt()
            when (
                val result = runtime.messageRepository.sendTextMessage(
                    conversationId = conversationId,
                    text = text,
                )
            ) {
                is AppResult.Success -> {
                    val free = _uiState.value.freeMessageCount
                    if (free > 0) {
                        _uiState.update { it.copy(freeMessageCount = (free - 1).coerceAtLeast(0)) }
                    }
                    runtime.messageRepository.syncLatestMessages(
                        conversationId = conversationId,
                        latestMtimeCeiling = syncCeiling,
                    )
                }
                // Failure: keep draft cleared; failed bubble + resend carry the text. No toast.
                // Limit/VIP tips (e.g. key=107) are written with the BizError — pull twice in
                // case the tip row lags the HTTP error by a beat.
                is AppResult.Failure -> {
                    maybeShowVipPayGuide(result)
                    maybeShowCoinPayGuide(result)
                    runtime.messageRepository.syncLatestMessages(
                        conversationId = conversationId,
                        latestMtimeCeiling = syncCeiling,
                    )
                    delay(POST_SEND_TIP_SYNC_RETRY_MS)
                    runtime.messageRepository.syncLatestMessages(
                        conversationId = conversationId,
                        latestMtimeCeiling = syncCeiling,
                    )
                }
            }
            _uiState.update { it.copy(isSending = false) }
        }
    }

    private fun sendImage(localUri: String) {
        val uri = localUri.trim()
        if (uri.isEmpty() || _uiState.value.isSending) return
        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            val syncCeiling = latestSuccessCreatedAt()
            when (
                val result = runtime.messageRepository.sendImageMessage(
                    conversationId = conversationId,
                    localUriOrPath = uri,
                )
            ) {
                is AppResult.Success -> {
                    val free = _uiState.value.freeMessageCount
                    if (free > 0) {
                        _uiState.update { it.copy(freeMessageCount = (free - 1).coerceAtLeast(0)) }
                    }
                    runtime.messageRepository.syncLatestMessages(
                        conversationId = conversationId,
                        latestMtimeCeiling = syncCeiling,
                    )
                }
                is AppResult.Failure -> {
                    maybeShowVipPayGuide(result)
                    maybeShowCoinPayGuide(result)
                    runtime.messageRepository.syncLatestMessages(
                        conversationId = conversationId,
                        latestMtimeCeiling = syncCeiling,
                    )
                    delay(POST_SEND_TIP_SYNC_RETRY_MS)
                    runtime.messageRepository.syncLatestMessages(
                        conversationId = conversationId,
                        latestMtimeCeiling = syncCeiling,
                    )
                }
            }
            _uiState.update { it.copy(isSending = false) }
        }
    }

    private fun sendVideo(localUri: String) {
        val uri = localUri.trim()
        if (uri.isEmpty() || _uiState.value.isSending) return
        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            val syncCeiling = latestSuccessCreatedAt()
            when (
                val result = runtime.messageRepository.sendVideoMessage(
                    conversationId = conversationId,
                    localUriOrPath = uri,
                )
            ) {
                is AppResult.Success -> {
                    val free = _uiState.value.freeMessageCount
                    if (free > 0) {
                        _uiState.update { it.copy(freeMessageCount = (free - 1).coerceAtLeast(0)) }
                    }
                    runtime.messageRepository.syncLatestMessages(
                        conversationId = conversationId,
                        latestMtimeCeiling = syncCeiling,
                    )
                }
                is AppResult.Failure -> {
                    maybeShowVipPayGuide(result)
                    maybeShowCoinPayGuide(result)
                    runtime.messageRepository.syncLatestMessages(
                        conversationId = conversationId,
                        latestMtimeCeiling = syncCeiling,
                    )
                    delay(POST_SEND_TIP_SYNC_RETRY_MS)
                    runtime.messageRepository.syncLatestMessages(
                        conversationId = conversationId,
                        latestMtimeCeiling = syncCeiling,
                    )
                }
            }
            _uiState.update { it.copy(isSending = false) }
        }
    }

    private fun maybeShowVipPayGuide(failure: AppResult.Failure) {
        if (!failure.isMsgSendRequireVip()) return
        val nickname = _uiState.value.nickname
        // Open the sheet immediately with a skeleton so the ModalBottomSheet entrance
        // is not an empty flash while the callback payload is mapped.
        _uiState.update {
            it.copy(
                vipPayGuide = VipPayGuideUiState(
                    isLoading = true,
                    peerNickname = nickname,
                ),
            )
        }
        viewModelScope.launch {
            yield()
            val biz = failure as? AppResult.BizError
            val guide = biz?.callback.toVipGuidePageDataOrNull()
            val ui = guide?.toPayGuideUiState(
                context = getApplication(),
                fallbackNickname = nickname,
            )
            if (ui?.plan == null) {
                _uiState.update { state ->
                    if (state.vipPayGuide?.isLoading == true) {
                        state.copy(vipPayGuide = null)
                    } else {
                        state
                    }
                }
                return@launch
            }
            _uiState.update { it.copy(vipPayGuide = ui.copy(isLoading = false)) }
        }
    }

    private fun purchaseVipPayGuide() {
        val guide = _uiState.value.vipPayGuide ?: return
        if (guide.isPurchasing) return
        val plan = guide.plan ?: return
        val activity = hostActivity
        if (activity == null) {
            viewModelScope.launch {
                _effects.send(
                    ChatDetailEffect.ShowMessage(
                        strVip(VipR.string.vip_status_no_activity),
                    ),
                )
            }
            return
        }
        val launcher = StorePurchaseLauncherHolder.launcher
        if (launcher == null) {
            viewModelScope.launch {
                _effects.send(
                    ChatDetailEffect.ShowMessage(
                        strVip(VipR.string.vip_status_launcher_missing),
                    ),
                )
            }
            return
        }
        val request = StorePurchaseRequest(
            uiId = "vip-guide-${plan.id}",
            goodsId = plan.id,
            productId = plan.sku,
            productType = BillingProductType.Vip,
            paymentType = BillingPaymentType.GooglePlay,
            fromType = guide.fromType,
        )
        viewModelScope.launch {
            _uiState.update {
                it.copy(vipPayGuide = it.vipPayGuide?.copy(isPurchasing = true))
            }
            launcher.launch(activity, request) { result ->
                when (result) {
                    is StorePurchaseResult.Success -> {
                        _uiState.update { it.copy(vipPayGuide = null) }
                        viewModelScope.launch {
                            _effects.send(
                                ChatDetailEffect.ShowMessage(
                                    strVip(VipR.string.vip_status_purchase_verified),
                                ),
                            )
                        }
                    }
                    is StorePurchaseResult.Canceled -> {
                        _uiState.update {
                            it.copy(vipPayGuide = it.vipPayGuide?.copy(isPurchasing = false))
                        }
                        viewModelScope.launch {
                            _effects.send(
                                ChatDetailEffect.ShowMessage(
                                    strVip(VipR.string.vip_status_purchase_canceled),
                                ),
                            )
                        }
                    }
                    is StorePurchaseResult.Failed -> {
                        _uiState.update {
                            it.copy(vipPayGuide = it.vipPayGuide?.copy(isPurchasing = false))
                        }
                        viewModelScope.launch {
                            _effects.send(
                                ChatDetailEffect.ShowMessage(
                                    strVip(
                                        VipR.string.vip_status_purchase_failed_fmt,
                                        result.message,
                                    ),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun maybeShowCoinPayGuide(failure: AppResult.Failure) {
        if (!failure.isInsufficientBalance()) return
        val balance = _uiState.value.coinBalance
        _uiState.update {
            it.copy(
                coinPayGuide = CoinPayGuideUiState(
                    isLoading = true,
                    balance = balance,
                ),
            )
        }
        viewModelScope.launch {
            yield()
            val biz = failure as? AppResult.BizError
            val fromCallback = biz?.callback.toRechargePageDataOrNull()
            val page = resolveCoinPayGuidePage(fromCallback)
            val fallbackLabel = strStore(StoreR.string.store_super_discount)
            val ui = page?.toCoinPayGuideUiState(
                fallbackSuperDiscountLabel = fallbackLabel,
                fromType = biz?.fromType,
            )
            if (ui == null || ui.isCatalogEmpty) {
                _uiState.update { state ->
                    if (state.coinPayGuide?.isLoading == true) {
                        state.copy(coinPayGuide = null)
                    } else {
                        state
                    }
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    coinPayGuide = ui.copy(
                        isLoading = false,
                        balance = if (ui.balance > 0) ui.balance else balance,
                    ),
                )
            }
        }
    }

    private suspend fun resolveCoinPayGuidePage(
        fromCallback: RechargePageData?,
    ): RechargePageData? {
        if (fromCallback != null && !fromCallback.isEmpty) {
            // Alert payloads can be partial; fill missing carousel/grid from the full index.
            return when (val full = runtime.coinRepository.getRechargePage()) {
                is AppResult.Success -> fromCallback.withCarouselFrom(full.data)
                is AppResult.Failure -> fromCallback
            }
        }
        return when (val full = runtime.coinRepository.getRechargePage()) {
            is AppResult.Success -> full.data.takeUnless { it.isEmpty }
            is AppResult.Failure -> null
        }
    }

    private fun purchaseCoinPayGuide(offerId: Long, isSale: Boolean) {
        val guide = _uiState.value.coinPayGuide ?: return
        if (guide.purchasingOfferId != null) return
        val sale = guide.saleOffers.firstOrNull { it.id == offerId }
        val coin = guide.coinOffers.firstOrNull { it.id == offerId }
        val sku: String
        val goodsId: Long
        when {
            isSale && sale != null -> {
                sku = sale.sku
                goodsId = sale.id
            }
            !isSale && coin != null -> {
                sku = coin.sku
                goodsId = coin.id
            }
            else -> return
        }
        val activity = hostActivity
        if (activity == null) {
            viewModelScope.launch {
                _effects.send(
                    ChatDetailEffect.ShowMessage(strStore(StoreR.string.store_status_no_activity)),
                )
            }
            return
        }
        val launcher = StorePurchaseLauncherHolder.launcher
        if (launcher == null) {
            viewModelScope.launch {
                _effects.send(
                    ChatDetailEffect.ShowMessage(
                        strStore(StoreR.string.store_status_launcher_missing),
                    ),
                )
            }
            return
        }
        val request = StorePurchaseRequest(
            uiId = "coin-guide-$goodsId",
            goodsId = goodsId,
            productId = sku,
            productType = BillingProductType.Coins,
            paymentType = BillingPaymentType.GooglePlay,
            fromType = guide.fromType ?: CHAT_UPSELL_FROM_TYPE,
        )
        viewModelScope.launch {
            _uiState.update {
                it.copy(coinPayGuide = it.coinPayGuide?.copy(purchasingOfferId = offerId))
            }
            launcher.launch(activity, request) { result ->
                when (result) {
                    is StorePurchaseResult.Success -> {
                        _uiState.update { it.copy(coinPayGuide = null) }
                        viewModelScope.launch {
                            _effects.send(
                                ChatDetailEffect.ShowMessage(
                                    strStore(StoreR.string.store_status_purchase_verified),
                                ),
                            )
                        }
                    }
                    is StorePurchaseResult.Canceled -> {
                        _uiState.update {
                            it.copy(
                                coinPayGuide = it.coinPayGuide?.copy(purchasingOfferId = null),
                            )
                        }
                        viewModelScope.launch {
                            _effects.send(
                                ChatDetailEffect.ShowMessage(
                                    strStore(StoreR.string.store_status_purchase_canceled),
                                ),
                            )
                        }
                    }
                    is StorePurchaseResult.Failed -> {
                        _uiState.update {
                            it.copy(
                                coinPayGuide = it.coinPayGuide?.copy(purchasingOfferId = null),
                            )
                        }
                        viewModelScope.launch {
                            _effects.send(
                                ChatDetailEffect.ShowMessage(
                                    strStore(
                                        StoreR.string.store_status_purchase_failed_fmt,
                                        result.message,
                                    ),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun strVip(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private fun strStore(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private fun openGiftSheet() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                isGiftSheetVisible = true,
                isEmojiSheetVisible = false,
                isMediaTypeSheetVisible = false,
                isGiftCatalogLoading = it.gifts.isEmpty(),
            )
        }
        if (state.gifts.isEmpty()) {
            loadGiftCatalog(notifyFailure = true)
        }
    }

    /**
     * Gift bubbles that arrive without `icon` / `svga_url` can only be illustrated and animated
     * from the `gift/config` catalog, so pull it in the background once such a row shows up.
     */
    private fun ensureGiftAssetsForBubbles(messages: List<Message>) {
        // One attempt per screen: a channel without gift assets must not re-request on
        // every Room emission.
        if (giftAssetPrefetchStarted || _uiState.value.gifts.isNotEmpty()) return
        val needsCatalogAsset = messages.any { message ->
            val gift = message.giftMessageContent() ?: return@any false
            gift.iconUrl.isBlank() || gift.animationUrl.isBlank()
        }
        if (!needsCatalogAsset) return
        giftAssetPrefetchStarted = true
        loadGiftCatalog(notifyFailure = false)
    }

    private fun playGiftAnimation(messageId: String) {
        val message = cachedMessages.firstOrNull { it.id == messageId } ?: return
        val url = message.giftAnimationUrl(giftAssets)
        if (url.isBlank()) return
        _uiState.update { it.copy(giftAnimationUrl = url) }
    }

    /** Opens the shared profile-style media viewer on the tapped bubble. */
    private fun openMediaPreview(messageId: String) {
        val mediaRows = _uiState.value.items
            .asSequence()
            .filterIsInstance<ChatDetailListItem.MessageRow>()
            .mapNotNull { row ->
                val item = row.message.toMediaViewerItem() ?: return@mapNotNull null
                row.message.id to item
            }
            .toList()
        val index = mediaRows.indexOfFirst { it.first == messageId }
        if (index < 0) return
        _uiState.update {
            it.copy(
                mediaViewerItems = mediaRows.map { pair -> pair.second },
                mediaViewerIndex = index,
            )
        }
    }

    /**
     * SVGA archives are large enough that decoding them on tap leaves the overlay waiting, so
     * warm the newest gift bubbles — those are the ones a user replays.
     */
    private fun prefetchGiftAnimations(messages: List<Message>) {
        messages.asReversed()
            .asSequence()
            .mapNotNull { it.giftAnimationUrl(giftAssets).takeIf(String::isNotBlank) }
            .distinct()
            .take(GIFT_ANIMATION_PREFETCH_COUNT)
            .forEach { GiftSvgaPreloader.prefetch(getApplication(), it) }
    }

    private fun loadGiftCatalog(notifyFailure: Boolean) {
        if (giftCatalogJob?.isActive == true) return
        giftCatalogJob = viewModelScope.launch {
            _uiState.update { it.copy(isGiftCatalogLoading = true) }
            when (val result = runtime.messageRepository.getGiftConfig()) {
                is AppResult.Success -> {
                    val gifts = result.data.gifts.map { gift ->
                        ChatGiftUi(
                            id = gift.id,
                            title = gift.title,
                            price = gift.price,
                            iconUrl = gift.iconUrl,
                            svgaUrl = gift.svgaUrl,
                        )
                    }
                    giftAssets = ChatGiftAssetIndex.from(gifts)
                    _uiState.update {
                        it.copy(
                            isGiftCatalogLoading = false,
                            gifts = gifts,
                            selectedGiftId = it.selectedGiftId ?: gifts.firstOrNull()?.id,
                        )
                    }
                    publishItems()
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isGiftCatalogLoading = false) }
                    if (notifyFailure) {
                        _effects.send(ChatDetailEffect.ShowMessage(result.message))
                    }
                }
            }
        }
    }

    private fun sendSelectedGift() {
        val state = _uiState.value
        val giftId = state.selectedGiftId ?: return
        sendGiftInternal(giftId = giftId, dismissQuickBarOnSuccess = true)
    }

    private fun sendQuickGift(giftId: Long) {
        sendGiftInternal(giftId = giftId, dismissQuickBarOnSuccess = true)
    }

    private fun sendGiftInternal(giftId: Long, dismissQuickBarOnSuccess: Boolean) {
        val state = _uiState.value
        if (state.isGiftSending || giftId <= 0L) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isGiftSending = true, selectedGiftId = giftId)
            }
            val syncCeiling = latestSuccessCreatedAt()
            when (
                val result = runtime.messageRepository.sendGift(
                    conversationId = conversationId,
                    giftId = giftId,
                    fromType = GiftFromType.CHAT,
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isGiftSending = false,
                            isGiftSheetVisible = false,
                            showGiftQuickBar = if (dismissQuickBarOnSuccess) {
                                false
                            } else {
                                it.showGiftQuickBar
                            },
                        )
                    }
                    _effects.send(
                        ChatDetailEffect.ShowMessage(str(R.string.chat_detail_gift_send_success)),
                    )
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isGiftSending = false) }
                    maybeShowCoinPayGuide(result)
                    _effects.send(ChatDetailEffect.ShowMessage(result.message))
                }
            }
            runtime.messageRepository.syncLatestMessages(
                conversationId = conversationId,
                latestMtimeCeiling = syncCeiling,
            )
        }
    }

    private fun sendGreeting() {
        // Hide immediately on tap; send continues regardless of success/failure.
        dismissGreeting()
        if (greetingSendInFlight) return
        greetingSendInFlight = true
        viewModelScope.launch {
            val greeting = str(R.string.chat_detail_greeting_text)
            val syncCeiling = latestSuccessCreatedAt()
            when (
                val result = runtime.messageRepository.sendTextMessage(
                    conversationId = conversationId,
                    text = greeting,
                )
            ) {
                is AppResult.Success -> {
                    val free = _uiState.value.freeMessageCount
                    if (free > 0) {
                        _uiState.update {
                            it.copy(freeMessageCount = (free - 1).coerceAtLeast(0))
                        }
                    }
                    runtime.messageRepository.syncLatestMessages(
                        conversationId = conversationId,
                        latestMtimeCeiling = syncCeiling,
                    )
                }
                is AppResult.Failure -> {
                    maybeShowVipPayGuide(result)
                    maybeShowCoinPayGuide(result)
                    runtime.messageRepository.syncLatestMessages(
                        conversationId = conversationId,
                        latestMtimeCeiling = syncCeiling,
                    )
                    delay(POST_SEND_TIP_SYNC_RETRY_MS)
                    runtime.messageRepository.syncLatestMessages(
                        conversationId = conversationId,
                        latestMtimeCeiling = syncCeiling,
                    )
                }
            }
            greetingSendInFlight = false
        }
    }

    private fun resend(messageId: String) {
        val message = cachedMessages.firstOrNull { it.id == messageId } ?: return
        if (message.status != MessageStatus.Failed) return
        viewModelScope.launch {
            val syncCeiling = latestSuccessCreatedAt()
            val result = when (message.type) {
                MessageType.Text -> runtime.messageRepository.sendTextMessage(
                    conversationId = conversationId,
                    text = message.content,
                    localMessageId = message.id,
                )
                MessageType.Emoji -> runtime.messageRepository.sendEmojiMessage(
                    conversationId = conversationId,
                    emoji = message.content,
                    localMessageId = message.id,
                )
                MessageType.Image -> runtime.messageRepository.sendImageMessage(
                    conversationId = conversationId,
                    localUriOrPath = message.content.resendMediaSource(preferImage = true),
                    localMessageId = message.id,
                )
                MessageType.Video -> runtime.messageRepository.sendVideoMessage(
                    conversationId = conversationId,
                    localUriOrPath = message.content.resendMediaSource(preferImage = false),
                    localMessageId = message.id,
                )
                else -> {
                    _effects.send(
                        ChatDetailEffect.ShowMessage(str(R.string.chat_detail_resend_unsupported)),
                    )
                    return@launch
                }
            }
            // Send failure stays on the bubble with resend; still pull any control tips.
            runtime.messageRepository.syncLatestMessages(
                conversationId = conversationId,
                latestMtimeCeiling = syncCeiling,
            )
            if (result is AppResult.Failure) {
                maybeShowVipPayGuide(result)
                maybeShowCoinPayGuide(result)
                delay(POST_SEND_TIP_SYNC_RETRY_MS)
                runtime.messageRepository.syncLatestMessages(
                    conversationId = conversationId,
                    latestMtimeCeiling = syncCeiling,
                )
            }
        }
    }

    private fun latestSuccessCreatedAt(): Long? =
        cachedMessages
            .asSequence()
            .filter {
                it.status == MessageStatus.Sent ||
                    it.status == MessageStatus.Delivered ||
                    it.status == MessageStatus.Read
            }
            .maxOfOrNull { it.createdAt }

    private fun translate(messageId: String) {
        if (_uiState.value.translatingMessageId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(translatingMessageId = messageId) }
            when (
                val result = runtime.messageRepository.translateMessage(
                    conversationId = conversationId,
                    messageId = messageId,
                )
            ) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> _effects.send(ChatDetailEffect.ShowMessage(result.message))
            }
            _uiState.update { it.copy(translatingMessageId = null) }
        }
    }

    private fun followPeer() {
        val state = _uiState.value
        if (state.isFollowing || state.showFollowedFlash) return
        val target = state.peerExternalUserId.ifBlank { state.peerId }
        if (target.isBlank()) return
        viewModelScope.launch {
            when (val result = runtime.profileRepository.followUser(target)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isFollowing = true, showFollowedFlash = true) }
                    followFlashJob?.cancel()
                    followFlashJob = viewModelScope.launch {
                        delay(FOLLOWED_FLASH_MS)
                        _uiState.update { it.copy(showFollowedFlash = false) }
                    }
                }
                is AppResult.Failure -> _effects.send(ChatDetailEffect.ShowMessage(result.message))
            }
        }
    }

    private fun publishItems() {
        val state = _uiState.value
        val ordered = messageOrder.ordered(cachedMessages)
        val profileCard = if (!state.hasMore) {
            ChatDetailListItem.ProfileCard(
                nickname = state.nickname,
                age = state.age,
                avatarUrl = state.peerAvatarUrl,
                countryFlag = state.countryFlag,
                countryName = state.countryName,
                photoUrls = profilePhotos,
                extraPhotoCount = extraPhotoCount,
            )
        } else {
            null
        }
        val items = ChatDetailMessageMapper.buildListItems(
            ordered = ordered,
            currentUserId = state.currentUserId,
            peerName = state.nickname,
            hasMore = state.hasMore,
            profileCard = profileCard,
            stringResolver = strings,
            giftAssets = giftAssets,
        )
        _uiState.update { it.copy(items = items) }
        ensureGiftAssetsForBubbles(ordered)
        prefetchGiftAnimations(ordered)
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private companion object {
        const val FOLLOWED_FLASH_MS = 2_000L
        const val POST_SEND_TIP_SYNC_RETRY_MS = 400L
        /** Auto-hide waving greeting if unused after first-visit show. */
        const val GREETING_AUTO_HIDE_MS = 5_000L

        /** Newest gift bubbles kept warm; bounded so history scrolling doesn't fetch everything. */
        const val GIFT_ANIMATION_PREFETCH_COUNT = 3
    }
}

private const val CHAT_UPSELL_FROM_TYPE = 2

private val AlbumPhoto.displayUrl: String?
    get() = thumbnailUrl?.takeIf { it.isNotBlank() }
        ?: imageUrl?.takeIf { it.isNotBlank() }
        ?: coverUrl?.takeIf { it.isNotBlank() }

/**
 * Failed media bubbles may still hold a local URI, a plain remote key, or a wire JSON body.
 * Prefer the uploadable / sendable path for repository resend.
 */
private fun String.resendMediaSource(preferImage: Boolean): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return trimmed
    if (!trimmed.startsWith("{")) return trimmed
    return runCatching {
        val json = org.json.JSONObject(trimmed)
        fun opt(key: String): String = json.optString(key).trim()
        if (preferImage) {
            sequenceOf("image_url", "pic_url", "media_url", "url", "small_url", "thumb_url")
                .map(::opt)
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
        } else {
            sequenceOf("url", "video_url", "media_url", "cover", "cover_url", "small_url")
                .map(::opt)
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
        }
    }.getOrDefault(trimmed).ifBlank { trimmed }
}
