package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.network.result.AppResult

/**
 * Domain-side checkout helper: create server order and persist for retry/verify.
 * Google Play BillingClient / purchase UI remains app-layer (brand-specific).
 */
class BillingCheckout(
    private val billingRepository: BillingRepository,
    private val orderStore: BillingOrderStore,
) {
    suspend fun createAndStoreOrder(request: StorePurchaseRequest): AppResult<GooglePayOrder> {
        val check = billingRepository.checkPaymentType(request)
        if (check is AppResult.Failure) return check
        return when (val created = billingRepository.createOrder(request)) {
            is AppResult.Failure -> created
            is AppResult.Success -> {
                val order = created.data
                if (order.tranNo.isNotBlank()) {
                    orderStore.upsert(
                        StoredBillingOrder(
                            uiId = request.uiId,
                            tranNo = order.tranNo,
                            productId = order.productId,
                            productTypeApi = order.productType.apiValue,
                            payTypeApi = order.payType.apiValue,
                            goodsId = order.goodsId,
                            price = order.price,
                            currency = order.currency,
                            status = BillingOrderStatus.Created,
                        ),
                    )
                }
                created
            }
        }
    }

    suspend fun markPurchased(
        tranNo: String,
        playOrderId: String,
        packageName: String,
        purchaseToken: String,
    ): StoredBillingOrder? =
        orderStore.update(tranNo) { order ->
            order.copy(
                status = BillingOrderStatus.Purchased,
                playOrderId = playOrderId,
                packageName = packageName,
                purchaseToken = purchaseToken,
            )
        }

    suspend fun verifyStoredOrder(tranNo: String): AppResult<Unit> {
        val order = orderStore.allOrders().firstOrNull { it.tranNo == tranNo }
            ?: return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, "Order not found")
        if (order.purchaseToken.isBlank() || order.playOrderId.isBlank()) {
            return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, "Missing Play purchase fields")
        }
        return when (
            val verified = billingRepository.verifyOrder(
                tranNo = order.tranNo,
                orderId = order.playOrderId,
                packageName = order.packageName,
                purchaseToken = order.purchaseToken,
            )
        ) {
            is AppResult.Success -> {
                orderStore.update(tranNo) { it.copy(status = BillingOrderStatus.Verified) }
                verified
            }
            is AppResult.Failure -> verified
        }
    }
}
