package com.example.demoproject.platform.data.promotion

import com.example.demoproject.platform.data.network.dto.CoinProductDto
import com.example.demoproject.platform.data.network.dto.CoinVipPayItemDto
import com.example.demoproject.platform.data.network.dto.VipAlertFuncDataDto
import com.example.demoproject.platform.data.network.mapper.isVisibleVipCandidate
import com.example.demoproject.platform.data.network.mapper.toRechargeVipPayItem
import com.example.demoproject.platform.data.repository.BillingProductType

/**
 * Picks one purchasable SKU from promo `func_data` buckets by user tier (doc §8).
 */
object TreasureProductSelector {

    data class SelectedProduct(
        val goodsId: Long,
        val sku: String,
        val productType: BillingProductType,
        val salePrice: String,
        val originalPrice: String,
        val coinAmount: Int,
        val bonusCoins: Int,
        val matchCount: Int,
        val vipTitle: String,
        val coinIconUrl: String?,
        val isVip: Boolean,
    )

    fun select(
        data: VipAlertFuncDataDto,
        tier: TreasureUserTier,
    ): SelectedProduct? {
        val vipItems = collectVip(data)
        val coinItems = collectCoins(data)
        return when (tier) {
            TreasureUserTier.Unpaid ->
                cheapestCoin(coinItems) ?: vipItems.firstOrNull()?.toSelected()
            TreasureUserTier.PaidNonVip,
            TreasureUserTier.VipExpired,
            ->
                vipItems.firstOrNull()?.toSelected() ?: cheapestCoin(coinItems)
            TreasureUserTier.PaidVip ->
                nthCheapestCoin(coinItems, index = 2)
                    ?: cheapestCoin(coinItems)
                    ?: vipItems.firstOrNull()?.toSelected()
        }
    }

    private fun collectVip(data: VipAlertFuncDataDto): List<CoinVipPayItemDto> {
        // `func_data.list` is shared by VIP and coin payloads; only keep real VIP rows.
        val fromItem = data.vipPayItem?.takeIf { it.isVisibleVipCandidate }
        val fromList = data.vipPayItems.filter { it.isVisibleVipCandidate }
        return buildList {
            if (fromItem != null) add(fromItem)
            addAll(fromList)
        }.distinctBy { it.id.takeIf { id -> id > 0L } ?: it.sku }
    }

    private fun collectCoins(data: VipAlertFuncDataDto): List<CoinProductDto> {
        val buckets = buildList {
            data.firstCoinItem?.let { add(it) }
            addAll(data.mergeList)
            addAll(data.saleList.orEmpty())
            addAll(data.hotList)
            addAll(data.coinPayItems)
        }
        return buckets
            .filter { it.diamond > 0 || it.giveCoins > 0 || it.extraGiveCoins > 0 }
            .filter { it.sku.isNotBlank() || it.id > 0L }
            .distinctBy { it.id.takeIf { id -> id > 0L } ?: it.sku }
    }

    private fun cheapestCoin(items: List<CoinProductDto>): SelectedProduct? =
        items.minByOrNull { it.money }?.toSelectedCoin()

    private fun nthCheapestCoin(items: List<CoinProductDto>, index: Int): SelectedProduct? {
        val sorted = items.sortedBy { it.money }
        return sorted.getOrNull(index)?.toSelectedCoin() ?: sorted.lastOrNull()?.toSelectedCoin()
    }

    private fun CoinVipPayItemDto.toSelected(): SelectedProduct? {
        val mapped = toRechargeVipPayItem() ?: return null
        return SelectedProduct(
            goodsId = mapped.id,
            sku = mapped.sku,
            productType = BillingProductType.Vip,
            salePrice = mapped.price,
            originalPrice = mapped.originalPrice ?: mapped.price,
            coinAmount = 0,
            bonusCoins = mapped.giveCoins,
            matchCount = mapped.matchCount,
            vipTitle = formatTreasureVipTitle(month = mapped.month, title = mapped.title),
            // VIP pay item may omit coin_icon; UI shows local coin glyph when null.
            coinIconUrl = coinIcon.takeIf { it.isNotBlank() },
            isVip = true,
        )
    }

    private fun CoinProductDto.toSelectedCoin(): SelectedProduct {
        val sale = moneyDesc.ifBlank { "$${"%.2f".format(money)}" }
        val original = originalDesc.ifBlank {
            if (original > money && original > 0.0) "$${"%.2f".format(original)}" else sale
        }
        return SelectedProduct(
            goodsId = id,
            sku = sku,
            productType = BillingProductType.Coins,
            salePrice = sale,
            originalPrice = original,
            coinAmount = when {
                diamond > 0 -> diamond
                giveCoins > 0 -> giveCoins
                else -> extraGiveCoins
            },
            bonusCoins = giveCoins.takeIf { diamond > 0 } ?: extraGiveCoins,
            matchCount = match,
            vipTitle = "",
            coinIconUrl = coinIcon.takeIf { it.isNotBlank() },
            isVip = false,
        )
    }
}

/** e.g. month=1 + title="Week" → "1 Week VIP". */
internal fun formatTreasureVipTitle(month: Int, title: String): String {
    val period = title.trim()
    return when {
        period.isBlank() && month > 0 -> "$month Month VIP"
        period.isBlank() -> "VIP"
        month > 0 -> "$month $period VIP"
        else -> "$period VIP"
    }
}
