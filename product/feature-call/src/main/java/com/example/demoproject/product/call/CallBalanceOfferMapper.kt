package com.example.demoproject.product.call

import com.example.demoproject.platform.callkit.signaling.SignalingCoinOffer
import com.example.demoproject.platform.callkit.signaling.SignalingEvent
import com.example.demoproject.platform.data.repository.RechargeProduct
import com.example.demoproject.platform.data.repository.RechargeSaleItem
import com.example.demoproject.platform.data.repository.RechargeVipPayItem
import com.example.demoproject.product.store.StoreSaleOfferUi
import com.example.demoproject.product.store.StoreVipOfferUi
import com.example.demoproject.product.store.toSaleUi
import com.example.demoproject.product.store.toUi

/**
 * Maps MQTT `a_type=7` SKUs into a [CallBalanceOffer].
 * Prefer explicit [SignalingEvent.BalanceAlert.vipPayItem], then VIP-typed [payItem].
 */
fun SignalingEvent.BalanceAlert.toCallBalanceOffer(
    liveRemainingSeconds: Int,
    isFreeCall: Boolean,
    fallbackSaleBadge: String,
): CallBalanceOffer? {
    val vipSource = vipPayItem ?: payItem?.takeIf { it.isVipProduct }
    if (vipSource != null) {
        val vip = vipSource.toRechargeVipPayItem() ?: return null
        val ui = vip.toUi()
        return CallBalanceOffer(
            roomKey = roomKey,
            variant = CallBalanceOfferVariant.NonVip,
            remainingSeconds = liveRemainingSeconds,
            price = ui.price,
            originalPrice = ui.originalPrice,
            offPercent = vipSource.offPercentOrNull(),
            giveCoins = ui.giveCoins,
            matchCount = ui.matchCount,
            baseCoins = 0,
            vipOffer = ui,
            isVipPurchase = true,
            coinSku = ui.sku,
            coinGoodsId = ui.id,
        )
    }

    val saleSource = salePayItem ?: payItem
    if (saleSource == null) return null
    val sale = saleSource.toRechargeSaleItem() ?: return null
    val saleUi = sale.toSaleUi(fallbackSaleBadge)
    val variant = when {
        isFreeCall || sale.bonusCoins <= 0 -> CallBalanceOfferVariant.Free
        else -> CallBalanceOfferVariant.Vip
    }
    return CallBalanceOffer(
        roomKey = roomKey,
        variant = variant,
        remainingSeconds = liveRemainingSeconds,
        price = saleUi.price,
        originalPrice = saleUi.originalPrice,
        offPercent = saleUi.discountPercent ?: saleSource.offPercentOrNull(),
        giveCoins = sale.bonusCoins,
        matchCount = sale.matchCount,
        baseCoins = sale.baseCoins.takeIf { it > 0 } ?: sale.totalCoins,
        saleOffer = saleUi,
        isVipPurchase = false,
        coinSku = sale.sku,
        coinGoodsId = sale.id,
    )
}

fun SignalingCoinOffer.toRechargeVipPayItem(): RechargeVipPayItem? {
    if (id <= 0L && sku.isBlank()) return null
    val resolvedMonth = when {
        month > 0 -> month
        days >= 28 -> (days / 30).coerceAtLeast(1)
        days > 0 -> 1
        else -> 1
    }
    return RechargeVipPayItem(
        id = id,
        sku = sku,
        productType = 2,
        title = title.ifBlank { "Month" },
        month = resolvedMonth,
        price = moneyDesc,
        originalPrice = originalDesc.takeIf { it.isNotBlank() },
        giveCoins = giveCoins.takeIf { it > 0 } ?: diamond,
        matchCount = matchCount,
        badgeLabel = label.takeIf { it.isNotBlank() },
    )
}

fun SignalingCoinOffer.toRechargeSaleItem(): RechargeSaleItem? {
    if (id <= 0L && sku.isBlank()) return null
    return RechargeSaleItem(
        id = id,
        sku = sku,
        productType = productType.coerceAtLeast(1),
        baseCoins = diamond,
        bonusCoins = giveCoins,
        totalCoins = diamond + giveCoins,
        price = moneyDesc,
        originalPrice = originalDesc.takeIf { it.isNotBlank() },
        discountRate = saveRate.ifBlank { saleDesc }.takeIf { it.isNotBlank() },
        styleIndex = 0,
        iconUrl = null,
        coinIndex = 0,
        showBonusAsMatch = matchCount > 0,
        matchCount = matchCount,
        superDiscountLabel = label.takeIf { it.isNotBlank() },
    )
}

fun SignalingCoinOffer.toRechargeProduct(): RechargeProduct? {
    if (id <= 0L && sku.isBlank()) return null
    return RechargeProduct(
        id = id,
        sku = sku,
        productType = productType.coerceAtLeast(1),
        coinAmount = diamond,
        price = moneyDesc,
        originalPrice = originalDesc.takeIf { it.isNotBlank() },
        saleLabel = saleDesc.takeIf { it.isNotBlank() },
        labelType = 0,
        iconUrl = null,
        coinIndex = 0,
    )
}

private fun SignalingCoinOffer.offPercentOrNull(): String? {
    val raw = saveRate.ifBlank { saleDesc }.trim()
    if (raw.isEmpty()) return null
    val digits = Regex("""\d+""").find(raw)?.value ?: return null
    return "$digits%"
}

fun CallBalanceOffer.toGuideVipOffers(): List<StoreVipOfferUi> =
    listOfNotNull(vipOffer)

fun CallBalanceOffer.toGuideSaleOffers(): List<StoreSaleOfferUi> =
    listOfNotNull(saleOffer)
