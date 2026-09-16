package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.GooglePayCancelRequestDto
import com.example.demoproject.platform.data.network.dto.GooglePayCheckRequestDto
import com.example.demoproject.platform.data.network.dto.GooglePayCreateRequestDto
import com.example.demoproject.platform.data.network.dto.GooglePayCreateResponseDto
import com.example.demoproject.platform.data.network.dto.GooglePayCheckResponseDto
import com.example.demoproject.platform.data.network.dto.GooglePayEventAckRequestDto
import com.example.demoproject.platform.data.network.dto.GooglePayVerifyRequestDto
import com.example.demoproject.platform.network.dto.ApiResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST

interface GooglePayApi {

    /** `google/check` — validate that the requested payment type is allowed for this product. */
    @POST("google/check")
    suspend fun check(@Body body: GooglePayCheckRequestDto): ApiResponse<GooglePayCheckResponseDto>

    /** `google/create` — create a Google Play order before launching BillingClient. */
    @POST("google/create")
    suspend fun create(@Body body: GooglePayCreateRequestDto): ApiResponse<GooglePayCreateResponseDto>

    /** `google/cancel` — retire an order that cannot continue through BillingClient. */
    @POST("google/cancel")
    suspend fun cancel(@Body body: GooglePayCancelRequestDto): ApiResponse<Unit?>

    /** `google/verify` — verify the Play purchase token and deliver entitlement. */
    @POST("google/verify")
    suspend fun verify(@Body body: GooglePayVerifyRequestDto): ApiResponse<JsonObject?>

    /** `google/event-ack` — acknowledge a server-delivered payment conversion event. */
    @POST("google/event-ack")
    suspend fun acknowledgeEvent(@Body body: GooglePayEventAckRequestDto): ApiResponse<Unit?>
}
