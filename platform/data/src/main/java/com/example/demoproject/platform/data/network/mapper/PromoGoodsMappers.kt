package com.example.demoproject.platform.data.network.mapper

import com.example.demoproject.platform.data.network.dto.CoinProductDto
import com.example.demoproject.platform.data.network.dto.VipAlertCallbackDto
import com.example.demoproject.platform.data.network.dto.VipAlertFuncDataDto
import com.example.demoproject.platform.data.network.dto.VipAlertResponseDto
import com.example.demoproject.platform.data.network.toAssetUrlOrNull
import com.example.demoproject.platform.data.promotion.PromotionScheduleConstants
import com.example.demoproject.platform.data.promotion.TreasureProductSelector
import com.example.demoproject.platform.data.promotion.TreasureUserTier
import com.example.demoproject.platform.data.promotion.toOfferRemainSeconds
import com.example.demoproject.platform.data.repository.BillingProductType
import com.example.demoproject.platform.data.repository.PromoGoods
import com.example.demoproject.platform.data.repository.PromoGoodsContent
import com.example.demoproject.platform.data.repository.WinningOffer

fun VipAlertResponseDto.toPromoGoodsOrNull(
    tier: TreasureUserTier = TreasureUserTier.Unpaid,
): PromoGoods? {
    val callback = resolveAlertCallback() ?: return null
    if (!callback.isTreasureCallback()) return null
    return callback.toPromoGoodsOrNull(tier)
}

fun VipAlertCallbackDto.toPromoGoodsOrNull(
    tier: TreasureUserTier = TreasureUserTier.Unpaid,
): PromoGoods? {
    if (!isTreasureCallback()) return null
    val data = funcData ?: return null
    val remain = toOfferRemainSeconds(data.alertRemainTime)
    val selected = TreasureProductSelector.select(data, tier) ?: return null
    val content = data.toPromoGoodsContent(selected)
    return PromoGoods(
        remainSeconds = remain,
        originalPrice = selected.originalPrice,
        salePrice = selected.salePrice,
        content = content,
        goodsId = selected.goodsId,
        sku = selected.sku,
        productType = selected.productType,
        fromType = data.fromType.takeIf { it > 0 }
            ?: PromotionScheduleConstants.DEFAULT_TREASURE_FROM_TYPE,
        fromId = data.fromId,
        orderFrom = data.orderFrom,
        sid = data.sid,
        saveDesc = data.saveDesc,
    )
}

fun VipAlertResponseDto.toWinningOfferOrNull(
    forceWinningSkin: Boolean,
    tier: TreasureUserTier = TreasureUserTier.Unpaid,
): WinningOffer? {
    val callback = resolveAlertCallback() ?: return null
    return callback.toWinningOfferOrNull(forceWinningSkin, tier)
}

fun VipAlertCallbackDto.toWinningOfferOrNull(
    forceWinningSkin: Boolean,
    tier: TreasureUserTier = TreasureUserTier.Unpaid,
): WinningOffer? {
    val winningName = funcName.equals("winning_recharge_alert", ignoreCase = true)
    if (!forceWinningSkin && !winningName) return null
    val data = funcData ?: return null
    val remain = toOfferRemainSeconds(data.alertRemainTime)
    val selected = TreasureProductSelector.select(data, tier)
    val coin = data.firstCoinItem
    val base = selected?.coinAmount
        ?: coin?.diamond
        ?: 0
    val bonus = selected?.bonusCoins
        ?: coin?.giveCoins
        ?: coin?.extraGiveCoins
        ?: 0
    val sale = selected?.salePrice
        ?: coin?.moneyDesc?.takeIf { it.isNotBlank() }
        ?: ""
    val original = selected?.originalPrice
        ?: coin?.originalDesc?.takeIf { it.isNotBlank() }
        ?: sale
    val sku = selected?.sku.orEmpty()
    val goodsId = selected?.goodsId ?: coin?.id ?: 0L
    return WinningOffer(
        remainSeconds = remain,
        originalPrice = original.ifBlank { sale },
        salePrice = sale,
        baseCoins = base,
        bonusCoins = bonus,
        baseCoinIconUrl = selected?.coinIconUrl ?: coin?.coinIcon?.toAssetUrlOrNull(),
        bonusCoinIconUrl = selected?.coinIconUrl ?: coin?.coinIcon?.toAssetUrlOrNull(),
        goodsId = goodsId,
        sku = sku,
        productType = selected?.productType ?: BillingProductType.Coins,
        fromType = data.fromType,
        fromId = data.fromId,
        orderFrom = data.orderFrom,
        sid = data.sid.takeIf { it > 0 } ?: PromotionScheduleConstants.DEFAULT_WINNING_SID,
        hasPurchasableSku = sku.isNotBlank() && goodsId > 0L,
    )
}

fun VipAlertCallbackDto.isTreasureCallback(): Boolean {
    val name = funcName.trim()
    if (name.equals("vip_discount_alert", ignoreCase = true)) return true
    if (name.equals("recharge_alert", ignoreCase = true)) return true
    if (name.equals("winning_recharge_alert", ignoreCase = true)) return false
    if (name.isBlank() && funcData != null) {
        return TreasureProductSelector.select(funcData!!, TreasureUserTier.Unpaid) != null
    }
    return false
}

fun VipAlertResponseDto.resolveAlertCallback(): VipAlertCallbackDto? {
    listOfNotNull(vipAlert, callback).forEach { candidate ->
        if (candidate.funcData != null) return candidate
    }
    if (funcData != null) {
        return VipAlertCallbackDto(funcName = funcName, funcData = funcData)
    }
    // Bare goods on data root: treat empty func_name + this DTO's fields as callback.
    if (funcName.isBlank() && vipAlert == null && callback == null && funcData == null) {
        return null
    }
    return null
}

private fun VipAlertFuncDataDto.toPromoGoodsContent(
    selected: TreasureProductSelector.SelectedProduct,
): PromoGoodsContent {
    if (selected.isVip) {
        return PromoGoodsContent.Vip(
            vipTitle = selected.vipTitle,
            bonusCoins = selected.bonusCoins,
            matchCount = selected.matchCount,
            coinIconUrl = selected.coinIconUrl?.toAssetUrlOrNull()
                ?: selected.coinIconUrl,
        )
    }
    val dual = mergeList.filter { it.hasPromoCoins() }.take(2)
    if (dual.size >= 2) {
        return PromoGoodsContent.DualCoins(
            leftCoins = dual[0].promoCoinAmount(),
            rightCoins = dual[1].promoCoinAmount(),
            leftIconUrl = dual[0].coinIcon.toAssetUrlOrNull(),
            rightIconUrl = dual[1].coinIcon.toAssetUrlOrNull(),
            matchCount = maxOf(dual[0].match, dual[1].match, selected.matchCount),
        )
    }
    return PromoGoodsContent.SmallCoins(
        coins = selected.coinAmount,
        coinIconUrl = selected.coinIconUrl?.toAssetUrlOrNull() ?: selected.coinIconUrl,
    )
}

private fun CoinProductDto.hasPromoCoins(): Boolean =
    diamond > 0 || giveCoins > 0 || extraGiveCoins > 0

private fun CoinProductDto.promoCoinAmount(): Int =
    when {
        diamond > 0 -> diamond
        giveCoins > 0 -> giveCoins
        else -> extraGiveCoins
    }
