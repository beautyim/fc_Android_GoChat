package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.network.result.AppResult
import kotlinx.serialization.json.JsonObject

enum class BillingProductType(val apiValue: Int, val uiPrefix: String) {
    Coins(apiValue = 1, uiPrefix = "stars"),
    Vip(apiValue = 2, uiPrefix = "vip"),
    PrivatePhoto(apiValue = 3, uiPrefix = "credit_photo"),
    PrivateVideo(apiValue = 4, uiPrefix = "credit_video"),
    FlashChat(apiValue = 5, uiPrefix = "flash_chat"),
    ;

    companion object {
        fun fromApi(value: Int): BillingProductType =
            entries.firstOrNull { it.apiValue == value } ?: Coins
    }
}

enum class BillingPayType(val apiValue: Int) {
    InApp(apiValue = 1),
    Subscription(apiValue = 2),
    ;

    companion object {
        fun forProduct(productType: BillingProductType): BillingPayType =
            if (productType == BillingProductType.Vip) Subscription else InApp
    }
}

enum class BillingPaymentType(val apiValue: Int) {
    GooglePlay(apiValue = 1),
    ThirdParty(apiValue = 2),
}

data class StorePurchaseRequest(
    val uiId: String,
    val goodsId: Long,
    val productId: String,
    val productType: BillingProductType,
    val paymentType: BillingPaymentType = BillingPaymentType.GooglePlay,
    val fromType: Int = 0,
    val fromId: Long = 0L,
    val orderFrom: Int = 0,
) {
    val payType: BillingPayType get() = BillingPayType.forProduct(productType)
}

sealed interface StorePurchaseResult {
    data class Success(val purchasedId: String) : StorePurchaseResult
    data object Canceled : StorePurchaseResult
    data class Failed(val message: String) : StorePurchaseResult
}

data class GooglePayOrder(
    val tranNo: String,
    val productId: String,
    val productType: BillingProductType,
    val payType: BillingPayType,
    val goodsId: Long,
    val price: String,
    val currency: String,
    val callback: JsonObject? = null,
)

interface BillingRepository {
    suspend fun checkPaymentType(request: StorePurchaseRequest): AppResult<Unit>
    suspend fun createOrder(request: StorePurchaseRequest): AppResult<GooglePayOrder>
    suspend fun cancelOrder(
        tranNo: String,
        appErrorCode: Int,
        googleCode: Int,
    ): AppResult<Unit>

    suspend fun verifyOrder(
        tranNo: String,
        orderId: String,
        packageName: String,
        purchaseToken: String,
    ): AppResult<Unit>
}

fun StorePurchaseRequest.storedUiId(): String =
    "${productType.uiPrefix}_$goodsId"
