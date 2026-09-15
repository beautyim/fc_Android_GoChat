package com.example.demoproject.product.chat

import com.example.demoproject.product.store.CoinPayGuideUiState
import com.example.demoproject.product.vip.VipPayGuideUiState
import com.example.demoproject.ui.designsystem.media.MediaViewerItem

data class ChatDetailUiState(
    val conversationId: String = "",
    val currentUserId: String = "",
    val selfAvatarUrl: String? = null,
    val peerId: String = "",
    val peerExternalUserId: String = "",
    val nickname: String = "",
    val age: Int = 0,
    val peerAvatarUrl: String? = null,
    val isOnline: Boolean = false,
    val countryFlag: String = "",
    val countryName: String = "",
    val peerHasReplied: Boolean = false,
    val isFollowing: Boolean = false,
    val showFollowedFlash: Boolean = false,
    val freeMessageCount: Int = 0,
    val unlockFromType: Int? = null,
    val draft: String = "",
    val coinBalance: Int = 0,
    val isGiftSheetVisible: Boolean = false,
    val isMediaTypeSheetVisible: Boolean = false,
    val isEmojiSheetVisible: Boolean = false,
    /** First-visit horizontal gift shortcuts above the composer. */
    val showGiftQuickBar: Boolean = false,
    /** First-visit waving-hand greeting overlay. */
    val showGreetingGesture: Boolean = false,
    val gifts: List<ChatGiftUi> = emptyList(),
    val selectedGiftId: Long? = null,
    val isGiftCatalogLoading: Boolean = false,
    val isGiftSending: Boolean = false,
    /** Absolute SVGA URL to play fullscreen over the chat; null when idle. */
    val giftAnimationUrl: String? = null,
    /** Shared profile-style media viewer items; empty when closed. */
    val mediaViewerItems: List<MediaViewerItem> = emptyList(),
    val mediaViewerIndex: Int? = null,
    /** VIP pay-guide sheet after free messages are exhausted (`msg/send` ok=3). */
    val vipPayGuide: VipPayGuideUiState? = null,
    /** Coin pay-guide sheet after insufficient balance (`msg/send` ok=6 / balance errors). */
    val coinPayGuide: CoinPayGuideUiState? = null,
    val items: List<ChatDetailListItem> = emptyList(),
    val hasMore: Boolean = true,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val translatingMessageId: String? = null,
    val errorMessage: String? = null,
    val hasLoaded: Boolean = false,
) {
    val title: String
        get() = if (age > 0) "$nickname, $age" else nickname

    val showFollowAction: Boolean
        get() = !isFollowing && !showFollowedFlash

    val composerShowsVideoCall: Boolean
        get() = draft.isBlank() && freeMessageCount > 0
}

sealed interface ChatDetailListItem {
    val key: String

    data object SafetyTips : ChatDetailListItem {
        override val key: String = "safety_tips"
    }

    data class ProfileCard(
        val nickname: String,
        val age: Int,
        val avatarUrl: String?,
        val countryFlag: String,
        val countryName: String,
        val photoUrls: List<String>,
        val extraPhotoCount: Int,
    ) : ChatDetailListItem {
        override val key: String = "profile_card"
        val title: String get() = if (age > 0) "$nickname, $age" else nickname
    }

    data class TimeSeparator(
        val label: String,
        override val key: String,
    ) : ChatDetailListItem

    data class MessageRow(
        val message: ChatDetailMessageUi,
    ) : ChatDetailListItem {
        override val key: String = "msg_${message.id}"
    }
}

data class ChatDetailMessageUi(
    val id: String,
    val isMine: Boolean,
    val status: ChatDetailMessageStatus,
    val createdAtMillis: Long,
    val timeLabel: String,
    val body: ChatDetailMessageBody,
)

/** Maps an unlocked image/video bubble into the shared [MediaViewerItem] model. */
internal fun ChatDetailMessageUi.toMediaViewerItem(): MediaViewerItem? {
    return when (val body = body) {
        is ChatDetailMessageBody.Image -> {
            if (body.locked) return null
            val url = body.previewUrl.ifBlank { body.url }.takeIf { it.isNotBlank() } ?: return null
            MediaViewerItem(
                id = id.stableMediaId(),
                isVideo = false,
                imageUrl = url,
                videoUrl = null,
                coverUrl = url,
            )
        }
        is ChatDetailMessageBody.Video -> {
            if (body.locked) return null
            val cover = body.previewUrl.ifBlank { body.url }.takeIf { it.isNotBlank() }
            val video = body.videoUrl.takeIf { it.isNotBlank() }
            if (cover.isNullOrBlank() && video.isNullOrBlank()) return null
            MediaViewerItem(
                id = id.stableMediaId(),
                isVideo = true,
                imageUrl = cover,
                videoUrl = video,
                coverUrl = cover,
                durationSeconds = body.durationSeconds,
            )
        }
        else -> null
    }
}

