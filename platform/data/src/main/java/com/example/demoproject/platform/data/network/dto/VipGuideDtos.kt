package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Envelope `callback` for `msg/send` when `ok = 3` (`FREE_MSG_USED_UP_NOT_VIP`).
 *
 * Wire shape (flat, not `func_name` / `func_data`):
 * ```
 * {"vip_list":[{"privilege_infos":[...],...}],"coach_info":{...},"from_type":2}
 * ```
 *
 * Benefit chips use each plan's `privilege_infos`. `banner_new` is ignored for display.
 */
@Serializable
data class VipGuideCallbackDto(
    @SerialName("vip_list")
    val vipList: List<CoinVipPayItemDto> = emptyList(),
    /** Legacy field — kept for decode compatibility; UI does not consume it. */
    @SerialName("banner_new")
    val bannerNew: List<VipGuideBannerDto> = emptyList(),
    @SerialName("coach_info")
    val coachInfo: VipGuideCoachDto? = null,
    @SerialName("coach_latest_msg")
    val coachLatestMsg: String = "",
    @SerialName("from_type")
    @Serializable(with = LenientIntSerializer::class)
    val fromType: Int = 0,
)

@Serializable
data class VipGuideBannerDto(
    val icon: String = "",
    val title: String = "",
)

@Serializable
data class VipGuideCoachDto(
    val nickname: String = "",
    val avatar: String = "",
    @SerialName("small_avatar")
    val smallAvatar: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val uid: Int = 0,
)

private val vipGuideJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun JsonElement?.toVipGuideCallbackDtoOrNull(): VipGuideCallbackDto? {
    if (this == null) return null
    return runCatching { vipGuideJson.decodeFromJsonElement<VipGuideCallbackDto>(this) }.getOrNull()
}
