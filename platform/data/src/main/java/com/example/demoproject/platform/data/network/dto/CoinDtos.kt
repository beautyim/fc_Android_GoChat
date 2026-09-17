package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.data.network.serializer.CoinSaleListSerializer
import com.example.demoproject.platform.data.network.serializer.JsonNumberOrStringAsStringSerializer
import com.example.demoproject.platform.network.serializer.LenientDoubleSerializer
import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class CoinIndexResponseDto(
    val balance: Int = 0,
    @SerialName("pay_list")
    val payList: List<CoinProductDto> = emptyList(),
    @SerialName("hot_list")
    val hotList: List<CoinProductDto> = emptyList(),
    @Serializable(with = CoinSaleListSerializer::class)
    @SerialName("sale_list")
    val saleList: List<CoinProductDto>? = null,
    @SerialName("vip_pay_item")
    val vipPayItem: CoinVipPayItemDto? = null,
    val list: List<JsonObject> = emptyList(),
) {
    val vipPayItems: List<CoinVipPayItemDto>
        get() = list.decodeVipPayItems()
}

@Serializable
data class CoinProductDto(
    val id: Long = 0,
    val sku: String = "",
    @SerialName("product_type")
    val productType: Int = 1,
    val money: Double = 0.0,
    @SerialName("money_desc")
    val moneyDesc: String = "",
    val original: Double = 0.0,
    @SerialName("original_desc")
    val originalDesc: String = "",
    @SerialName("sale_desc")
    val saleDesc: String? = null,
    @SerialName("save_desc")
    val saveDesc: String? = null,
    @SerialName("save_rate")
    val saveRate: String? = null,
    val diamond: Int = 0,
    val title: String = "",
    @Serializable(with = JsonNumberOrStringAsStringSerializer::class)
    val unit: String = "",
    @SerialName("currency_unit")
    @Serializable(with = JsonNumberOrStringAsStringSerializer::class)
    val currencyUnit: String = "",
    @SerialName("is_hot")
    val isHot: Int = 0,
    @SerialName("label_type")
    val labelType: Int = 0,
    val label: String = "",
    val hidden: Int = 0,
    @SerialName("coin_icon")
    val coinIcon: String = "",
    @SerialName("sale_icon")
    val saleIcon: String = "",
    @SerialName("is_single")
    val isSingle: Int = 0,
    @SerialName("is_vip")
    val isVip: Int = 0,
    @SerialName("vip_type")
    val vipType: String = "",
    @SerialName("coin_index")
    val coinIndex: Int = 0,
    @SerialName("give_coins")
    val giveCoins: Int = 0,
    @SerialName("extra_give_coins")
    val extraGiveCoins: Int = 0,
    val match: Int = 0,
    @SerialName("is_super")
    @Serializable(with = LenientIntSerializer::class)
    val isSuper: Int = 0,
    @SerialName("is_discount")
    @Serializable(with = LenientIntSerializer::class)
    val isDiscount: Int = 0,
)

@Serializable
data class CoinVipPayItemDto(
    @Serializable(with = LenientLongSerializer::class)
    val id: Long = 0,
    val sku: String = "",
    @SerialName("product_type")
    @Serializable(with = LenientIntSerializer::class)
    val productType: Int = 2,
    val title: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val days: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val month: Int = 0,
    @Serializable(with = LenientDoubleSerializer::class)
    val money: Double = 0.0,
    @SerialName("money_desc")
    val moneyDesc: String = "",
    val total: String = "",
    @SerialName("sale_desc")
    val saleDesc: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val sale: Int = 0,
    @SerialName("day_desc")
    val dayDesc: String = "",
    val label: String = "",
    @SerialName("label_type")
    @Serializable(with = LenientIntSerializer::class)
    val labelType: Int = 0,
    @SerialName("currency_unit")
    @Serializable(with = JsonNumberOrStringAsStringSerializer::class)
    val currencyUnit: String = "",
    @Serializable(with = JsonNumberOrStringAsStringSerializer::class)
    val original: String = "",
    @SerialName("original_desc")
    val originalDesc: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val spins: Int = 0,
    @SerialName("give_coins")
    @Serializable(with = LenientIntSerializer::class)
    val giveCoins: Int = 0,
    @SerialName("coin_icon")
    val coinIcon: String = "",
    @SerialName("save_rate")
    val saveRate: String = "",
    @SerialName("sale_icon")
    val saleIcon: String = "",
    @SerialName("sale_money")
    val saleMoney: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val hidden: Int = 0,
    @SerialName("is_vip")
    @Serializable(with = LenientIntSerializer::class)
    val isVip: Int = 0,
    @SerialName("vip_type")
    val vipType: String = "",
    @SerialName("extra_rewards")
    val extraRewards: JsonObject? = null,
    @Serializable(with = LenientIntSerializer::class)
    val match: Int = 0,
    @SerialName("vip_alert_remain_time")
    @Serializable(with = LenientLongSerializer::class)
    val vipAlertRemainTime: Long = 0L,
    /** Present on `msg/send` VIP-guide `vip_list` items; ignored elsewhere. */
    @SerialName("privilege_infos")
    val privilegeInfos: List<VipPrivilegeDto> = emptyList(),
)

