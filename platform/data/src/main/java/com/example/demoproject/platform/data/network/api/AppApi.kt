package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.AppCloseRequestDto
import com.example.demoproject.platform.data.network.dto.AppInitResponseDto
import com.example.demoproject.platform.data.network.dto.AppOpenRequestDto
import com.example.demoproject.platform.data.network.dto.AppOpenResponseDto
import com.example.demoproject.platform.data.network.dto.AppStatResponseDto
import com.example.demoproject.platform.data.network.dto.AdjustAddRequestDto
import com.example.demoproject.platform.network.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AppApi {

    /** `/app/init` — initialize the current authenticated app session. */
    @POST("app/init")
    suspend fun init(): ApiResponse<AppInitResponseDto>

    /** `/app/open` — report the app entering foreground. */
    @POST("app/open")
    suspend fun open(@Body body: AppOpenRequestDto): ApiResponse<AppOpenResponseDto?>

    /** `/app/close` — report the app entering background. */
    @POST("app/close")
    suspend fun close(@Body body: AppCloseRequestDto = AppCloseRequestDto()): ApiResponse<Unit?>

    /** `/app/stat` — check whether the current device is a first-time install. */
    @POST("app/stat")
    suspend fun stat(): ApiResponse<AppStatResponseDto>

    /** `/adjust/add` — upload Adjust attribution metadata once per local app installation. */
    @POST("adjust/add")
    suspend fun addAdjustAttribution(@Body body: AdjustAddRequestDto): ApiResponse<Unit?>
}
