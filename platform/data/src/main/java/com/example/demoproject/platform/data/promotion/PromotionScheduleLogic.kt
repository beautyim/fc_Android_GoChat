package com.example.demoproject.platform.data.promotion

/**
 * Pure schedule mutations for cold-start / purchase / MQTT / welfare ticks.
 */
object PromotionScheduleLogic {

    fun onSessionBound(
        state: PromotionScheduleState,
        now: Long,
        isPaidUser: Boolean,
    ): PromotionScheduleState = state.withColdStartSeed(now, isPaidUser)

    fun tickWelfareDue(state: PromotionScheduleState, now: Long): PromotionScheduleState {
        var next = state
        if (!next.freeMatchInPool && next.freeMatchDueAt > 0L && next.freeMatchDueAt <= now) {
            next = next.copy(freeMatchInPool = true)
        }
        if (!next.freeCallInPool && next.freeCallDueAt > 0L && next.freeCallDueAt <= now) {
            next = next.copy(freeCallInPool = true)
        }
        return next
    }

    fun tickTreasureRepool(
        state: PromotionScheduleState,
        now: Long,
        isPaidUser: Boolean,
        isVip: Boolean,
        vipExpired: Boolean,
    ): PromotionScheduleState {
        if (state.treasureInPool) return state
        val last = state.lastTreasureShownAt
        if (last <= 0L) return state
        val cooldown = if (isPaidUser) {
            PromotionScheduleConstants.PAID_TREASURE_REPOOL_MILLIS
        } else {
            PromotionScheduleConstants.UNPAID_TREASURE_REPOOL_MILLIS
        }
        if (now - last < cooldown) return state

        // VIP expired scene sid=8 — interval 3h from lastVipExpiredTreasureAt.
        if (vipExpired && !isVip) {
            val lastVip = state.lastVipExpiredTreasureAt
            if (lastVip > 0L &&
                now - lastVip < PromotionScheduleConstants.VIP_EXPIRED_TREASURE_INTERVAL_MILLIS
            ) {
                return state
            }
            return state.copy(
                treasureInPool = true,
                lastVipExpiredTreasureAt = now,
            )
        }
        return state.copy(treasureInPool = true)
    }

    fun enqueueWinning(
        state: PromotionScheduleState,
        sid: Int,
        delayMillis: Long,
        now: Long,
    ): PromotionScheduleState = state.copy(
        winningInPool = true,
        winningSid = sid,
        winningDueAt = now + delayMillis,
    )

    fun enqueueTreasureFromMqtt(state: PromotionScheduleState): PromotionScheduleState =
        state.copy(treasureInPool = true)

    fun onPurchaseVerifiedSuccess(
        state: PromotionScheduleState,
        now: Long,
    ): PromotionScheduleState = state.onPurchaseVerifiedSuccess(now)

    fun resolveTreasureScene(
        state: PromotionScheduleState,
        isPaidUser: Boolean,
        isVip: Boolean,
        vipExpired: Boolean,
    ): Pair<Int, Int>? {
        if (!state.treasureInPool) return null
        return when {
            !isPaidUser ->
                PromotionScheduleConstants.SID_TREASURE_UNPAID_COLD to
                    PromotionScheduleConstants.TYPE_TREASURE_UNPAID
            state.paidTreasurePendingColdStart ->
                PromotionScheduleConstants.SID_TREASURE_PAID_AFTER_PURCHASE to
                    PromotionScheduleConstants.TYPE_TREASURE_PAID
            vipExpired && !isVip ->
                PromotionScheduleConstants.SID_TREASURE_VIP_EXPIRED to
                    PromotionScheduleConstants.TYPE_TREASURE_PAID
            else ->
                PromotionScheduleConstants.SID_TREASURE_PAID_DAILY to
                    PromotionScheduleConstants.TYPE_TREASURE_PAID
        }
    }
}
