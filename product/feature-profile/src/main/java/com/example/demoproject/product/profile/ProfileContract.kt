package com.example.demoproject.product.profile

import com.example.demoproject.platform.data.model.AlbumPhoto
import com.example.demoproject.product.profile.media.toMediaViewerItem
import com.example.demoproject.ui.designsystem.media.MediaViewerItem

data class ProfileMediaUi(
    val id: Long,
    val isVideo: Boolean,
    val thumbnailUrl: String?,
    val durationSeconds: Int,
)

data class ProfileGiftUi(
    val id: Long,
    val title: String,
    val price: Int,
    val iconUrl: String,
    val svgaUrl: String = "",
    val isPlaceholder: Boolean = false,
)

data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSelf: Boolean = true,
    val userId: String = "",
    val nickname: String = "",
    val age: Int = 0,
    val avatarUrl: String? = null,
    val backgroundUrl: String? = null,
    val countryFlag: String = "",
    val countryName: String = "",
    val followingCount: Int = 0,
    val followerCount: Int = 0,
    val bio: String = "",
    /** Cached translation of [bio]; empty until a successful Translate request. */
    val translatedBio: String = "",
    val showTranslatedBio: Boolean = false,
    val isTranslatingBio: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollowBusy: Boolean = false,
    val isBlocked: Boolean = false,
    /** True when the viewed user has blocked the current account. */
    val isBlockedByPeer: Boolean = false,
    val media: List<ProfileMediaUi> = emptyList(),
    val viewerItems: List<MediaViewerItem> = emptyList(),
    val viewerIndex: Int? = null,
    val coinBalance: Int = 0,
    val isGiftSheetVisible: Boolean = false,
    val gifts: List<ProfileGiftUi> = emptyList(),
    val selectedGiftId: Long? = null,
    val isGiftCatalogLoading: Boolean = false,
    val isGiftSending: Boolean = false,
    /** Absolute SVGA URL to play fullscreen after a successful send; null when idle. */
    val giftAnimationUrl: String? = null,
    val isMoreSheetVisible: Boolean = false,
    val isBlockBusy: Boolean = false,
    val confirmDialog: ProfileConfirmDialog? = null,
)

enum class ProfileConfirmDialog {
    Unfollow,
    Block,
}

sealed interface ProfileIntent {
    data object Load : ProfileIntent
    data object Back : ProfileIntent
    data object More : ProfileIntent
    data object DismissMoreSheet : ProfileIntent
    data object MoreFollow : ProfileIntent
    data object MoreBlock : ProfileIntent
    data object MoreReport : ProfileIntent
    data object DismissConfirmDialog : ProfileIntent
    data object ConfirmUnfollow : ProfileIntent
    data object ConfirmBlock : ProfileIntent
    data object ToggleFollow : ProfileIntent
    data object TranslateBio : ProfileIntent
    data object Gift : ProfileIntent
    data object DismissGiftSheet : ProfileIntent
    data class SelectGift(val giftId: Long) : ProfileIntent
    data object SendGift : ProfileIntent
    data object DismissGiftAnimation : ProfileIntent
    data object OpenCoins : ProfileIntent
    data object Message : ProfileIntent
    data object VideoChat : ProfileIntent
    data class OpenMedia(val index: Int) : ProfileIntent
    data object CloseMedia : ProfileIntent
}

sealed interface ProfileEffect {
    data object NavigateBack : ProfileEffect
    data object OpenStore : ProfileEffect
    data class OpenChatDetail(val conversationId: String, val nickname: String) : ProfileEffect
    data class ShowMessage(val message: String) : ProfileEffect
}

fun AlbumPhoto.toProfileMediaUi(): ProfileMediaUi = ProfileMediaUi(
    id = mediaId,
    isVideo = isVideo,
    thumbnailUrl = coverUrl ?: thumbnailUrl ?: imageUrl,
    durationSeconds = duration,
)

fun List<AlbumPhoto>.toViewerItems(): List<MediaViewerItem> =
    map(AlbumPhoto::toMediaViewerItem)
