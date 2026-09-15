package com.example.demoproject.product.profile

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoproject.platform.data.model.ChatSendLimitNotice
import com.example.demoproject.platform.data.model.GiftFromType
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.network.dto.TranslationSubmitRequestDto
import com.example.demoproject.platform.data.repository.ProfileHomeDetail
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.ui.designsystem.gift.GiftSvgaPreloader
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    application: Application,
    /** Public backend `user_id` for another user's home; null/blank loads self. */
    private val externalUserId: String? = null,
) : AndroidViewModel(application) {
    private val runtime = NetworkRuntime.get(application)
    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<ProfileEffect> = _effect.asSharedFlow()

    private val isSelfTarget: Boolean
        get() = externalUserId.isNullOrBlank()

    init {
        viewModelScope.launch {
            runtime.accountBalanceStore.coins.collect { coins ->
                _uiState.update { it.copy(coinBalance = coins) }
            }
        }
        viewModelScope.launch {
            runtime.blockRepository.blockedUids.collect { blocked ->
                val uid = _uiState.value.userId.toLongOrNull()
                if (uid != null && uid > 0L) {
                    _uiState.update { it.copy(isBlocked = uid in blocked) }
                }
            }
        }
        onIntent(ProfileIntent.Load)
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Load -> loadProfile()
            ProfileIntent.Back -> emit(ProfileEffect.NavigateBack)
            ProfileIntent.More -> openMoreSheet()
            ProfileIntent.DismissMoreSheet -> _uiState.update {
                it.copy(isMoreSheetVisible = false)
            }
            ProfileIntent.MoreFollow -> {
                _uiState.update { it.copy(isMoreSheetVisible = false) }
                requestUnfollowOrFollow()
            }
            ProfileIntent.MoreBlock -> {
                val state = _uiState.value
                if (state.isBlocked) {
                    _uiState.update { it.copy(isMoreSheetVisible = false) }
                    unblockUser()
                } else {
                    _uiState.update {
                        it.copy(
                            isMoreSheetVisible = false,
                            confirmDialog = ProfileConfirmDialog.Block,
                        )
                    }
                }
            }
            ProfileIntent.MoreReport -> {
                _uiState.update { it.copy(isMoreSheetVisible = false) }
                emit(ProfileEffect.ShowMessage(str(R.string.profile_message_report_soon)))
            }
            ProfileIntent.DismissConfirmDialog -> _uiState.update {
                it.copy(confirmDialog = null)
            }
            ProfileIntent.ConfirmUnfollow -> {
                _uiState.update { it.copy(confirmDialog = null) }
                toggleFollow()
            }
            ProfileIntent.ConfirmBlock -> blockUser()
            ProfileIntent.ToggleFollow -> requestUnfollowOrFollow()
            ProfileIntent.TranslateBio -> translateOrCollapseBio()
            ProfileIntent.Gift -> openGiftSheet()
            ProfileIntent.DismissGiftSheet -> _uiState.update {
                it.copy(isGiftSheetVisible = false, isGiftSending = false)
            }
            is ProfileIntent.SelectGift -> {
                _uiState.update { it.copy(selectedGiftId = intent.giftId) }
                val url = _uiState.value.gifts
                    .firstOrNull { it.id == intent.giftId }
                    ?.svgaUrl
                GiftSvgaPreloader.prefetch(getApplication(), url)
            }
            ProfileIntent.SendGift -> sendSelectedGift()
            ProfileIntent.DismissGiftAnimation -> _uiState.update {
                it.copy(giftAnimationUrl = null)
            }
            ProfileIntent.OpenCoins -> emit(ProfileEffect.OpenStore)
            ProfileIntent.Message -> openChatDetail()
            ProfileIntent.VideoChat -> {
                if (_uiState.value.isBlockedByPeer) return
                emit(ProfileEffect.ShowMessage(str(R.string.profile_message_video_soon)))
            }
            is ProfileIntent.OpenMedia -> {
                val index = intent.index
                if (index in _uiState.value.viewerItems.indices) {
                    _uiState.update { it.copy(viewerIndex = index) }
                }
            }
            ProfileIntent.CloseMedia -> _uiState.update { it.copy(viewerIndex = null) }
        }
    }

    private fun openChatDetail() {
        val state = _uiState.value
        if (state.isSelf || state.isBlockedByPeer || state.userId.isBlank()) return
        emit(
            ProfileEffect.OpenChatDetail(
                conversationId = state.userId,
                nickname = state.nickname,
            ),
        )
    }

    private fun openGiftSheet() {
        val state = _uiState.value
        if (state.isSelf || state.isBlockedByPeer || state.userId.isBlank()) {
            if (!state.isBlockedByPeer) {
                emit(ProfileEffect.ShowMessage(str(R.string.profile_message_gift_soon)))
            }
            return
        }
        _uiState.update {
            it.copy(
                isGiftSheetVisible = true,
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
                            selectedGiftId = it.selectedGiftId
                                ?: gifts.firstOrNull()?.id,
                        )
                    }
                    val selectedId = _uiState.value.selectedGiftId
                    val url = gifts.firstOrNull { it.id == selectedId }?.svgaUrl
                    GiftSvgaPreloader.prefetch(getApplication(), url)
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isGiftCatalogLoading = false) }
                    emit(ProfileEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun sendSelectedGift() {
        val state = _uiState.value
        val giftId = state.selectedGiftId ?: return
        val selected = state.gifts.firstOrNull { it.id == giftId } ?: return
        if (state.isSelf || state.userId.isBlank() || state.isGiftSending) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGiftSending = true) }
            GiftSvgaPreloader.prefetch(getApplication(), selected.svgaUrl)
            when (
                val result = runtime.messageRepository.sendGift(
                    conversationId = state.userId,
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
                        emit(ProfileEffect.ShowMessage(str(R.string.profile_gift_send_success)))
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isGiftSending = false) }
                    emit(ProfileEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, viewerIndex = null)
            }
            val result = if (isSelfTarget) {
                runtime.profileRepository.getMyHomeDetail()
            } else {
                runtime.profileRepository.getUserHomeDetail(externalUserId!!.trim())
            }
            when (result) {
                is AppResult.Success -> {
                    applyDetail(result.data)
                    if (result.data.isSelf) {
                        result.data.coinBalance?.let { runtime.accountBalanceStore.update(it) }
                        runtime.vipStatusStore.update(
                            isVip = result.data.user.isVip,
                            expiryText = result.data.user.vipExpireTime,
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun applyDetail(detail: ProfileHomeDetail) {
        val user = detail.user
        val blockedByPeerFromHome = !detail.isSelf && user.isBlockedByPeer
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                isSelf = detail.isSelf,
                userId = user.id,
                nickname = user.nickname,
                age = user.age,
                avatarUrl = user.avatar,
                backgroundUrl = detail.backgroundUrl,
                countryFlag = countryCodeToFlagEmoji(user.countryCode),
                countryName = user.countryName.orEmpty(),
                followingCount = detail.followingCount,
                followerCount = detail.followerCount,
                bio = user.bio,
                translatedBio = "",
                showTranslatedBio = false,
                isTranslatingBio = false,
                isFollowing = user.isFollowing,
                isBlocked = runtime.blockRepository.isBlocked(user.id),
                isBlockedByPeer = blockedByPeerFromHome,
                media = if (blockedByPeerFromHome) {
                    emptyList()
                } else {
                    detail.media.map { photo -> photo.toProfileMediaUi() }
                },
                viewerItems = if (blockedByPeerFromHome) {
                    emptyList()
                } else {
                    detail.media.toViewerItems()
                },
                viewerIndex = null,
            )
        }
        if (!detail.isSelf && user.id.isNotBlank()) {
            refreshBlockedByPeerFromChat(user.id, blockedByPeerFromHome)
        }
    }

    /**
     * `/home/info` may omit reverse-block flags; chat control notices (`key=102`)
     * remain the durable local signal that the peer blocked the current user.
     */
    private fun refreshBlockedByPeerFromChat(userId: String, alreadyBlocked: Boolean) {
        if (alreadyBlocked) return
        viewModelScope.launch {
            val messages = runtime.messageRepository.observeMessages(userId).first()
            val blockedByPeer = messages.any { message ->
                ChatSendLimitNotice.keyOf(message.content) ==
                    ChatSendLimitNotice.KEY_BLOCKED_BY_PEER
            }
            if (!blockedByPeer) return@launch
            _uiState.update {
                if (it.userId != userId || it.isBlockedByPeer) {
                    it
                } else {
                    it.copy(
                        isBlockedByPeer = true,
                        media = emptyList(),
                        viewerItems = emptyList(),
                        viewerIndex = null,
                    )
                }
            }
        }
    }

    private fun translateOrCollapseBio() {
        val state = _uiState.value
        if (state.bio.isBlank() || state.isTranslatingBio) return
        if (state.showTranslatedBio) {
            _uiState.update { it.copy(showTranslatedBio = false) }
            return
        }
        if (state.translatedBio.isNotBlank()) {
            _uiState.update { it.copy(showTranslatedBio = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTranslatingBio = true) }
            val fromUid = state.userId.toLongOrNull() ?: 0L
            when (
                val result = runtime.messageRepository.translateText(
                    content = state.bio,
                    fromUid = fromUid,
                    fromType = TranslationSubmitRequestDto.FROM_TYPE_PROFILE_SIGN,
                )
            ) {
                is AppResult.Success -> {
                    val text = result.data.trim()
                    if (text.isBlank()) {
                        _uiState.update { it.copy(isTranslatingBio = false) }
                        emit(ProfileEffect.ShowMessage(str(R.string.profile_translate_failed)))
                    } else {
                        _uiState.update {
                            it.copy(
                                isTranslatingBio = false,
                                translatedBio = text,
                                showTranslatedBio = true,
                            )
                        }
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isTranslatingBio = false) }
                    emit(ProfileEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun openMoreSheet() {
        val state = _uiState.value
        if (state.isSelf || state.userId.isBlank()) {
            emit(ProfileEffect.ShowMessage(str(R.string.profile_message_more_soon)))
            return
        }
        _uiState.update { it.copy(isMoreSheetVisible = true) }
    }

    private fun requestUnfollowOrFollow() {
        val state = _uiState.value
        if (state.isSelf || state.isFollowBusy || state.userId.isBlank()) return
        if (state.isFollowing) {
            _uiState.update { it.copy(confirmDialog = ProfileConfirmDialog.Unfollow) }
        } else {
            toggleFollow()
        }
    }

    private fun blockUser() {
        val state = _uiState.value
        if (state.isSelf || state.isBlockBusy || state.userId.isBlank()) return
        val targetUid = state.userId.toLongOrNull()
        if (targetUid == null || targetUid <= 0L) {
            emit(ProfileEffect.ShowMessage(str(R.string.profile_message_more_soon)))
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBlockBusy = true,
                    isMoreSheetVisible = false,
                    confirmDialog = null,
                )
            }
            when (val result = runtime.blockRepository.blockUser(targetUid)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isBlockBusy = false, isBlocked = true) }
                    emit(ProfileEffect.ShowMessage(str(R.string.profile_block_success)))
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isBlockBusy = false) }
                    emit(ProfileEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun unblockUser() {
        val state = _uiState.value
        if (state.isSelf || state.isBlockBusy || state.userId.isBlank()) return
        val targetUid = state.userId.toLongOrNull()
        if (targetUid == null || targetUid <= 0L) {
            emit(ProfileEffect.ShowMessage(str(R.string.profile_message_more_soon)))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBlockBusy = true) }
            when (val result = runtime.blockRepository.unblockUser(targetUid)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isBlockBusy = false, isBlocked = false) }
                    emit(ProfileEffect.ShowMessage(str(R.string.profile_unblock_success)))
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isBlockBusy = false) }
                    emit(ProfileEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun toggleFollow() {
        val state = _uiState.value
        if (state.isSelf || state.isFollowBusy || state.userId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFollowBusy = true) }
            val result = if (state.isFollowing) {
                runtime.profileRepository.unfollowUser(state.userId)
            } else {
                runtime.profileRepository.followUser(state.userId)
            }
            when (result) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isFollowBusy = false,
                        isFollowing = !state.isFollowing,
                        followerCount = (it.followerCount + if (state.isFollowing) -1 else 1)
                            .coerceAtLeast(0),
                    )
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isFollowBusy = false) }
                    emit(ProfileEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun emit(effect: ProfileEffect) {
        _effect.tryEmit(effect)
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)
}

internal fun countryCodeToFlagEmoji(code: String?): String {
    val normalized = code?.trim()?.uppercase().orEmpty()
    if (normalized.length != 2 || !normalized.all { it in 'A'..'Z' }) return ""
    val first = 0x1F1E6 + (normalized[0].code - 'A'.code)
    val second = 0x1F1E6 + (normalized[1].code - 'A'.code)
    return String(Character.toChars(first)) + String(Character.toChars(second))
}
