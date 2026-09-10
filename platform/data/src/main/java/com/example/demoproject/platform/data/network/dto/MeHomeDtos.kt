package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeMyResponseDto(
    @SerialName("user_info") val userInfo: UserDto = UserDto(),
    val media: HomeInfoMediaPageDto = HomeInfoMediaPageDto(),
    val views: HomeMyViewsDto = HomeMyViewsDto(),
    val social: List<HomeInfoSocialDto> = emptyList(),
    val tags: List<HomeMyTagDto> = emptyList(),
    @SerialName("account_info") val accountInfo: HomeMyAccountInfoDto = HomeMyAccountInfoDto(),
)

@Serializable
data class HomeMyViewsDto(
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("total_num") val totalNum: Int = 0,
    val list: List<HomeMyViewUserDto> = emptyList(),
)

@Serializable
data class HomeMyViewUserDto(
    @Serializable(with = LenientLongSerializer::class)
    val uid: Long = 0,
    val avatar: String = "",
)

@Serializable
data class HomeMyTagDto(
    @Serializable(with = LenientLongSerializer::class)
    val id: Long = 0,
    val name: String = "",
)

@Serializable
data class HomeMyAccountInfoDto(
    @Serializable(with = LenientIntSerializer::class)
    val money: Int = 0,
)

@Serializable
data class NewVisitorRequestDto(
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_me") val isMe: Int = 1,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_need_fans_new") val isNeedFansNew: Int = 1,
    @Serializable(with = LenientIntSerializer::class)
    val limit: Int = 3,
)

@Serializable
data class NewVisitorSummaryDto(
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("visitor_new") val visitorNew: Int = 0,
    @SerialName("visitor_list") val visitorList: List<NewVisitorUserDto> = emptyList(),
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("fans_new") val fansNew: Int = 0,
    @SerialName("fans_list") val fansList: List<NewFanUserDto> = emptyList(),
)

@Serializable
data class NewVisitorUserDto(
    @Serializable(with = LenientIntSerializer::class)
    val unread: Int = 0,
    @Serializable(with = LenientLongSerializer::class)
    val uid: Long = 0,
    @SerialName("small_avatar") val smallAvatar: String = "",
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("hide_profile") val hideProfile: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val sex: Int = 0,
    @SerialName("avatar_labels") val avatarLabels: String = "",
)

@Serializable
data class NewFanUserDto(
    @Serializable(with = LenientLongSerializer::class)
    val uid: Long = 0,
    @SerialName("small_avatar") val smallAvatar: String = "",
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("hide_profile") val hideProfile: Int = 0,
)
