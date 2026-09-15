package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class VipListResponseDto(
    @SerialName("user_info")
    val userInfo: VipUserInfoDto = VipUserInfoDto(),
    val list: List<VipProductDto> = emptyList(),
    @SerialName("privilege_list")
    val privilegeList: List<VipPrivilegeDto> = emptyList(),
)

@Serializable
data class VipUserInfoDto(
    @SerialName("is_vip")
    val isVip: Int = 0,
    @SerialName("vip_exp")
    val vipExp: JsonElement? = null,
    val nickname: String = "",
    val avatar: String = "",
    val sex: Int = 0,
)

/**
 * `vip/list` product item. Gateway formats [money] as a `$…` string and mirrors
 * [original] into [originPrice]. Per-product [privilegeInfos] drive the benefit strip
 * on BerryCam / Omichat; [privilegeList] is a legacy shared envelope.
 */
@Serializable
data class VipProductDto(
    @Serializable(with = LenientLongSerializer::class)
    val id: Long = 0,
    val sku: String = "",
    val title: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val days: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val month: Int = 0,
    /** Gateway-formatted price, e.g. `"$9.99"`. */
    val money: String = "",
    @SerialName("money_desc")
    val moneyDesc: String = "",
    @SerialName("product_type")
    @Serializable(with = LenientIntSerializer::class)
    val productType: Int = 0,
    val total: String = "",
    @SerialName("sale_desc")
    val saleDesc: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val sale: Int = 0,
    @SerialName("day_desc")
    val dayDesc: String = "",
    val label: String = "",
    @SerialName("currency_unit")
    @Serializable(with = LenientIntSerializer::class)
    val currencyUnit: Int = 0,
    val original: String = "",
    @SerialName("origin_price")
    val originPrice: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val hidden: Int = 0,
    @SerialName("save_rate")
    val saveRate: String = "",
    val icon: String = "",
    @SerialName("give_coins")
    @Serializable(with = LenientIntSerializer::class)
    val giveCoins: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val match: Int = 0,
    @SerialName("privilege_list")
    val privilegeList: VipProductPrivilegeListDto? = null,
    @SerialName("privilege_infos")
    val privilegeInfos: List<VipPrivilegeDto> = emptyList(),
)

@Serializable
data class VipProductPrivilegeListDto(
    val list: List<VipPrivilegeDto> = emptyList(),
)

@Serializable
data class VipPrivilegeDto(
    @Serializable(with = LenientIntSerializer::class)
    val id: Int = 0,
    val title: String = "",
    val tips: String = "",
    val icon: String = "",
)
