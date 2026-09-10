package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.VipListResponseDto
import com.example.demoproject.platform.network.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface VipApi {

    /** `vip/list` — VIP products, user VIP status and page privileges. */
    @POST("vip/list")
    suspend fun getVipList(@Body body: List<String> = emptyList()): ApiResponse<VipListResponseDto>
}
