package com.example.demoproject.platform.network.paging

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cursor-style paging envelope used by feed-like endpoints
 * (`/post/list`, `/post/focus-list`, `/post/notice-list`, `/msg/detail-list` …).
 *
 * Back-end contract (per YAML):
 *  - Request carries an opaque cursor (`last_id` or `last_time`, depending on
 *    the endpoint). Pass `0` on the first page.
 *  - Response returns `list`, a flag `has_more` (`1` = yes, `0` = no), and
 *    the next-page cursor to echo back.
 *
 * The cursor is modelled as `String` because some endpoints stringify
 * numbers (e.g. `/post/notice-list` returns `"last_time":"1776764683"`
 * rather than the int) — the [com.example.demoproject.platform.network.paging.CursorPageResponse]
 * happily round-trips both via Kotlinx's `coerceInputValues`.
 */
@Serializable
data class CursorPageResponse<T>(
    val list: List<T> = emptyList(),
    /** BerryCam uses `1` / `0`; we expose [hasMore] as Boolean for callers. */
    @SerialName("has_more") val hasMoreRaw: Int = 0,
    @SerialName("last_id") val lastId: String? = null,
    @SerialName("last_time") val lastTime: String? = null,
) {
    val hasMore: Boolean get() = hasMoreRaw == 1

    /**
     * Resolves the cursor a caller should echo back for the next page,
     * prefering `last_id` and falling back to `last_time` for time-cursor
     * endpoints. Returns `null` when there are no more pages.
     */
    val nextCursor: String?
        get() = if (!hasMore) null else (lastId ?: lastTime)
}
