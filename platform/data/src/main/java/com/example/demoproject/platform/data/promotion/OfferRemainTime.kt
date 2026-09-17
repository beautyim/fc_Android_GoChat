package com.example.demoproject.platform.data.promotion

import kotlin.math.max

/**
 * Normalizes `alert_remain_time` / `vip_alert_remain_time` which may be:
 * remaining seconds, Unix seconds, or Unix milliseconds.
 */
fun toOfferRemainSeconds(
    raw: Long,
    nowMillis: Long = System.currentTimeMillis(),
): Long {
    if (raw <= 0L) return 0L
    // Unix milliseconds
    if (raw > 1_000_000_000_000L) {
        return max(0L, (raw - nowMillis) / 1_000L)
    }
    // Unix seconds
    if (raw > 1_000_000_000L) {
        return max(0L, raw - nowMillis / 1_000L)
    }
    return raw
}
