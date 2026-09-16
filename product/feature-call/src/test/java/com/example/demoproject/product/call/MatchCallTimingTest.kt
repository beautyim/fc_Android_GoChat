package com.example.demoproject.product.call

import com.example.demoproject.platform.data.repository.resolveMatchBalanceAlertRemainingSeconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchCallTimingTest {
    @Test
    fun `next waits at least six seconds`() {
        val state = CallUiState(
            isMatchCall = true,
            callDurationSec = 5,
            matchTimeSeconds = 60,
            nextTimeSeconds = 2,
        )

        assertEquals(1, state.matchNextCountdownSec)
        assertFalse(state.isMatchNextEnabled)
        assertTrue(state.copy(callDurationSec = 6).isMatchNextEnabled)
    }

    @Test
    fun `server next time can extend lock`() {
        val state = CallUiState(
            isMatchCall = true,
            callDurationSec = 8,
            matchTimeSeconds = 60,
            nextTimeSeconds = 10,
        )

        assertEquals(2, state.matchNextCountdownSec)
        assertFalse(state.isMatchHangupEnabled)
        assertTrue(state.copy(callDurationSec = 10).isMatchHangupEnabled)
    }

    @Test
    fun `next disappears when paid segment starts`() {
        val state = CallUiState(
            isMatchCall = true,
            callDurationSec = 60,
            matchTimeSeconds = 60,
            nextTimeSeconds = 6,
        )

        assertFalse(state.showMatchNext)
        assertFalse(state.isMatchNextEnabled)
        assertTrue(state.isMatchHangupEnabled)
    }

    @Test
    fun `match balance gates use server thresholds`() {
        val remaining = resolveMatchBalanceAlertRemainingSeconds(
            totalDurationSeconds = 30,
            elapsedSeconds = 20,
        )

        assertEquals(10, remaining)
        assertTrue(
            shouldShowMatchBalanceAlertFloatingWindow(
                remainingSeconds = remaining,
                saleRechargeAlertTimeSeconds = 12,
            ),
        )
        assertFalse(
            shouldShowMatchBalanceAlertFloatingWindow(
                remainingSeconds = remaining,
                saleRechargeAlertTimeSeconds = 0,
            ),
        )
    }
}
