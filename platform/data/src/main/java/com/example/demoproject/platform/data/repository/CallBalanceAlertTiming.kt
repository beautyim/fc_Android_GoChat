package com.example.demoproject.platform.data.repository

/**
 * Live remaining seconds for an in-call balance alert.
 *
 * Free vs paid is fixed at `/call/create` / invite `call_free_min` ([isFreeCall]), never
 * re-derived from later MQTT balance pushes.
 */
fun resolveCallBalanceAlertRemainingSeconds(
    isFreeCall: Boolean,
    rawDurationSeconds: Int,
    totalDurationSeconds: Int,
    elapsedSeconds: Int,
): Int {
    val elapsed = elapsedSeconds.coerceAtLeast(0)
    return when {
        isFreeCall -> (totalDurationSeconds + rawDurationSeconds) - elapsed
        totalDurationSeconds > 0 -> totalDurationSeconds - elapsed
        else -> rawDurationSeconds
    }.coerceAtLeast(0)
}

/** Match calls always use `total_duration - elapsed`; ordinary free-call math never applies. */
fun resolveMatchBalanceAlertRemainingSeconds(
    totalDurationSeconds: Int,
    elapsedSeconds: Int,
): Int = (totalDurationSeconds - elapsedSeconds.coerceAtLeast(0)).coerceAtLeast(0)

/** Near-hangup / free-force / auto-offer thresholds for ordinary 1v1 video calls. */
object CallBalanceAlertTiming {
    const val ShortCallNearHangupSeconds = 30
    const val LongCallNearHangupSeconds = 60
    const val LongCallElapsedSeconds = 180
    const val FreeCallForceShowSeconds = 10
    const val AutoOfferSeconds = 15
}
