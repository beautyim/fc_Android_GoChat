package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.network.dto.ApiResponse
import com.example.demoproject.platform.data.network.dto.CallBlurRequestDto
import com.example.demoproject.platform.data.network.dto.CallCreateDataDto
import com.example.demoproject.platform.data.network.dto.CallCreateRequestDto
import com.example.demoproject.platform.data.network.dto.CallEndRequestDto
import com.example.demoproject.platform.data.network.dto.CallMessageRequestDto
import com.example.demoproject.platform.data.network.dto.CallRecordsRequestDto
import com.example.demoproject.platform.data.network.dto.CallRecordsResponseDto
import com.example.demoproject.platform.data.network.dto.CallRoomIdRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

interface CallApi {
    @POST("call/records")
    suspend fun records(@Body body: CallRecordsRequestDto): ApiResponse<CallRecordsResponseDto>

    @POST("call/create")
    suspend fun create(@Body body: CallCreateRequestDto): ApiResponse<CallCreateDataDto?>

    @POST("call/success")
    suspend fun success(@Body body: CallRoomIdRequestDto): ApiResponse<Unit?>

    @POST("call/end")
    suspend fun end(@Body body: CallEndRequestDto): ApiResponse<Unit?>

    @POST("call/heart")
    suspend fun heart(@Body body: CallRoomIdRequestDto): ApiResponse<Unit?>

    @POST("call/mask-status")
    suspend fun maskStatus(@Body body: CallBlurRequestDto): ApiResponse<Unit?>

    @POST("call/send-msg")
    suspend fun sendMsg(@Body body: CallMessageRequestDto): ApiResponse<Unit?>
}
