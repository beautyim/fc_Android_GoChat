package com.example.demoproject.platform.data.network.mapper

import com.example.demoproject.platform.data.network.dto.CoinIndexResponseDto
import com.example.demoproject.platform.data.network.dto.CoinProductDto
import com.example.demoproject.platform.data.network.dto.CoinVipPayItemDto
import com.example.demoproject.platform.data.network.dto.VipAlertCallbackDto
import com.example.demoproject.platform.data.network.dto.VipAlertFuncDataDto
import com.example.demoproject.platform.data.network.toAssetUrlOrNull
import com.example.demoproject.platform.data.repository.RechargePageData
import com.example.demoproject.platform.data.repository.RechargeProduct
import com.example.demoproject.platform.data.repository.RechargeSaleItem
import com.example.demoproject.platform.data.repository.RechargeVipPayItem
import com.example.demoproject.platform.data.repository.resolveRechargeCoinSpriteIndex
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun CoinIndexResponseDto.toRechargePageData(): RechargePageData {
    val hot = hotList
        .filter { it.isVisibleCoinProduct }
        .map { it.toRechargeProduct(isHot = true) }
    val regular = payList
        .filter { it.isVisibleCoinProduct }
        .map { it.toRechargeProduct(isHot = it.isHot == 1) }
    val sales = saleList
        .orEmpty()
        .filter { it.isVisibleCoinProduct }
        .mapIndexed { index, item -> item.toRechargeSaleItem(index) }
    val vipCandidates = vipPayItems + (payList + hotList + saleList.orEmpty())
        .mapNotNull { it.toCoinVipPayItemOrNull() }
    return RechargePageData(
        balance = balance,
        hotProducts = hot,
        products = regular,
        saleItems = sales,
        vipPayItem = resolveRechargeVipPayItem(vipPayItem, vipCandidates),
    )
}

fun VipAlertCallbackDto.toRechargePageData(): RechargePageData? {
    if (funcName != "recharge_alert") return null
    return funcData?.toRechargePageData()?.takeUnless { it.isEmpty }
}

fun VipAlertFuncDataDto.toRechargePageData(): RechargePageData {
    val hot = hotList
        .filterNot { it.hidden == 1 }
        .map { it.toRechargeProduct(isHot = true) }
    val regular = coinPayItems
        .filterNot { it.hidden == 1 }
        .map { it.toRechargeProduct(isHot = it.isHot == 1) }
    val sales = resolveAlertSaleItems()
    val vipCandidates = vipPayItems + (coinPayItems + hotList + saleList.orEmpty())
        .mapNotNull { it.toCoinVipPayItemOrNull() }
    return RechargePageData(
        balance = balance,
        hotProducts = hot,
        products = regular,
        saleItems = sales,
        vipPayItem = resolveRechargeVipPayItem(vipPayItem, vipCandidates),
    )
}

/**
 * Alert payloads may omit `sale_list` and instead provide a single `sale_pay_item`
 * that represents the lowest promotional tier the user should be guided to buy.
 */
internal fun VipAlertFuncDataDto.resolveAlertSaleItems(): List<RechargeSaleItem> {
    val fromSaleList = saleList.orEmpty().filterNot { it.hidden == 1 }
    if (fromSaleList.isNotEmpty()) {
        return fromSaleList.mapIndexed { index, item -> item.toRechargeSaleItem(index) }
    }
    val promoItem = salePayItem?.takeIf { it.hidden != 1 } ?: return emptyList()
    return listOf(promoItem.toRechargeSaleItem(styleIndex = 0))
}

internal fun CoinProductDto.toRechargeProduct(isHot: Boolean): RechargeProduct {
    val hasDiscount = original > 0.0 && original > money
    return RechargeProduct(
        id = id,
        sku = sku,
        productType = productType,
        coinAmount = diamond,
        price = moneyDesc.ifBlank { formatRechargeMoney(money, currencyUnit) },
        originalPrice = if (hasDiscount) {
            originalDesc.ifBlank { formatRechargeMoney(original, currencyUnit) }
        } else {
            null
        },
        saleLabel = saleDesc?.takeIf { it.isNotBlank() }
            ?: saveRate?.takeIf { it.isNotBlank() }?.let { rate ->
                if (rate.contains('%', ignoreCase = true)) rate else "$rate%"
            },
        labelType = if (isHot && labelType == 0) 1 else labelType,
        iconUrl = coinIcon.toAssetUrlOrNull(),
        coinIndex = resolveRechargeCoinSpriteIndex(coinIcon, coinIndex),
    )
}

internal fun CoinProductDto.toRechargeSaleItem(styleIndex: Int): RechargeSaleItem {
    val bonusCoins = giveCoins
    val hasDiscount = original > 0.0 && original > money
    return RechargeSaleItem(
        id = id,
        sku = sku,
        productType = productType,
        baseCoins = diamond,
        bonusCoins = bonusCoins,
        totalCoins = diamond + bonusCoins,
        price = moneyDesc.ifBlank { formatRechargeMoney(money, currencyUnit) },
        originalPrice = if (hasDiscount) {
            originalDesc.ifBlank { formatRechargeMoney(original, currencyUnit) }
        } else {
            null
        },
        discountRate = saleDesc?.takeIf { it.isNotBlank() }
            ?: saveRate?.takeIf { it.isNotBlank() },
        styleIndex = styleIndex,
        iconUrl = coinIcon.toAssetUrlOrNull(),
        coinIndex = resolveRechargeCoinSpriteIndex(coinIcon, coinIndex),
        showBonusAsMatch = false,
        matchCount = match,
        // This mapper lives outside Compose and can't resolve string resources, so pass the
        // blank case through as null and let the UI layer localize the fallback text.
        superDiscountLabel = label.takeIf { it.isNotBlank() },
    )
}

