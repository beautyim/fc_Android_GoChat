package com.example.demoproject.product.chat

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.analytics.AnalyticsEvent
import com.example.demoproject.platform.analytics.AnalyticsHolder
import com.example.demoproject.platform.data.model.AlbumPhoto
import com.example.demoproject.platform.data.model.GiftFromType
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.model.MessageStatus
import com.example.demoproject.platform.data.model.MessageType
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private var cachedMessages: List<Message> = emptyList()
    private var profilePhotos: List<String> = emptyList()
    private var extraPhotoCount: Int = 0

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
                _uiState.update { it.copy(coinBalance = coins) }
            }
        }
        bootstrap()
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
            is ChatDetailIntent.SelectGift -> _uiState.update { it.copy(selectedGiftId = intent.giftId) }
            ChatDetailIntent.SendGift -> sendSelectedGift()
            ChatDetailIntent.OpenCoins -> viewModelScope.launch {
                _effects.send(ChatDetailEffect.OpenStore)
            }
            ChatDetailIntent.OpenPlusMenu -> viewModelScope.launch {
                _effects.send(ChatDetailEffect.OpenPlusMenu)
            }
            ChatDetailIntent.OpenEmoji -> _uiState.update {
                it.copy(isEmojiSheetVisible = true, isGiftSheetVisible = false)
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
            is ChatDetailIntent.OpenMedia -> Unit
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
                runtime.messageRepository.observeMessages(conversationId).collect { messages ->
                    cachedMessages = messages
                    publishItems()
                    _uiState.update {
                        it.copy(isLoading = false, hasLoaded = true, errorMessage = null)
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

    private fun openGiftSheet() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                isGiftSheetVisible = true,
                isEmojiSheetVisible = false,
                isGiftCatalogLoading = it.gifts.isEmpty(),
            )
        }
        if (state.gifts.isEmpty()) {
            loadGiftCatalog()
        }
    }

    private fun loadGiftCatalog() {
        viewModelScope.launch {
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
                    _uiState.update {
                        it.copy(
                            isGiftCatalogLoading = false,
                            gifts = gifts,
                            selectedGiftId = it.selectedGiftId ?: gifts.firstOrNull()?.id,
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isGiftCatalogLoading = false) }
                    _effects.send(ChatDetailEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun sendSelectedGift() {
        val state = _uiState.value
        val giftId = state.selectedGiftId ?: return
        if (state.isGiftSending || giftId <= 0L) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGiftSending = true) }
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
                        it.copy(isGiftSending = false, isGiftSheetVisible = false)
                    }
                    _effects.send(
                        ChatDetailEffect.ShowMessage(str(R.string.chat_detail_gift_send_success)),
                    )
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isGiftSending = false) }
                    _effects.send(ChatDetailEffect.ShowMessage(result.message))
                }
            }
            runtime.messageRepository.syncLatestMessages(
                conversationId = conversationId,
                latestMtimeCeiling = syncCeiling,
            )
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
        )
        _uiState.update { it.copy(items = items) }
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private companion object {
        const val FOLLOWED_FLASH_MS = 2_000L
        const val POST_SEND_TIP_SYNC_RETRY_MS = 400L
    }
}

private val AlbumPhoto.displayUrl: String?
    get() = thumbnailUrl?.takeIf { it.isNotBlank() }
        ?: imageUrl?.takeIf { it.isNotBlank() }
        ?: coverUrl?.takeIf { it.isNotBlank() }
