package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.TranslationSubmitRequestDto
import com.example.demoproject.platform.data.network.dto.TranslationSubmitResponseDto
import com.example.demoproject.platform.network.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TranslationApi {
    @POST("translation/submit")
    suspend fun submitTranslation(
        @Body body: TranslationSubmitRequestDto,
    ): ApiResponse<TranslationSubmitResponseDto>
}
