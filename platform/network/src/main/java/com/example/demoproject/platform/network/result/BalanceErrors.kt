package com.example.demoproject.platform.network.result

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Match / call create and other legacy balance errors. */
const val INSUFFICIENT_BALANCE_ERROR_CODE = 2

/** `POST msg/send` returns `ok = 6` for `FEE_MSG_NO_BALANCE`. */
const val MSG_SEND_INSUFFICIENT_BALANCE_ERROR_CODE = 6
/** `POST msg/send` returns `ok = 3` for `FREE_MSG_USED_UP_NOT_VIP`. */
const val MSG_SEND_REQUIRE_VIP_ERROR_CODE = 3

/**
 * Mid-tier `NOT_ENOUGH_MONEY` for private-album unlock; Spicy gateway may surface
 * this as `ok=0` with a recharge `callback`, or preserve `1013`.
 */
const val PRIVATE_ALBUM_NOT_ENOUGH_MONEY_ERROR_CODE = 1013

private val INSUFFICIENT_BALANCE_ERROR_CODES = setOf(
    INSUFFICIENT_BALANCE_ERROR_CODE,
    MSG_SEND_INSUFFICIENT_BALANCE_ERROR_CODE,
    PRIVATE_ALBUM_NOT_ENOUGH_MONEY_ERROR_CODE,
)

fun AppResult.Failure.isInsufficientBalance(): Boolean =
    when (this) {
        is AppResult.BizError ->
            code in INSUFFICIENT_BALANCE_ERROR_CODES ||
                message.contains("not enough stars", ignoreCase = true) ||
                message.contains("insufficient balance", ignoreCase = true) ||
                message.contains("insufficient account balance", ignoreCase = true) ||
                message.contains("NOT_ENOUGH_MONEY", ignoreCase = true) ||
                (
                    message.contains("insufficient", ignoreCase = true) &&
                        message.contains("balance", ignoreCase = true)
                    ) ||
                (
                    message.contains("insufficient", ignoreCase = true) &&
                        message.contains("star", ignoreCase = true)
                    ) ||
                // private-album/unlock often returns ok=0 + recharge_alert callback only
                (code == 0 && callback.isRechargeAlert())
        else -> false
    }

private fun JsonElement?.isRechargeAlert(): Boolean {
    val obj = this as? JsonObject ?: return false
    val name = (obj["func_name"] as? JsonPrimitive)?.content.orEmpty()
    return name.equals("recharge_alert", ignoreCase = true)
}

fun AppResult.Failure.isMsgSendRequireVip(): Boolean =
    when (this) {
        is AppResult.BizError ->
            code == MSG_SEND_REQUIRE_VIP_ERROR_CODE ||
                message.contains("FREE_MSG_USED_UP_NOT_VIP", ignoreCase = true) ||
                (
                    message.contains("free", ignoreCase = true) &&
                        message.contains("vip", ignoreCase = true)
                    )
        else -> false
    }
