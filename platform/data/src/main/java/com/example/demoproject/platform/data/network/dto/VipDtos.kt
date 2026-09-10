package com.example.demoproject.platform.data.network.dto

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

@Serializable
data class VipProductDto(
    val id: Long = 0,
    val sku: String = "",
    val title: String = "",
    val days: Int = 0,
    val month: Int = 0,
    val money: String = "",
    @SerialName("money_desc")
    val moneyDesc: String = "",
    @SerialName("product_type")
    val productType: Int = 0,
    val total: String = "",
    @SerialName("sale_desc")
    val saleDesc: String = "",
    val sale: Int = 0,
    @SerialName("day_desc")
    val dayDesc: String = "",
    val label: String = "",
    @SerialName("currency_unit")
    val currencyUnit: Int = 0,
    val original: String = "",
    @SerialName("origin_price")
    val originPrice: String = "",
    val hidden: Int = 0,
    @SerialName("save_rate")
    val saveRate: String = "",
    val icon: String = "",
    @SerialName("privilege_infos")
    val privilegeInfos: List<VipPrivilegeDto> = emptyList(),
)

@Serializable
data class VipPrivilegeDto(
    val id: Int = 0,
    val title: String = "",
    val tips: String = "",
    val icon: String = "",
)
