package com.example.demoproject.platform.data.repository

/** Figma node 405:11377 — local sprite row 0..6 when CDN [coin_icon] is unavailable. */
internal const val RechargeCoinSpriteTierCount = 7

/** Single-coin row used for sale-card bonus column (no match-count gift). */
internal const val RechargeCoinBonusSpriteIndex = 6

/**
 * Resolves sprite row from backend [coin_index] or [coin_icon] path.
 * Prefer [coinIndex] when the API returns a valid tier (0..6).
 */
internal fun resolveRechargeCoinSpriteIndex(
    coinIconPath: String,
    coinIndex: Int,
): Int {
    if (coinIndex in 0 until RechargeCoinSpriteTierCount) return coinIndex
    return resolveRechargeCoinIconIndex(coinIconPath)
}

/**
 * Maps backend [coin_icon] asset keys (e.g. berrycam/s/coin/coin_1000.png) to local sprite rows.
 */
internal fun resolveRechargeCoinIconIndex(coinIconPath: String): Int {
    val key = coinIconPath.lowercase()
    return when {
        "coin_max" in key -> 5
        "coin_5000" in key -> 4
        "coin_2500" in key -> 3
        "coin_1000" in key -> 2
        "coin_300" in key -> 1
        "coin_200" in key -> 0
        else -> 0
    }
}
