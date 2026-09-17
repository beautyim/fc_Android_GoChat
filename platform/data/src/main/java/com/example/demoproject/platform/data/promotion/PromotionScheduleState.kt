package com.example.demoproject.platform.data.promotion

import kotlinx.serialization.Serializable

/**
 * Per-user promotion schedule persisted by [PromotionPopupStore].
 * Pool flags + due timestamps drive [PromotionPopupSelector].
 */
@Serializable
data class PromotionScheduleState(
    val freeMatchDueAt: Long = 0L,
    val freeCallDueAt: Long = 0L,
    val freeMatchInPool: Boolean = false,
    val freeCallInPool: Boolean = false,
    val treasureInPool: Boolean = false,
    val winningInPool: Boolean = false,
    val winningSid: Int = PromotionScheduleConstants.DEFAULT_WINNING_SID,
    val winningDueAt: Long = 0L,
    val globalLastShownAt: Long = 0L,
    val lastTreasureShownAt: Long = 0L,
    val lastVipExpiredTreasureAt: Long = 0L,
    val lastRechargeAt: Long = 0L,
    val paidTreasurePendingColdStart: Boolean = false,
    /** True after the first Online/Home visit in this signed-in session (blocks auto sid=1). */
    val registeredFirstEnterSession: Boolean = false,
    val sessionBoundAt: Long = 0L,
) {
    fun withColdStartSeed(now: Long, isPaidUser: Boolean): PromotionScheduleState {
        var next = copy(
            sessionBoundAt = now,
            registeredFirstEnterSession = false,
        )
        if (freeMatchDueAt <= 0L || freeMatchDueAt <= now) {
            next = next.copy(freeMatchInPool = true, freeMatchDueAt = now)
        }
        if (freeCallDueAt <= 0L || freeCallDueAt <= now) {
            next = next.copy(freeCallInPool = true, freeCallDueAt = now)
        }
        // Winning cold-start sid=10 after +30s.
        if (!next.winningInPool) {
            next = next.copy(
                winningInPool = true,
                winningSid = PromotionScheduleConstants.SID_WINNING_COLD,
                winningDueAt = now + PromotionScheduleConstants.WINNING_COLD_START_DELAY_MILLIS,
            )
        }
        // Treasure scenes.
        next = when {
            !isPaidUser -> next.copy(treasureInPool = true)
            next.paidTreasurePendingColdStart -> next.copy(
                treasureInPool = true,
                paidTreasurePendingColdStart = false,
            )
            else -> {
                val lastDay = dayKey(next.lastRechargeAt)
                val today = dayKey(now)
                if (lastDay != today) {
                    next.copy(treasureInPool = true)
                } else {
                    next
                }
            }
        }
        return next
    }

    fun onWelfareShown(now: Long, isMatch: Boolean): PromotionScheduleState {
        val due = now + PromotionScheduleConstants.WELFARE_REPOOL_MILLIS
        return if (isMatch) {
            copy(
                freeMatchInPool = false,
                freeMatchDueAt = due,
                globalLastShownAt = now,
            )
        } else {
            copy(
                freeCallInPool = false,
                freeCallDueAt = due,
                globalLastShownAt = now,
            )
        }
    }

    fun onTreasureShown(now: Long, isPaidUser: Boolean): PromotionScheduleState {
        val repool = if (isPaidUser) {
            PromotionScheduleConstants.PAID_TREASURE_REPOOL_MILLIS
        } else {
            PromotionScheduleConstants.UNPAID_TREASURE_REPOOL_MILLIS
        }
        return copy(
            treasureInPool = false,
            lastTreasureShownAt = now,
            globalLastShownAt = now,
            // Will re-enter via due check on next evaluate using lastTreasureShownAt + repool.
        ).also { /* repool via lastTreasureShownAt */ }
            .let { state ->
                // Schedule re-entry by clearing pool; evaluator re-adds when cooldown elapsed.
                state
            }
    }

    fun onWinningShown(now: Long): PromotionScheduleState =
        copy(winningInPool = false, winningDueAt = 0L, globalLastShownAt = now)

    fun onPurchaseVerifiedSuccess(now: Long): PromotionScheduleState =
        copy(
            winningInPool = false,
            winningDueAt = 0L,
            paidTreasurePendingColdStart = true,
            lastRechargeAt = now,
        )

    fun markRegisteredFirstEnter(): PromotionScheduleState =
        copy(registeredFirstEnterSession = true)

    companion object {
        fun dayKey(millis: Long): Long = if (millis <= 0L) -1L else millis / 86_400_000L
    }
}

enum class TreasureUserTier {
    Unpaid,
    PaidNonVip,
    PaidVip,
    VipExpired,
}

enum class PromotionPopupType(val priority: Int) {
    FreeMatch(40),
    FreeCall(30),
    Winning(20),
    TreasureBox(10),
}