private fun String.stableMediaId(): Long {
    // Prefer numeric server mtime / id; fall back to a stable hash for local_* ids.
    toLongOrNull()?.let { return it }
    filter { it.isDigit() }.toLongOrNull()?.let { return it }
    return padEnd(16, '0').take(16).fold(0L) { acc, c -> (acc * 31L) + c.code }
}

enum class ChatDetailMessageStatus {
    Sending,
    Sent,
    Failed,
}

sealed interface ChatDetailMessageBody {
    data class Text(
        val text: String,
        val translatedText: String? = null,
    ) : ChatDetailMessageBody

    data class Image(
        /** Bubble + preview — clear / full-size (`image_url`). */
        val url: String,
        val previewUrl: String = url,
        val locked: Boolean = false,
    ) : ChatDetailMessageBody

    data class Video(
        /** Bubble cover / thumb. */
        val url: String,
        /** Fullscreen cover image. */
        val previewUrl: String = url,
        /** Absolute video playback URL. */
        val videoUrl: String = "",
        val durationLabel: String? = null,
        val durationSeconds: Int = 0,
        val locked: Boolean = false,
    ) : ChatDetailMessageBody

    data class Gift(
        val title: String,
        val iconUrl: String,
        val price: Int?,
        val isRequest: Boolean,
        val peerName: String,
        /** True when an SVGA animation is known for this gift, so the bubble is tappable. */
        val canPlayAnimation: Boolean = false,
    ) : ChatDetailMessageBody

    data class Call(
        val label: String,
        val isMissed: Boolean,
    ) : ChatDetailMessageBody

    data class SystemNotice(
        val text: String,
    ) : ChatDetailMessageBody

    data class Emoji(
        val text: String,
    ) : ChatDetailMessageBody
}

sealed interface ChatDetailIntent {
    data object RetryLoad : ChatDetailIntent
    data object LoadMore : ChatDetailIntent
    data class DraftChanged(val value: String) : ChatDetailIntent
    data object SendText : ChatDetailIntent
    data class Resend(val messageId: String) : ChatDetailIntent
    data class Translate(val messageId: String) : ChatDetailIntent
    data object Follow : ChatDetailIntent
    data object OpenPeerProfile : ChatDetailIntent
    data object OpenMore : ChatDetailIntent
    data object OpenGiftPanel : ChatDetailIntent
    data object DismissGiftSheet : ChatDetailIntent
    data class SelectGift(val giftId: Long) : ChatDetailIntent
    data object SendGift : ChatDetailIntent
    data class SendQuickGift(val giftId: Long) : ChatDetailIntent
    data object SendGreeting : ChatDetailIntent
    data object DismissGreeting : ChatDetailIntent
    data object OpenCoins : ChatDetailIntent
    data object OpenPlusMenu : ChatDetailIntent
    data object DismissMediaTypeSheet : ChatDetailIntent
    data object SelectSendImage : ChatDetailIntent
    data object SelectSendVideo : ChatDetailIntent
    data class SendPickedImage(val localUri: String) : ChatDetailIntent
    data class SendPickedVideo(val localUri: String) : ChatDetailIntent
    data object OpenEmoji : ChatDetailIntent
    data object DismissEmojiSheet : ChatDetailIntent
    data class InsertEmoji(val emoji: String) : ChatDetailIntent
    data object StartVideoCall : ChatDetailIntent
    data class OpenMedia(val messageId: String) : ChatDetailIntent
    data object DismissMediaPreview : ChatDetailIntent
    data class SendRequestedGift(val messageId: String) : ChatDetailIntent
    data class PlayGiftAnimation(val messageId: String) : ChatDetailIntent
    data object DismissGiftAnimation : ChatDetailIntent
    data object DismissVipPayGuide : ChatDetailIntent
    data object PurchaseVipPayGuide : ChatDetailIntent
    data object DismissCoinPayGuide : ChatDetailIntent
    data class PurchaseCoinPayGuideCoin(val offerId: Long) : ChatDetailIntent
    data class PurchaseCoinPayGuideSale(val offerId: Long) : ChatDetailIntent
}

sealed interface ChatDetailEffect {
    data object NavigateBack : ChatDetailEffect
    data class OpenPeerProfile(val externalUserId: String) : ChatDetailEffect
    data class StartVideoCall(val peerUserId: String) : ChatDetailEffect
    data class ShowMessage(val message: String) : ChatDetailEffect
    data object OpenMoreMenu : ChatDetailEffect
    data object OpenStore : ChatDetailEffect
    /** User chose send-image from the plus action sheet; wire media picker next. */
    data object PickSendImage : ChatDetailEffect
    /** User chose send-video from the plus action sheet; wire media picker next. */
    data object PickSendVideo : ChatDetailEffect
}
