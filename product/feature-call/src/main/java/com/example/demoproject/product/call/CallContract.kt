package com.example.demoproject.product.call

import com.example.demoproject.platform.data.repository.ReportReason
import com.example.demoproject.product.store.CallBalanceOfferGuideUiState
import com.example.demoproject.product.store.CallHangupContinueUiState
import com.example.demoproject.product.store.CallHangupRechargeUiState
import com.example.demoproject.product.store.CoinPayGuideUiState
import kotlin.math.max

enum class CallRingingPhase {
    /** Waiting for /call/create or an incoming invite. */
    Preparing,
    Outgoing,
    Incoming,
    /** Peer answered / local accept — keep chrome until RTC joins. */
    Connecting,
    /** Connected video call status UI. */
    InCall,
    Ended,
}

enum class CallConnectingNotice {
    PeerLeft,
    PoorNetwork,
}

enum class CallLikePhase {
    Visible,
    Liked,
    Hidden,
}

sealed interface CallChatItem {
    val id: String

    data class SystemTips(override val id: String = "system_tips") : CallChatItem

    /** Server billing / safety tip from MQTT a_type=8 msg_type=4. */
    data class BillingTip(
        override val id: String,
        val text: String,
    ) : CallChatItem

    data class Message(
        override val id: String,
        val isLocal: Boolean,
        val senderName: String,
        val text: String,
        val translation: String? = null,
        val showTranslation: Boolean = false,
    ) : CallChatItem

    data class GiftRequest(
        override val id: String,
        val senderName: String,
        val giftIconUrl: String,
        val giftPrice: Int,
        val giftId: Long,
    ) : CallChatItem
}

data class CallGiftUi(
    val id: Long,
    val title: String,
    val price: Int,
    val iconUrl: String,
    val svgaUrl: String = "",
    val isPlaceholder: Boolean = false,
)

data class CallGiftSentTipUi(
    val giftTitle: String,
    val giftIconUrl: String,
    val count: Int = 1,
    val id: Long = System.currentTimeMillis(),
)

data class CallReportUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val reasons: List<ReportReason> = emptyList(),
    val selectedReasonIds: Set<Int> = emptySet(),
)

