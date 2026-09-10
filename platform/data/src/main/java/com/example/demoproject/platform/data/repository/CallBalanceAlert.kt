package com.example.demoproject.platform.data.repository

data class CallBalanceAlert(
    /**
     * Numeric room id when MQTT `room_id` is numeric or `room_session_id` is present.
     * May be `0` for channel-style match rooms that only send a string `room_id`.
     */
    val roomId: Long,
    val balance: Int,
    /** Server field `duration` (raw), unmodified. */
    val remainingSeconds: Int,
    /**
     * Server field `total_duration` (raw), unmodified — may be `0`. Whether this is combined
     * with [remainingSeconds] (free calls) or used alone (paid calls) is decided by the caller
     * per `/call/create` `call_free_min`; see `resolveCallBalanceAlertRemainingSeconds`.
     */
    val totalDurationSeconds: Int,
    val vipPayItem: RechargeVipPayItem? = null,
    val saleItems: List<RechargeSaleItem> = emptyList(),
    val originalPrice: String?,
    val saleDesc: String?,
    /**
     * Raw MQTT `room_id` (numeric string or channel key such as `sc…`).
     * Blank means fall back to [roomId] as decimal string.
     */
    val roomKey: String = "",
    /**
     * Server field `sale_recharge_alert_time` — remaining-seconds threshold (`total_duration -`
     * elapsed) at which the match-call floating recharge guide should appear. `0`/missing
     * means the server did not send a timing hint for this push (e.g. non-match calls per
     * BerryCam §6.0 commercialization popup rules), and callers should fall back to a local default.
     */
    val saleRechargeAlertTimeSeconds: Int = 0,
    /**
     * Server field `recharge_alert_time` — remaining-seconds threshold at which the match-call
     * half-screen recharge guide / VIP offer sheet should auto-open. Same `0`/missing
     * fallback semantics as [saleRechargeAlertTimeSeconds].
     */
    val rechargeAlertTimeSeconds: Int = 0,
) {
    val isVipCoinSaleAlert: Boolean
        get() = vipPayItem == null && saleItems.isNotEmpty()

    /** Stable key for matching the active call/match room and auto-offer dedupe. */
    val effectiveRoomKey: String
        get() = roomKey.trim().takeIf { it.isNotEmpty() }
            ?: roomId.takeIf { it > 0L }?.toString().orEmpty()
}
