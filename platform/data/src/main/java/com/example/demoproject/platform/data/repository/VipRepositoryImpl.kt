package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.network.api.VipApi
import com.example.demoproject.platform.data.network.dto.VipListResponseDto
import com.example.demoproject.platform.data.network.dto.VipPrivilegeDto
import com.example.demoproject.platform.data.network.dto.VipProductDto
import com.example.demoproject.platform.data.network.mapper.toDisplayLabelOrNull
import com.example.demoproject.platform.data.network.toAssetUrlOrNull
import com.example.demoproject.platform.data.network.toPicUrlOrNull
import com.example.demoproject.platform.data.vip.VipStatusStore
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.safeApiCall
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VipRepositoryImpl @Inject constructor(
    private val api: VipApi,
    private val vipStatusStore: VipStatusStore,
) : VipRepository {

    override suspend fun getVipPage(): AppResult<VipPageData> =
        safeApiCall { api.getVipList() }
            .map { dto ->
                val page = dto.toDomain()
                vipStatusStore.update(
                    isVip = page.user.isVip,
                    expiryText = page.user.expiryText,
                )
                page
            }
}

internal fun VipListResponseDto.toDomain(): VipPageData =
    VipPageData(
        user = VipUser(
            isVip = userInfo.isVip == 1,
            expiryText = userInfo.vipExp.displayString(),
            nickname = userInfo.nickname,
            avatarUrl = userInfo.avatar.toPicUrlOrNull(),
            sex = userInfo.sex,
        ),
        plans = list
            .filterNot { it.hidden == 1 }
            .map(VipProductDto::toDomain)
            .sortedByDescending { it.durationDays() },
        benefits = privilegeList.map(VipPrivilegeDto::toDomain),
    )

private fun VipProductDto.toDomain(): VipPlan {
    val resolvedBenefits = when {
        privilegeInfos.isNotEmpty() -> privilegeInfos
        else -> privilegeList?.list.orEmpty()
    }
    val originalResolved = originPrice.takeIf { it.isNotBlank() }
        ?: original.takeIf { it.isNotBlank() }
    val priceResolved = moneyDesc.ifBlank { money }
    return VipPlan(
        id = id,
        sku = sku,
        productType = productType.takeIf { it > 0 } ?: 2,
        title = title.trim(),
        days = days,
        month = month,
        dayDesc = dayDesc,
        price = priceResolved,
        originalPrice = originalResolved?.takeUnless { it == priceResolved },
        saleText = saleDesc.ifBlank {
            when {
                sale > 0 -> "$sale% OFF"
                saveRate.isNotBlank() -> {
                    val rate = saveRate.trim()
                    if (rate.contains('%', ignoreCase = true) ||
                        rate.contains("off", ignoreCase = true)
                    ) {
                        rate
                    } else {
                        val pct = rate.toDoubleOrNull()?.let { value ->
                            if (value in 0.0..1.0) (value * 100).toInt() else value.toInt()
                        }
                        pct?.let { "$it% OFF" }.orEmpty()
                    }
                }
                else -> ""
            }
        },
        label = label.toDisplayLabelOrNull().orEmpty(),
        hidden = hidden == 1,
        iconUrl = icon.toAssetUrlOrNull(),
        giveCoins = giveCoins,
        matchCount = match,
        benefits = resolvedBenefits.map(VipPrivilegeDto::toDomain),
    )
}

private fun VipPlan.durationDays(): Int =
    when {
        days > 0 -> days
        month > 0 -> month * 30
        else -> 0
    }

private fun VipPrivilegeDto.toDomain(): VipBenefit =
    VipBenefit(
        id = id,
        title = title,
        tips = tips,
        iconUrl = icon.toAssetUrlOrNull(),
    )

private val VIP_EXP_DATE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val VIP_EXP_DISPLAY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

/**
 * `vip_exp` may come back as a date string (e.g. "2026-12-31 23:59:59") or, in some
 * environments, as a raw Unix timestamp in seconds (e.g. 1745300000). Normalize both
 * shapes into a human-readable date so the VIP page never shows a raw number.
 */
private fun kotlinx.serialization.json.JsonElement?.displayString(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    val raw = primitive.contentOrNull?.takeIf { it.isNotBlank() && it != "0" } ?: return null
    val epochSeconds = if (primitive.isString) raw.toLongOrNull() else primitive.longOrNull
    if (epochSeconds != null) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault())
            .format(VIP_EXP_DISPLAY_FORMATTER)
    }
    val parsedDate = runCatching { LocalDateTime.parse(raw, VIP_EXP_DATE_TIME_FORMATTER).toLocalDate() }
        .recoverCatching { LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE) }
        .getOrNull()
    return parsedDate?.format(VIP_EXP_DISPLAY_FORMATTER) ?: raw
}
