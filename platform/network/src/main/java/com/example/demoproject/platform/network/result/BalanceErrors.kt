package com.example.demoproject.platform.network.result

/** Match / call create and other legacy balance errors. */
const val INSUFFICIENT_BALANCE_ERROR_CODE = 2

/** `POST msg/send` returns `ok = 6` for `FEE_MSG_NO_BALANCE`. */
const val MSG_SEND_INSUFFICIENT_BALANCE_ERROR_CODE = 6
/** `POST msg/send` returns `ok = 3` for `FREE_MSG_USED_UP_NOT_VIP`. */
const val MSG_SEND_REQUIRE_VIP_ERROR_CODE = 3

private val INSUFFICIENT_BALANCE_ERROR_CODES = setOf(
    INSUFFICIENT_BALANCE_ERROR_CODE,
    MSG_SEND_INSUFFICIENT_BALANCE_ERROR_CODE,
)

fun AppResult.Failure.isInsufficientBalance(): Boolean =
    when (this) {
        is AppResult.BizError ->
            code in INSUFFICIENT_BALANCE_ERROR_CODES ||
                message.contains("not enough stars", ignoreCase = true) ||
                message.contains("insufficient balance", ignoreCase = true) ||
                (
                    message.contains("insufficient", ignoreCase = true) &&
                        message.contains("star", ignoreCase = true)
                    )
        else -> false
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
