package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.MomentCreateRequestDto
import com.example.demoproject.platform.data.network.dto.MomentCreateResponseDto
import com.example.demoproject.platform.data.network.dto.MomentDeleteRequestDto
import com.example.demoproject.platform.data.network.dto.UserOperateLikeRequestDto
import com.example.demoproject.platform.network.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface PostApi {
    @POST("moment/create")
    suspend fun createMoment(@Body body: MomentCreateRequestDto): ApiResponse<MomentCreateResponseDto?>

    @POST("moment/delete")
    suspend fun deleteMoment(@Body body: MomentDeleteRequestDto): ApiResponse<Unit?>

    @POST("user-operate/like")
    suspend fun likeContent(@Body body: UserOperateLikeRequestDto): ApiResponse<Unit?>
}
