package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.MatchEndRequestDto
import com.example.demoproject.platform.data.network.dto.MatchInfoDto
import com.example.demoproject.platform.data.network.dto.MatchStartRequestDto
import com.example.demoproject.platform.network.dto.ApiResponse
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST

interface MatchApi {
    @POST("match/start")
    suspend fun start(@Body body: MatchStartRequestDto): ApiResponse<JsonElement?>

    @POST("match/end")
    suspend fun end(@Body body: MatchEndRequestDto): ApiResponse<Unit?>

    @POST("match/info")
    suspend fun info(@Body body: List<String> = emptyList()): ApiResponse<MatchInfoDto>

    @POST("match/close")
    suspend fun close(): ApiResponse<Unit?>

    @POST("match/next")
    suspend fun next(@Body body: MatchStartRequestDto): ApiResponse<JsonElement?>
}