data class CallUiState(
    val phase: CallRingingPhase = CallRingingPhase.Preparing,
    /** True only for a call entered from the random-match flow. */
    val isMatchCall: Boolean = false,
    val connectingNotice: CallConnectingNotice? = null,
    val peerUserId: String = "",
    val peerNickname: String = "",
    val peerAge: Int = 0,
    val peerAvatarUrl: String = "",
    /** Absolute video-show URL; blank → show avatar fallback. */
    val videoUrl: String = "",
    val coverUrl: String = "",
    val errorMessage: String? = null,
    /** HTTP room key for in-call gift send. */
    val callRoomId: String = "",
    val callDurationSec: Int = 0,
    val matchSessionId: Long = 0L,
    val matchId: Long? = null,
    val matchTimeSeconds: Int = 0,
    val nextTimeSeconds: Int = 0,
    val isMatchNextInProgress: Boolean = false,
    val isMatchReceiveOnly: Boolean = false,
    val likePhase: CallLikePhase = CallLikePhase.Visible,
    val micEnabled: Boolean = true,
    val cameraEnabled: Boolean = true,
    /** Remote Agora uid once peer joins; 0 = not yet. */
    val remoteRtcUid: Int = 0,
    /** True when local RTC surface should be bound (after join). */
    val rtcSurfacesActive: Boolean = false,
    /** Peer enabled camera mask/blur (MQTT a_type=9). */
    val peerMasked: Boolean = false,
    val isMoreSheetVisible: Boolean = false,
    val isGiftSheetVisible: Boolean = false,
    val isReportSheetVisible: Boolean = false,
    val report: CallReportUiState? = null,
    val coinPayGuide: CoinPayGuideUiState? = null,
    val hangupRecharge: CallHangupRechargeUiState? = null,
    val hangupContinue: CallHangupContinueUiState? = null,
    val showGiftQuickBar: Boolean = false,
    val coinBalance: Int = 0,
    val gifts: List<CallGiftUi> = emptyList(),
    val selectedGiftId: Long? = null,
    val isGiftCatalogLoading: Boolean = false,
    val isGiftSending: Boolean = false,
    val giftAnimationUrl: String? = null,
    val giftSentTip: CallGiftSentTipUi? = null,
    val draftMessage: String = "",
    val isSendingMessage: Boolean = false,
    val chatItems: List<CallChatItem> = listOf(CallChatItem.SystemTips()),
    /** Fixed at `/call/create` or invite `call_free_min`; never re-derived mid-call. */
    val isFreeCall: Boolean = false,
    val balanceAlertRoomKey: String = "",
    val balanceAlertRawDuration: Int = 0,
    val balanceAlertTotalDuration: Int = 0,
    val balanceAlertSaleThreshold: Int = 0,
    val balanceAlertRechargeThreshold: Int = 0,
    val balanceOffer: CallBalanceOffer? = null,
    val showBalanceFloat: Boolean = false,
    val isBalanceOfferGuideVisible: Boolean = false,
    val balanceOfferGuide: CallBalanceOfferGuideUiState? = null,
    val freeCallTriggerConsumed: Boolean = false,
    val autoShownBalanceOfferRoomId: String = "",
) {
    val displayName: String
        get() = if (peerAge > 0 && peerNickname.isNotBlank()) {
            "$peerNickname, $peerAge"
        } else {
            peerNickname
        }

    val formattedDuration: String
        get() {
            val total = callDurationSec.coerceAtLeast(0)
            val minutes = total / 60
            val seconds = total % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    val showMatchNext: Boolean
        get() = isMatchCall && matchTimeSeconds > 0 && callDurationSec < matchTimeSeconds

    val isMatchNextEnabled: Boolean
        get() = showMatchNext &&
            !isMatchNextInProgress &&
            callDurationSec >= max(nextTimeSeconds, MATCH_NEXT_MINIMUM_DELAY_SECONDS)

    val matchNextCountdownSec: Int
        get() = (
            max(nextTimeSeconds, MATCH_NEXT_MINIMUM_DELAY_SECONDS) - callDurationSec
            ).coerceAtLeast(0)

    val isMatchHangupEnabled: Boolean
        get() = !isMatchCall ||
            callDurationSec >= max(nextTimeSeconds, MATCH_NEXT_MINIMUM_DELAY_SECONDS)
}

const val MATCH_NEXT_MINIMUM_DELAY_SECONDS: Int = 6

sealed interface CallIntent {
    data object Hangup : CallIntent
    data object NextMatch : CallIntent
    data object Answer : CallIntent
    data object Report : CallIntent
    data object DismissReport : CallIntent
    data class ToggleReportReason(val reasonId: Int) : CallIntent
    data object SubmitReport : CallIntent
    data object Like : CallIntent
    data object OpenMore : CallIntent
    data object DismissMore : CallIntent
    data class SetMicEnabled(val enabled: Boolean) : CallIntent
    data class SetCameraEnabled(val enabled: Boolean) : CallIntent
    data object FlipCamera : CallIntent
    data object OpenGiftSheet : CallIntent
    data object DismissGiftSheet : CallIntent
    data class SelectGift(val giftId: Long) : CallIntent
    data object SendSelectedGift : CallIntent
    data class SendQuickGift(val giftId: Long) : CallIntent
    data class DraftChanged(val text: String) : CallIntent
    data object SendMessage : CallIntent
    data class ToggleMessageTranslation(val messageId: String) : CallIntent
    data object DismissGiftAnimation : CallIntent
    data object DismissGiftSentTip : CallIntent
    data object OpenCoins : CallIntent
    data object DismissCoinPayGuide : CallIntent
    data class PurchaseCoinPayGuideCoin(val offerId: Long) : CallIntent
    data class PurchaseCoinPayGuideSale(val offerId: Long) : CallIntent
    data object DismissHangupRecharge : CallIntent
    data class PurchaseHangupRechargeCoin(val offerId: Long) : CallIntent
    data class PurchaseHangupRechargeSale(val offerId: Long) : CallIntent
    data object DismissHangupContinue : CallIntent
    data object HangupContinueVideo : CallIntent
    data object HangupContinueChat : CallIntent
    data object OpenBalanceOfferGuide : CallIntent
    data object DismissBalanceOfferGuide : CallIntent
    data object BalanceOfferContinue : CallIntent
    data object BalanceOfferMoreOptions : CallIntent
    data class PurchaseBalanceOfferVip(val offerId: Long) : CallIntent
    data class PurchaseBalanceOfferSale(val offerId: Long) : CallIntent
}

sealed interface CallEffect {
    data object Exit : CallEffect
    data object OpenMatch : CallEffect
    data class ShowMessage(val message: String) : CallEffect
    data object OpenStore : CallEffect
    data class OpenChatDetail(val conversationId: String, val nickname: String) : CallEffect
    data class OpenReport(val userId: String, val age: Int = 0) : CallEffect
    data class RestartVideoCall(
        val userId: String,
        val nickname: String,
        val age: Int,
        val avatarUrl: String,
        val videoUrl: String,
        val coverUrl: String,
    ) : CallEffect
}
