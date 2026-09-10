package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.network.api.VipApi
import com.example.demoproject.platform.data.network.dto.VipListResponseDto
import com.example.demoproject.platform.data.network.dto.VipPrivilegeDto
import com.example.demoproject.platform.data.network.dto.VipProductDto
import com.example.demoproject.platform.data.network.toAssetUrlOrNull
import com.example.demoproject.platform.data.network.toPicUrlOrNull
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
) : VipRepository {

    override suspend fun getVipPage(): AppResult<VipPageData> =
        safeApiCall { api.getVipList() }
            .map(VipListResponseDto::toDomain)
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

private fun VipProductDto.toDomain(): VipPlan =
    VipPlan(
        id = id,
        sku = sku,
        productType = productType.takeIf { it > 0 } ?: 2,
        title = displayTitle(),
        days = days,
        month = month,
        dayDesc = dayDesc,
        price = moneyDesc.ifBlank { money },
        saleText = saleDesc.ifBlank { saveRate },
        label = label.displayLabel(),
        hidden = hidden == 1,
        iconUrl = icon.toAssetUrlOrNull(),
        benefits = privilegeInfos.map(VipPrivilegeDto::toDomain),
    )

private fun VipProductDto.displayTitle(): String =
    when {
        title.isNotBlank() -> title
        days == 7 -> "1 Week"
        days in 1..13 -> if (days == 1) "1 Day" else "$days Days"
        month > 1 -> "$month Months"
        month == 1 -> "1 Month"
        days > 1 -> "$days Days"
        else -> dayDesc
    }

private fun String.displayLabel(): String {
    val lower = trim().lowercase()
    if (lower.isBlank()) return ""
    return lower.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }
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
