package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.CoinIndexResponseDto
import com.example.demoproject.platform.data.network.dto.VipAlertResponseDto
import com.example.demoproject.platform.data.network.dto.VipEventRequestDto
import com.example.demoproject.platform.network.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface CoinApi {

    /** `coin/index` — coin recharge page catalog and balance. */
    @POST("coin/index")
    suspend fun getCoinIndex(@Body body: List<String> = emptyList()): ApiResponse<CoinIndexResponseDto>

    /** `vip/event` — report reward (winning) popup scenes and consume server callback payloads when returned inline. */
    @POST("vip/event")
    suspend fun reportVipEvent(@Body body: VipEventRequestDto): ApiResponse<VipAlertResponseDto?>

    /**
     * `promo/goods` — fetch treasure box (discount/VIP) goods. The server decides eligibility and
     * content on its own; the request carries no parameters and none of the client's fields are
     * forwarded, so [body] is always an empty object. The response reuses the same
     * `func_name`/`func_data` (`vip_discount_alert`/`recharge_alert`) envelope as `vip/event` and
     * `vip/get-alert`.
     */
    @POST("promo/goods")
    suspend fun getPromoGoods(@Body body: Map<String, String> = emptyMap()): ApiResponse<VipAlertResponseDto?>
}
