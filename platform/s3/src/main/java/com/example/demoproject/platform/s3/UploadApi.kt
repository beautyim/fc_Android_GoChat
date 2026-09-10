package com.example.demoproject.platform.s3

import com.example.demoproject.platform.network.dto.ApiResponse
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface UploadApi {
    @POST("upload/handle")
    suspend fun getUploadParams(@Body body: GetUploadParamsRequestDto): ApiResponse<UploadParamsDto>

    @POST
    suspend fun getUploadParamsByUrl(
        @Url url: String,
        @Body body: GetUploadParamsRequestDto,
    ): ApiResponse<UploadParamsDto>

    @POST
    suspend fun getUploadParamsByUrlRaw(
        @Url url: String,
        @Body body: JsonElement,
    ): ApiResponse<JsonElement?>

    @POST("upload/success")
    suspend fun uploadSuccess(@Body body: UploadSuccessRequestDto): ApiResponse<Unit?>
}