@Serializable
data class VipEventRequestDto(
    @Serializable(with = LenientIntSerializer::class)
    val sid: Int,
    val type: Int? = null,
    @SerialName("target_uid")
    val targetUid: Long? = null,
    val extra: String? = null,
    @SerialName("need_price")
    val needPrice: Int? = null,
    @SerialName("from_id")
    val fromId: Long? = null,
)

@Serializable
data class VipAlertResponseDto(
    @SerialName("func_name")
    val funcName: String = "",
    @SerialName("func_data")
    val funcData: VipAlertFuncDataDto? = null,
    @SerialName("vip_alert")
    val vipAlert: VipAlertCallbackDto? = null,
    val callback: VipAlertCallbackDto? = null,
)

@Serializable
data class VipAlertCallbackDto(
    @SerialName("func_name")
    val funcName: String = "",
    @SerialName("func_data")
    val funcData: VipAlertFuncDataDto? = null,
)

@Serializable
data class VipAlertFuncDataDto(
    @Serializable(with = LenientIntSerializer::class)
    val origin: Int = 0,
    @SerialName("alert_remain_time")
    @Serializable(with = LenientLongSerializer::class)
    val alertRemainTime: Long = 0L,
    @SerialName("save_desc")
    val saveDesc: String = "",
    @SerialName("vip_pay_item")
    val vipPayItem: CoinVipPayItemDto? = null,
    @SerialName("hot_list")
    val hotList: List<CoinProductDto> = emptyList(),
    val balance: Int = 0,
    @SerialName("diff_coin")
    val diffCoin: Int = 0,
    @SerialName("msg")
    val message: String = "",
    val title: String = "",
    @SerialName("enter_title")
    val enterTitle: String = "",
    val list: List<JsonObject> = emptyList(),
    @SerialName("pay_item")
    val payItem: CoinProductDto? = null,
    @SerialName("coin_pay_item")
    val coinPayItem: CoinProductDto? = null,
    @SerialName("coin_item")
    val coinItem: CoinProductDto? = null,
    val item: CoinProductDto? = null,
    @SerialName("sale_pay_item")
    val salePayItem: CoinProductDto? = null,
    @SerialName("merge_list")
    val mergeList: List<CoinProductDto> = emptyList(),
    @Serializable(with = CoinSaleListSerializer::class)
    @SerialName("sale_list")
    val saleList: List<CoinProductDto>? = null,
    @Serializable(with = LenientIntSerializer::class)
    val sid: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val type: Int = 0,
    @SerialName("from_id")
    @Serializable(with = LenientLongSerializer::class)
    val fromId: Long = 0L,
    @SerialName("from_type")
    @Serializable(with = LenientIntSerializer::class)
    val fromType: Int = 0,
    @SerialName("order_from")
    @Serializable(with = LenientIntSerializer::class)
    val orderFrom: Int = 0,
) {
    val vipPayItems: List<CoinVipPayItemDto>
        get() = list.decodeVipPayItems()

    val coinPayItems: List<CoinProductDto>
        get() = list.mapNotNull { element ->
            runCatching { vipAlertJson.decodeFromJsonElement<CoinProductDto>(element) }.getOrNull()
        }

    val firstCoinItem: CoinProductDto?
        get() = listOfNotNull(payItem, coinPayItem, coinItem, item, salePayItem)
            .firstOrNull { it.isValidCoinProduct }
            ?: mergeList.firstOrNull { it.isValidCoinProduct }
            ?: saleList?.firstOrNull { it.isValidCoinProduct }
            ?: coinPayItems.firstOrNull { it.isValidCoinProduct }
}

private val vipAlertJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * Some endpoints (e.g. `call/create`) place the recharge-guide callback on the
 * response envelope root rather than nesting it inside `data`. This decodes that
 * raw [JsonElement] into the same [VipAlertCallbackDto] shape used elsewhere.
 */
fun JsonElement?.toVipAlertCallbackDtoOrNull(): VipAlertCallbackDto? {
    if (this == null) return null
    return runCatching { vipAlertJson.decodeFromJsonElement<VipAlertCallbackDto>(this) }.getOrNull()
}

private fun List<JsonObject>.decodeVipPayItems(): List<CoinVipPayItemDto> =
    mapNotNull { element ->
        runCatching { vipAlertJson.decodeFromJsonElement<CoinVipPayItemDto>(element) }.getOrNull()
    }

private val CoinProductDto.isValidCoinProduct: Boolean
    get() = diamond > 0 || giveCoins > 0 || extraGiveCoins > 0
