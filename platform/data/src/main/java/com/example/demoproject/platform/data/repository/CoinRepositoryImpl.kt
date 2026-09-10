package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.network.api.CoinApi
import com.example.demoproject.platform.data.network.mapper.toRechargePageData
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.safeApiCall

class CoinRepositoryImpl(
    private val coinApi: CoinApi,
) : CoinRepository {
    override suspend fun getRechargePage(): AppResult<RechargePageData> =
        safeApiCall { coinApi.getCoinIndex() }
            .map { it.toRechargePageData() }
}
