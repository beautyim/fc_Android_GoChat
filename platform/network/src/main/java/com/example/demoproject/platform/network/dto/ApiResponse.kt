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
 *   - `callback`: some endpoints (e.g. `call/create`) put the vip/recharge guide
 *     callback on the envelope root instead of nesting it inside `data`.
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
