package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.LenientDoubleSerializer
import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import com.example.demoproject.platform.data.network.serializer.JsonNumberOrStringAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GooglePayCheckRequestDto(
    @SerialName("goods_id")
    val goodsId: Long,
    @SerialName("product_type")
    val productType: Int,
    val type: Int,
)

@Serializable
data class GooglePayCheckResponseDto(
    @SerialName("pop_type")
    @Serializable(with = LenientIntSerializer::class)
    val popType: Int = 0,
    val pop: GooglePayMethodPopupDto? = null,
)

@Serializable
data class GooglePayMethodPopupDto(
    val title: String = "",
    val list: List<GooglePayMethodDto> = emptyList(),
)

@Serializable
data class GooglePayMethodDto(
    val title: String = "",
    val icon: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val type: Int = 0,
)

@Serializable
data class GooglePayCreateRequestDto(
    @SerialName("goods_id")
    val goodsId: Long,
    @SerialName("product_type")
    val productType: Int,
    val type: Int,
    @SerialName("from_type")
    val fromType: Int = 0,
    @SerialName("from_id")
    val fromId: Long = 0L,
    @SerialName("order_from")
    val orderFrom: Int = 0,
)

@Serializable
data class GooglePayCreateResponseDto(
    @SerialName("product_type")
    @Serializable(with = LenientIntSerializer::class)
    val productType: Int = 0,
    @SerialName("tran_no")
    val tranNo: String = "",
    @SerialName("product_id")
    val productId: String = "",
    @SerialName("pay_type")
    @Serializable(with = LenientIntSerializer::class)
    val payType: Int = 0,
    @SerialName("goods_id")
    @Serializable(with = LenientLongSerializer::class)
    val goodsId: Long = 0L,
    @Serializable(with = JsonNumberOrStringAsStringSerializer::class)
    val price: String = "",
    val currency: String = "",
    @SerialName("pay_url")
    val payUrl: String = "",
    val url: String = "",
    val link: String = "",
    @SerialName("pay_item")
    val payItem: GooglePayItemDto? = null,
    val callback: JsonObject? = null,
)

@Serializable
data class GooglePayItemDto(
    @SerialName("goods_id")
    @Serializable(with = LenientLongSerializer::class)
    val goodsId: Long = 0L,
    @Serializable(with = LenientLongSerializer::class)
    val id: Long = 0L,
    val sku: String = "",
    @SerialName("product_id")
    val productId: String = "",
    @SerialName("product_type")
    @Serializable(with = LenientIntSerializer::class)
    val productType: Int = 0,
    @SerialName("pay_type")
    @Serializable(with = LenientIntSerializer::class)
    val payType: Int = 0,
    @SerialName("money_desc")
    val moneyDesc: String = "",
    @Serializable(with = LenientDoubleSerializer::class)
    val money: Double = 0.0,
    @SerialName("currency_unit")
    @Serializable(with = JsonNumberOrStringAsStringSerializer::class)
    val currencyUnit: String = "",
    val currency: String = "",
)

@Serializable
data class GooglePayCancelRequestDto(
    @SerialName("tran_no")
    val tranNo: String,
    val errorCode: Int = 0,
    val googleCode: Int = 0,
)

@Serializable
data class GooglePayVerifyRequestDto(
    @SerialName("tran_no")
    val tranNo: String,
    @SerialName("order_id")
    val orderId: String,
    @SerialName("package_name")
    val packageName: String,
    @SerialName("purchase_token")
    val purchaseToken: String,
)

@Serializable
data class GooglePayEventAckRequestDto(
    @SerialName("event_id")
    val eventId: Long,
)
