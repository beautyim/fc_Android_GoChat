package com.example.demoproject.platform.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Unified response envelope for the BerryCam backend.
 *
 * Contract (from the OpenAPI spec):
 *   - `status`: **1 = success**, any other value (commonly `0`) is a business failure.
 *   - `ok`:     newer endpoints may use **1 = success** instead of `status`.
 *   - `msg`:    human-readable failure message.
 *   - `result` / `data`: payload when successful.
 *   - `callback`: vip/recharge guide payload. Some endpoints put it on the
 *     envelope root (`call/create`); others nest it under `data`
 *     (`private-album/unlock`). The signing interceptor promotes a nested
 *     `data.callback` to the root when the root omits it so [SafeApiCall] can
 *     attach it to [com.example.demoproject.platform.network.result.AppResult.BizError].
 */
@Serializable
data class ApiResponse<T>(
    val status: Int = 0,
    val ok: Int? = null,
    val msg: String = "",
    val result: T? = null,
    val data: T? = null,
    val callback: JsonElement? = null,
) {
    val isSuccess: Boolean get() = status == SUCCESS_STATUS || ok == SUCCESS_STATUS
    val failureCode: Int get() = ok ?: status
    val payload: T? get() = result ?: data
    val businessMessage: String
        get() = msg.ifBlank {
            (payload as? ApiBusinessMessage)?.message.orEmpty()
        }

    companion object {
        const val SUCCESS_STATUS: Int = 1
    }
}

interface ApiBusinessMessage {
    val message: String
}
