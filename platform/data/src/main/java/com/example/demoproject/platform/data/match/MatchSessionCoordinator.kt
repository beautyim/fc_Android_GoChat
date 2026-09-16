package com.example.demoproject.platform.data.match

import com.example.demoproject.platform.data.repository.MatchStartInfo
import com.example.demoproject.platform.data.repository.RechargePageData
import com.example.demoproject.platform.mqtt.MatchSignalPeer
import com.example.demoproject.platform.mqtt.MatchSignalRoom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MatchCallEntry(
    val matchSessionId: Long,
    val matchId: Long?,
    val peer: MatchSignalPeer,
    val room: MatchSignalRoom,
)

sealed interface MatchContinuation {
    /**
     * @param callConnected false when the previous room ended before RTC ever connected, which
     * lets Match stop retrying rooms that can never be joined.
     */
    data class Searching(val callConnected: Boolean) : MatchContinuation

    data class Success(val info: MatchStartInfo) : MatchContinuation

    data class Failure(
        val message: String,
        val rechargePageData: RechargePageData? = null,
    ) : MatchContinuation
}

/**
 * Process-local, single-use handoff between Match and Call routes.
 *
 * RTC/fencing credentials never enter navigation URLs or saved state. If Android kills the
 * process, the missing entry is handled by returning to Match instead of attempting `/call/create`.
 */
class MatchSessionCoordinator {
    private val callEntries = ConcurrentHashMap<String, MatchCallEntry>()
    private val _continuation = MutableStateFlow<MatchContinuation?>(null)
    private val unjoinableRoomStreak = AtomicInteger(0)

    val continuation: StateFlow<MatchContinuation?> = _continuation.asStateFlow()

    /**
     * Records whether a match room actually connected and returns how many rooms in a row failed
     * to connect. Callers use it to stop chaining `/match/next` when every room dies pre-join
     * (bad credentials, blocked camera, dead channel).
     */
    fun recordRoomOutcome(connected: Boolean): Int =
        if (connected) {
            unjoinableRoomStreak.set(0)
            0
        } else {
            unjoinableRoomStreak.incrementAndGet()
        }

    fun resetRoomOutcomes() {
        unjoinableRoomStreak.set(0)
    }

    fun putCallEntry(entry: MatchCallEntry): String {
        callEntries.clear()
        return UUID.randomUUID().toString().also { callEntries[it] = entry }
    }

    fun consumeCallEntry(id: String): MatchCallEntry? =
        id.takeIf { it.isNotBlank() }?.let(callEntries::remove)

    fun publishContinuation(value: MatchContinuation) {
        _continuation.value = value
    }

    fun consumeContinuation(value: MatchContinuation) {
        _continuation.compareAndSet(value, null)
    }

    fun clear() {
        callEntries.clear()
        _continuation.value = null
        unjoinableRoomStreak.set(0)
    }
}
