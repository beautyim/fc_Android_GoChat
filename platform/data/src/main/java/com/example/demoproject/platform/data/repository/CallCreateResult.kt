package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.CallRoom

sealed interface CallCreateResult {
    data class Success(val room: CallRoom) : CallCreateResult

    data class Failure(
        val message: String,
        val code: Int,
        val rechargePageData: RechargePageData? = null,
        val alert: CallCreateAlert? = null,
    ) : CallCreateResult
}
