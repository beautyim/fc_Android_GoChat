package com.example.demoproject.platform.data.network.mapper

import com.example.demoproject.platform.data.network.dto.CoinVipPayItemDto
import com.example.demoproject.platform.data.network.dto.VipGuideCallbackDto
import com.example.demoproject.platform.data.network.dto.VipGuideCoachDto
import com.example.demoproject.platform.data.network.dto.VipPrivilegeDto
import com.example.demoproject.platform.data.network.dto.toVipGuideCallbackDtoOrNull
import com.example.demoproject.platform.data.network.toAssetUrlOrNull
import com.example.demoproject.platform.data.network.toPicUrlOrNull
import com.example.demoproject.platform.data.repository.VipGuidePageData
import com.example.demoproject.platform.data.repository.VipGuidePlan
import com.example.demoproject.platform.data.repository.VipGuidePrivilege
import kotlinx.serialization.json.JsonElement

fun JsonElement?.toVipGuidePageDataOrNull(): VipGuidePageData? =
    toVipGuideCallbackDtoOrNull()?.toVipGuidePageData()?.takeUnless { it.isEmpty }

fun VipGuideCallbackDto.toVipGuidePageData(): VipGuidePageData {
    val plans = vipList
        .filterNot { it.hidden == 1 }
        .mapNotNull { it.toVipGuidePlanOrNull() }
        .sortedByDescending { it.durationDays() }
    return VipGuidePageData(
        plans = plans,
        peerNickname = coachInfo?.nickname.orEmpty(),
        peerAvatarUrl = coachInfo?.resolvedAvatarUrl(),
        fromType = fromType,
    )
}

private fun CoinVipPayItemDto.toVipGuidePlanOrNull(): VipGuidePlan? {
    if (id <= 0L && sku.isBlank()) return null
    val priceResolved = moneyDesc.ifBlank { formatRechargeMoney(money, currencyUnit) }
    val originalAmount = parseRechargeAmount(original)
        ?: parseRechargeAmount(originalDesc)
    val hasNumericDiscount = originalAmount != null && originalAmount > money && originalAmount > 0.0
    val originalDisplay = original.trim().takeIf { it.isNotBlank() }
        ?: originalDesc.trim().takeIf { it.isNotBlank() }
    val resolvedOriginal = when {
        originalDisplay != null &&
            !originalDisplay.equals(priceResolved, ignoreCase = true) &&
            (hasNumericDiscount || originalAmount == null) -> originalDisplay
        hasNumericDiscount -> formatRechargeMoney(originalAmount!!, currencyUnit)
        else -> null
    }
    return VipGuidePlan(
        id = id,
        sku = sku,
        productType = productType.takeIf { it > 0 } ?: 2,
        title = title.trim(),
        month = month,
        days = days,
        price = priceResolved,
        originalPrice = resolvedOriginal,
        label = label.toDisplayLabelOrNull().orEmpty(),
        giveCoins = giveCoins,
        matchCount = match,
        privileges = privilegeInfos
            .mapNotNull { it.toVipGuidePrivilegeOrNull() }
            .sortedBy { it.id },
    )
}

private fun VipPrivilegeDto.toVipGuidePrivilegeOrNull(): VipGuidePrivilege? {
    val titleResolved = title.trim()
    if (titleResolved.isBlank()) return null
    return VipGuidePrivilege(
        id = id,
        title = titleResolved,
        iconUrl = icon.toAssetUrlOrNull(),
    )
}

private fun VipGuideCoachDto.resolvedAvatarUrl(): String? =
    smallAvatar.toPicUrlOrNull() ?: avatar.toPicUrlOrNull()

private fun VipGuidePlan.durationDays(): Int =
    when {
        days > 0 -> days
        month > 0 -> month * 30
        else -> 0
    }
