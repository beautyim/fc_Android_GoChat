package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.FeedsListRequestDto
import com.example.demoproject.platform.data.network.dto.FeedsListResponseDto
import com.example.demoproject.platform.data.network.dto.MomentListRequestDto
import com.example.demoproject.platform.network.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit bindings for `/feeds/list` and `/moment/list` per the Spicy API doc.
 */
interface FeedApi {

    /** `/feeds/list` — main home feed, cursor-paged by `last_id`. */
    @POST("feeds/list")
    suspend fun getFeeds(@Body body: FeedsListRequestDto): ApiResponse<FeedsListResponseDto>

    /** `/moment/list` — posts by a specific user. */
    @POST("moment/list")
    suspend fun getMomentList(@Body body: MomentListRequestDto): ApiResponse<FeedsListResponseDto>
}
