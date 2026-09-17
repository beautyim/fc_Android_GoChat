package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.network.api.CoinApi
import com.example.demoproject.platform.data.network.dto.VipEventRequestDto
import com.example.demoproject.platform.data.network.mapper.toPromoGoodsOrNull
import com.example.demoproject.platform.data.network.mapper.toRechargePageData
import com.example.demoproject.platform.data.network.mapper.toWinningOfferOrNull
import com.example.demoproject.platform.data.promotion.TreasureUserTier
import com.example.demoproject.platform.data.wallet.AccountBalanceStore
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.safeApiCall
import com.example.demoproject.platform.network.safeApiCallNullable

class CoinRepositoryImpl(
    private val coinApi: CoinApi,
    private val accountBalanceStore: AccountBalanceStore,
) : CoinRepository {
    override suspend fun getRechargePage(): AppResult<RechargePageData> =
        safeApiCall { coinApi.getCoinIndex() }
            .map { dto ->
                val page = dto.toRechargePageData()
                accountBalanceStore.update(page.balance)
                page
            }

    override suspend fun getPromoGoods(tier: TreasureUserTier): AppResult<PromoGoods?> =
        safeApiCallNullable { coinApi.getPromoGoods() }
            .map { dto -> dto?.toPromoGoodsOrNull(tier) }

    override suspend fun reportVipEvent(
        sid: Int,
        needPrice: Int,
        fromId: Long,
        forceWinningSkin: Boolean,
        tier: TreasureUserTier,
    ): AppResult<WinningOffer?> =
        safeApiCallNullable {
            coinApi.reportVipEvent(
                VipEventRequestDto(
                    sid = sid,
                    needPrice = needPrice,
                    fromId = fromId.takeIf { it > 0L },
                ),
            )
        }.map { dto -> dto?.toWinningOfferOrNull(forceWinningSkin, tier) }
}
