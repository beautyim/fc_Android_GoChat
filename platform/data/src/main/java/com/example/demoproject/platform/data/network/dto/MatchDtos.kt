package com.example.demoproject.platform.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val MATCH_TYPE_VIDEO: Int = 1
/** Backend gender filter — male. */
const val MATCH_SEX_MALE: Int = 1
/** Backend gender filter — female (Match tab default). */
const val MATCH_SEX_FEMALE: Int = 2

@Serializable
data class MatchStartRequestDto(
    @SerialName("match_type") val matchType: Int = MATCH_TYPE_VIDEO,
    @SerialName("match_sex") val matchSex: Int = MATCH_SEX_FEMALE,
    @SerialName("support_ack") val supportAck: Int = 1,
    val caps: MatchCapabilitiesDto = MatchCapabilitiesDto(),
)

@Serializable
data class MatchCapabilitiesDto(
    val amv: Int = 1,
)

@Serializable
data class MatchNextRequestDto(
    @SerialName("match_type") val matchType: Int = MATCH_TYPE_VIDEO,
    @SerialName("match_sex") val matchSex: Int = MATCH_SEX_FEMALE,
)

@Serializable
data class MatchEndRequestDto(
    @SerialName("match_session_id") val matchSessionId: Long = 0,
)

@Serializable
data class MatchHeartRequestDto(
    @SerialName("match_session_id") val matchSessionId: Long,
)

@Serializable
data class MatchInfoDto(
    @SerialName("match_free_count") val matchFreeCount: Int = 0,
    @SerialName("free_user_match_price") val freeUserMatchPrice: Int = 0,
    @SerialName("pay_user_match_price") val payUserMatchPrice: Int = 0,
    @SerialName("vip_match_price") val vipMatchPrice: Int = 0,
    @SerialName("avatar_list") val avatarList: List<String> = emptyList(),
)

@Serializable
data class MatchStartResponseDto(
    @SerialName("match_session_id") val matchSessionId: Long? = null,
    @SerialName("match_id") val matchId: Long? = null,
    @SerialName("match_free_count") val matchFreeCount: Int = 0,
    val user: UserDto? = null,
)
