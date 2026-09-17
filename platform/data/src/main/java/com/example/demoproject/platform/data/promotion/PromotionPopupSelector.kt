package com.example.demoproject.platform.data.promotion

/**
 * Gate + priority selection for the next promotion popup.
 */
data class PromotionPresentGate(
    val hostResumed: Boolean,
    val matchImmersive: Boolean,
    val excludedRoute: Boolean,
    val blockingOverlay: Boolean,
    val rechargeGuideBlocking: Boolean,
    /**
     * Current main bottom-tab route when on one of the five roots
     * (`home` / `call-records` / `match` / `chat` / `profile`); null elsewhere.
     */
    val currentMainTab: String?,
)

data class PromotionSelectionInput(
    val schedule: PromotionScheduleState,
    val now: Long,
    val gate: PromotionPresentGate,
    val matchFreeCount: Int,
    val callFreeMin: Int,
    val coins: Int,
    val isPaidUser: Boolean,
    val hasActivePopup: Boolean,
    val isFetching: Boolean,
)

object PromotionPopupSelector {

    fun select(input: PromotionSelectionInput): PromotionPopupType? {
        if (input.hasActivePopup || input.isFetching) return null
        if (!canPresent(input.gate)) return null
        if (!globalCooldownReady(input.schedule, input.now)) return null

        val candidates = buildList {
            if (input.schedule.freeMatchInPool &&
                input.gate.currentMainTab == TAB_MATCH &&
                input.matchFreeCount > 0
            ) {
                add(PromotionPopupType.FreeMatch)
            }
            if (input.schedule.freeCallInPool &&
                input.gate.currentMainTab == TAB_HOME &&
                input.callFreeMin > 0
            ) {
                add(PromotionPopupType.FreeCall)
            }
            if (input.schedule.winningInPool &&
                input.schedule.winningDueAt <= input.now &&
                winningEligible(input.isPaidUser, input.coins)
            ) {
                add(PromotionPopupType.Winning)
            }
            if (input.schedule.treasureInPool) {
                // Auto-show blocked for unpaid first session enter — floating entry still works.
                val blockAutoUnpaid =
                    !input.isPaidUser && !input.schedule.registeredFirstEnterSession
                if (!blockAutoUnpaid) {
                    add(PromotionPopupType.TreasureBox)
                }
            }
        }
        return candidates.maxByOrNull { it.priority }
    }

    fun canPresent(gate: PromotionPresentGate): Boolean =
        gate.hostResumed &&
            !gate.matchImmersive &&
            !gate.excludedRoute &&
            !gate.blockingOverlay &&
            !gate.rechargeGuideBlocking

    fun globalCooldownReady(schedule: PromotionScheduleState, now: Long): Boolean {
        if (schedule.globalLastShownAt <= 0L) return true
        return now - schedule.globalLastShownAt >= PromotionScheduleConstants.GLOBAL_COOLDOWN_MILLIS
    }

    fun winningEligible(isPaidUser: Boolean, coins: Int): Boolean {
        if (!isPaidUser) return true
        return coins < PromotionScheduleConstants.WINNING_BALANCE_LIMIT
    }

    const val TAB_HOME = "home"
    const val TAB_MATCH = "match"
}
