package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GiftHistoryRequestDto(
    val page: Int = 1,
    @SerialName("home_uid") val homeUid: Long = 0,
)

@Serializable
data class GiftHistoryInfoDto(
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("gift_id") val giftId: Long = 0,
    val title: String = "",
    val icon: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val price: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val count: Int = 0,
)

@Serializable
data class GiftSentRecordDto(
    @Serializable(with = LenientLongSerializer::class)
    val uid: Long = 0,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("to_uid") val toUid: Long = 0,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("gift_id") val giftId: Long = 0,
    @SerialName("sign_time") val signTime: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val number: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_sign") val isSign: Int = 0,
)

@Serializable
data class GiftSentListResponseDto(
    val list: List<GiftSentRecordDto> = emptyList(),
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("has_more") val hasMore: Int = 0,
    @SerialName("gift_info") val giftInfo: Map<String, GiftHistoryInfoDto> = emptyMap(),
)

@Serializable
data class GiftReceivedRecordDto(
    @Serializable(with = LenientLongSerializer::class)
    val uid: Long = 0,
    @Serializable(with = LenientIntSerializer::class)
    val number: Int = 0,
    @SerialName("gift_info") val giftInfo: GiftHistoryInfoDto = GiftHistoryInfoDto(),
)

@Serializable
data class GiftReceivedListResponseDto(
    val list: List<GiftReceivedRecordDto> = emptyList(),
    @SerialName("gift_list") val giftList: List<GiftHistoryInfoDto> = emptyList(),
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("has_more") val hasMore: Int = 0,
)
