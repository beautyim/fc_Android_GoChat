package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.FirebaseTokenRequestDto
import com.example.demoproject.platform.network.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface FirebaseApi {

    /** `firebase/token` — upload the current Firebase Cloud Messaging token. */
    @POST("firebase/token")
    suspend fun uploadToken(@Body body: FirebaseTokenRequestDto): ApiResponse<Unit?>
}
