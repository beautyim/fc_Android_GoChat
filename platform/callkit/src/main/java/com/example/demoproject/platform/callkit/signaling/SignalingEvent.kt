package com.example.demoproject.platform.callkit.signaling

import com.example.demoproject.platform.callkit.CallMediaType

sealed interface SignalingEvent {
    data class IncomingInvite(
        val inviteId: String,
        val callerUserId: String,
        val callerName: String = "",
        val callerAvatar: String = "",
        val callerAge: Int = 0,
        val callType: Int = CallMediaType.Video,
        val channelId: String,
        val rtcToken: String,
        val rtcUid: Int,
        val rtcAppId: String = "",
        /** Numeric `room_session_id` when [inviteId] is a channel-style HTTP room key. */
        val roomSessionId: Long = 0L,
        /**
         * `user_info.call_free_min` from the invite push — `> 0` means this incoming call is
         * free, per product rule. Fixed at invite time, never re-derived later.
         */
        val callFreeMin: Int = 0,
        /**
         * `user_info.video` play / cover paths from the invite push (video show).
         * May be relative CDN keys; UI resolves them before playback.
         */
        val peerVideoUrl: String = "",
        val peerCoverUrl: String = "",
        /** From invite push; echo on `/call/success` / `/call/heart` when present. */
        val fencingToken: String? = null,
    ) : SignalingEvent

    data class InviteAccepted(val inviteId: String, val callId: String) : SignalingEvent

    data class InviteRejected(val inviteId: String, val reason: RejectReason) : SignalingEvent

    data class InviteCancelled(val inviteId: String) : SignalingEvent

    data class CallEnded(val callId: String, val reason: EndReason) : SignalingEvent

    /** MQTT a_type=7 — low balance / recharge guide during an active call. */
    data class BalanceAlert(
        val roomKey: String,
        val roomSessionId: Long = 0L,
        val balance: Int = 0,
        val remainingSeconds: Int = 0,
        val totalDurationSeconds: Int = 0,
        val saleRechargeAlertTimeSeconds: Int = 0,
        val rechargeAlertTimeSeconds: Int = 0,
        val payItem: SignalingCoinOffer? = null,
        val salePayItem: SignalingCoinOffer? = null,
        val vipPayItem: SignalingCoinOffer? = null,
    ) : SignalingEvent

    /**
     * MQTT a_type=6 — in-call wallet balance snapshot (no offer SKU).
     * Emitted after mid-call recharge / VIP so clients refresh coins without a float/guide.
     */
    data class BalanceSync(
        val roomKey: String,
        val balance: Int,
    ) : SignalingEvent

    /**
     * MQTT a_type=8 — in-call chat or billing tip.
     * [msgType] `1` = peer text, `4` = system/billing tip.
     */
    data class InCallChat(
        val roomKey: String,
        val msgType: Int,
        val content: String,
        val sendUid: Long = 0L,
        val messageId: String = "",
    ) : SignalingEvent

    /** MQTT a_type=9 (or other non-lifecycle) — peer camera mask/blur. */
    data class PeerMaskStatus(
        val roomKey: String,
        /** `true` when remote blur is on (`status=1`). */
        val masked: Boolean,
    ) : SignalingEvent

    data class Error(val message: String, val cause: Throwable? = null) : SignalingEvent
}

/** Lightweight coin / VIP SKU snapshot carried on MQTT balance alerts (no data-module dependency). */
data class SignalingCoinOffer(
    val id: Long = 0L,
    val sku: String = "",
    /** `1` = coins, `2` = VIP (server `product_type`). */
    val productType: Int = 1,
    val diamond: Int = 0,
    val giveCoins: Int = 0,
    val moneyDesc: String = "",
    val originalDesc: String = "",
    val saleDesc: String = "",
    /** Percent-only or "38% OFF" from `save_rate` / `sale_desc`. */
    val saveRate: String = "",
    val title: String = "",
    val label: String = "",
    val days: Int = 0,
    val month: Int = 0,
    val matchCount: Int = 0,
) {
    val isVipProduct: Boolean get() = productType == 2
}

