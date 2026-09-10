package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.MsgNoticeListRequestDto
import com.example.demoproject.platform.data.network.dto.MsgNoticeListResponseDto
import com.example.demoproject.platform.network.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface NotificationApi {
    @POST("msg/notice")
    suspend fun getClubNotices(@Body body: MsgNoticeListRequestDto): ApiResponse<MsgNoticeListResponseDto>
}
