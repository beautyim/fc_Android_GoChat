package com.example.demoproject.platform.network.paging

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Page/size-style paging request used by list endpoints whose backend
 * pagination is 1-based (`/user/list`, `/visitor/list`, `/block/list`).
 *
 * Assumption: the YAML specifies only `page` / `page_size` on the request side;
 * the response shape is not documented. Callers should wrap their own
 * response DTOs but may reuse [OffsetPageResponse] as a defensive default.
 */
@Serializable
data class OffsetPageRequest(
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = DEFAULT_PAGE_SIZE,
) {
    companion object {
        const val DEFAULT_PAGE_SIZE: Int = 20
        const val FIRST_PAGE: Int = 1
    }
}

/**
 * Generic page-envelope used when the server only returns a flat `list`
 * plus optional `has_more` / `total`. Features that need richer pagination
 * should define their own response DTO.
 */
@Serializable
data class OffsetPageResponse<T>(
    val list: List<T> = emptyList(),
    @SerialName("has_more") val hasMoreRaw: Int = 0,
    val total: Int? = null,
) {
    val hasMore: Boolean get() = hasMoreRaw == 1
}
