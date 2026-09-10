package com.example.demoproject.platform.data.repository

sealed interface MatchStartResult {
    data class Success(val info: MatchStartInfo) : MatchStartResult

    data class Failure(
        val message: String,
        val code: Int,
        val rechargePageData: RechargePageData? = null,
    ) : MatchStartResult
}
