package com.example.demoproject.product.store

import com.example.demoproject.platform.data.repository.RechargePageData
import com.example.demoproject.platform.data.repository.RechargeProduct
import com.example.demoproject.platform.data.repository.RechargeSaleItem
import com.example.demoproject.platform.data.repository.RechargeVipPayItem

fun RechargePageData.toCoinPayGuideUiState(
    fallbackSuperDiscountLabel: String,
    fromType: Int? = null,
    isLoading: Boolean = false,
): CoinPayGuideUiState =
    CoinPayGuideUiState(
        isLoading = isLoading,
        balance = balance,
        saleOffers = saleItems.map { it.toSaleUi(fallbackSuperDiscountLabel) },
        coinOffers = toCoinOffers(),
        fromType = fromType,
    )

fun RechargePageData.toCallHangupRechargeUiState(
    peerNickname: String,
    peerAge: Int,
    peerAvatarUrl: String,
    fallbackSuperDiscountLabel: String,
    fromType: Int? = null,
    isLoading: Boolean = false,
): CallHangupRechargeUiState =
    CallHangupRechargeUiState(
        isLoading = isLoading,
        peerNickname = peerNickname,
        peerAge = peerAge,
        peerAvatarUrl = peerAvatarUrl,
        saleOffers = saleItems.map { it.toSaleUi(fallbackSuperDiscountLabel) },
        coinOffers = toCoinOffers(),
        fromType = fromType,
    )

fun RechargePageData.vipCarouselItems(): List<RechargeVipPayItem> =
    vipPayItems.ifEmpty { listOfNotNull(vipPayItem) }

/** Regular coin grid excludes sale carousel SKUs. */
fun RechargePageData.toCoinOffers(): List<StoreCoinOfferUi> {
    val saleIds = saleItems.map { it.id }.toHashSet()
    return (hotProducts + products)
        .distinctBy { it.id }
        .filterNot { it.id in saleIds }
        .map { it.toUi() }
}

fun RechargeSaleItem.toSaleUi(fallbackBadge: String): StoreSaleOfferUi =
    StoreSaleOfferUi(
        id = id,
        sku = sku,
        baseCoins = baseCoins.takeIf { it > 0 } ?: totalCoins,
        bonusCoins = bonusCoins,
        price = price,
        originalPrice = originalPrice,
        discountPercent = discountRate?.toDiscountPercentOrNull(),
        badgeLabel = superDiscountLabel?.takeIf { it.isNotBlank() } ?: fallbackBadge,
    )

fun RechargeProduct.toUi(): StoreCoinOfferUi =
    StoreCoinOfferUi(
        id = id,
        sku = sku,
        coinAmount = coinAmount,
        price = price,
        originalPrice = originalPrice,
        discountLabel = saleLabel?.normalizeDiscountLabel(),
        cornerBadge = null,
        iconUrl = iconUrl,
    )

fun RechargeVipPayItem.toUi(): StoreVipOfferUi =
    StoreVipOfferUi(
        id = id,
        sku = sku,
        title = title,
        month = month,
        price = price,
        originalPrice = originalPrice,
        giveCoins = giveCoins,
        matchCount = matchCount,
        badgeLabel = badgeLabel,
    )

internal fun String.normalizeDiscountLabel(): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return trimmed
    return if (trimmed.contains('%', ignoreCase = true) ||
        trimmed.contains("off", ignoreCase = true)
    ) {
        trimmed
    } else {
        "$trimmed% OFF"
    }
}

/** Keeps only the percent token for the sale OFF ribbon (e.g. "72%"). */
internal fun String.toDiscountPercentOrNull(): String? {
    val digits = Regex("""\d+""").find(trim())?.value ?: return null
    return "$digits%"
}
