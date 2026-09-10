package com.example.demoproject.product.chat

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
    val isEmojiSheetVisible: Boolean = false,
    val gifts: List<ChatGiftUi> = emptyList(),
    val selectedGiftId: Long? = null,
    val isGiftCatalogLoading: Boolean = false,
    val isGiftSending: Boolean = false,
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
        val url: String,
        val locked: Boolean = false,
    ) : ChatDetailMessageBody

    data class Video(
        val url: String,
        val durationLabel: String? = null,
        val locked: Boolean = false,
    ) : ChatDetailMessageBody

    data class Gift(
        val title: String,
        val iconUrl: String,
        val price: Int?,
        val isRequest: Boolean,
        val peerName: String,
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
    data object OpenCoins : ChatDetailIntent
    data object OpenPlusMenu : ChatDetailIntent
    data object OpenEmoji : ChatDetailIntent
    data object DismissEmojiSheet : ChatDetailIntent
    data class InsertEmoji(val emoji: String) : ChatDetailIntent
    data object StartVideoCall : ChatDetailIntent
    data class OpenMedia(val messageId: String) : ChatDetailIntent
    data class SendRequestedGift(val messageId: String) : ChatDetailIntent
}

sealed interface ChatDetailEffect {
    data object NavigateBack : ChatDetailEffect
    data class OpenPeerProfile(val externalUserId: String) : ChatDetailEffect
    data class StartVideoCall(val peerUserId: String) : ChatDetailEffect
    data class ShowMessage(val message: String) : ChatDetailEffect
    data object OpenPlusMenu : ChatDetailEffect
    data object OpenMoreMenu : ChatDetailEffect
    data object OpenStore : ChatDetailEffect
}