internal fun CoinVipPayItemDto.toRechargeVipPayItem(): RechargeVipPayItem? {
    if (hidden == 1 || (id <= 0L && sku.isBlank())) return null
    return RechargeVipPayItem(
        id = id,
        sku = sku,
        productType = productType.takeIf { it > 0 } ?: 2,
        title = title,
        price = moneyDesc.ifBlank { formatRechargeMoney(money, currencyUnit) },
        giveCoins = resolvedGiveCoins(),
        matchCount = resolvedMatchCount(),
    )
}

internal fun CoinProductDto.toRechargeVipPayItemOrNull(): RechargeVipPayItem? =
    toCoinVipPayItemOrNull()?.toRechargeVipPayItem()

private val CoinProductDto.isVisibleCoinProduct: Boolean
    get() = hidden != 1 &&
        productType == 1 &&
        isVip != 1 &&
        vipType.isBlank() &&
        !sku.contains("vip", ignoreCase = true) &&
        !title.contains("vip", ignoreCase = true)

private fun CoinProductDto.toCoinVipPayItemOrNull(): CoinVipPayItemDto? {
    val looksLikeVip = productType == 2 ||
        isVip == 1 ||
        isSuper == 1 ||
        vipType.isNotBlank() ||
        sku.contains("vip", ignoreCase = true) ||
        title.contains("vip", ignoreCase = true)
    if (hidden == 1 || !looksLikeVip || (id <= 0L && sku.isBlank())) return null
    return CoinVipPayItemDto(
        id = id,
        sku = sku,
        productType = productType,
        title = title,
        money = money,
        moneyDesc = moneyDesc,
        saleDesc = saleDesc.orEmpty(),
        label = label,
        labelType = labelType,
        currencyUnit = currencyUnit,
        originalDesc = originalDesc,
        giveCoins = giveCoins,
        saveRate = saveRate.orEmpty(),
        saleIcon = saleIcon,
        hidden = hidden,
        isVip = isVip,
        vipType = vipType,
        match = match,
    )
}

internal fun resolveRechargeVipPayItem(
    vipPayItem: CoinVipPayItemDto?,
    vipPayItems: List<CoinVipPayItemDto>,
): RechargeVipPayItem? =
    resolveVipPayItemDto(vipPayItem, vipPayItems)?.toRechargeVipPayItem()

fun resolveVipPayItemDto(
    vipPayItem: CoinVipPayItemDto?,
    vipPayItems: List<CoinVipPayItemDto>,
): CoinVipPayItemDto? {
    val items = vipPayItems.filter { it.isVisibleVipCandidate }
    if (vipPayItem == null && items.isEmpty()) return null
    if (vipPayItem == null) return items.firstOrNull()
    val matched = items.firstOrNull { candidate ->
        candidate.id == vipPayItem.id ||
            (candidate.sku.isNotBlank() && candidate.sku == vipPayItem.sku)
    }
    return matched ?: vipPayItem
}

private val CoinVipPayItemDto.isVisibleVipCandidate: Boolean
    get() = hidden != 1 &&
        (
            productType == 2 ||
                isVip == 1 ||
                vipType.isNotBlank() ||
                sku.contains("vip", ignoreCase = true) ||
                title.contains("vip", ignoreCase = true)
            )

internal fun CoinVipPayItemDto.resolvedGiveCoins(): Int {
    if (giveCoins > 0) return giveCoins
    return extraRewards.giveCoinsOrDefault()
}

internal fun CoinVipPayItemDto.resolvedMatchCount(): Int {
    if (match > 0) return match
    return extraRewards.matchCountOrDefault()
}

internal fun kotlinx.serialization.json.JsonObject?.giveCoinsOrDefault(): Int {
    if (this == null) return 0
    val keys = listOf(
        "give_coins",
        "coins",
        "coin",
        "diamond",
    )
    keys.forEach { key ->
        this[key]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 }?.let { return it }
    }
    return 0
}

internal fun kotlinx.serialization.json.JsonObject?.matchCountOrDefault(): Int {
    if (this == null) return 1
    val keys = listOf(
        "match_count",
        "free_match_count",
        "match_free_count",
        "free_match",
        "match",
    )
    keys.forEach { key ->
        this[key]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 }?.let { return it }
    }
    return 1
}

internal fun formatRechargeMoney(amount: Double, currencyUnit: String): String {
    val unit = currencyUnit.currencySymbol()
    val normalized = if (amount % 1.0 == 0.0) {
        amount.toInt().toString()
    } else {
        String.format("%.2f", amount)
    }
    return "$unit$normalized"
}

internal fun String.currencySymbol(): String = when {
    isBlank() || this == "0" || this == "1" -> "$"
    startsWith("$") -> this
    else -> this
}
