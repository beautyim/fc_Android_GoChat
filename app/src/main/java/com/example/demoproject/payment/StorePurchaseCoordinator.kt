package com.example.demoproject.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.demoproject.BuildConfig
import com.example.demoproject.platform.analytics.AnalyticsEvent
import com.example.demoproject.platform.analytics.AnalyticsTracker
import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.repository.BillingOrderStatus
import com.example.demoproject.platform.data.repository.BillingOrderStore
import com.example.demoproject.platform.data.repository.BillingPayType
import com.example.demoproject.platform.data.repository.BillingPaymentType
import com.example.demoproject.platform.data.repository.BillingPaymentCheck
import com.example.demoproject.platform.data.repository.BillingProductType
import com.example.demoproject.platform.data.repository.BillingRepository
import com.example.demoproject.platform.data.repository.GooglePayOrder
import com.example.demoproject.platform.data.repository.StorePurchaseRequest
import com.example.demoproject.platform.data.repository.StorePurchaseResult
import com.example.demoproject.platform.data.repository.StoredBillingOrder
import com.example.demoproject.platform.data.vip.VipStatusStore
import com.example.demoproject.platform.network.result.AppResult
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private const val TAG = "StorePurchaseCoordinator"
private const val STALE_CREATED_ORDER_MS = 2 * 60 * 1000L
private const val FAKE_PURCHASE_TOKEN_PREFIX = "debug-fake-"

class StorePurchaseCoordinator(
    context: Context,
    private val repository: BillingRepository,
    private val orderStore: BillingOrderStore,
    private val onPurchaseVerified: (() -> Unit)? = null,
    /**
     * Single, canonical "payment succeeded" hook: every verified purchase — whether it just
     * completed in [handleCompletedPurchase]/[completeFakePurchase] or was confirmed later by
     * [BillingOrderRetryWorker]'s background retry — flows through [verifyStoredOrder], so wiring
     * a balance refresh here (e.g. reloading Me home into the global `AccountBalanceStore`) covers
     * every purchase surface (Recharge, Chat, Match, Call, Vip, Treasure) uniformly, including the
     * ones that previously forgot to refresh balance after a successful purchase.
     */
    private val onPaymentSucceeded: (suspend () -> Unit)? = null,
    /**
     * When a VIP order verifies, flip this immediately so Discover Match Now / Me / Profile can
     * hide VIP upsells before the follow-up Me/VIP network refresh returns.
     */
    private val vipStatusStore: VipStatusStore? = null,
    /** Debug-only hook, toggled from Settings, that skips real Google Play Billing and simulates a success. */
    private val isFakePaymentEnabled: suspend () -> Boolean = { false },
    private val analyticsTracker: AnalyticsTracker? = null,
    private val selectPaymentMethod: suspend (BillingPaymentCheck) -> BillingPaymentType? = {
        BillingPaymentType.GooglePlay
    },
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val purchaseActive = AtomicBoolean(false)
    private var pendingPurchase: PendingPurchase? = null

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(appContext)
            .setListener { billingResult, purchases ->
                scope.launch {
                    handlePurchasesUpdated(billingResult, purchases.orEmpty())
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .build()
    }

    fun launchPurchase(
        activity: Activity,
        request: StorePurchaseRequest,
        onResult: (StorePurchaseResult) -> Unit,
    ) {
        scope.launch {
            if (!purchaseActive.compareAndSet(false, true)) {
                onResult(StorePurchaseResult.Failed(genericPaymentError()))
                return@launch
            }
            val normalizedRequest = request.copy(
                productId = request.productId.ifBlank { request.goodsId.toString() },
                productType = BillingProductType.fromApi(request.productType.apiValue),
            )
            runCatching {
                retryPendingOrders()
                startPurchase(activity, normalizedRequest, onResult)
            }.onFailure { error ->
                AppLogger.e(TAG, "launch failed: ${error.javaClass.simpleName}: ${error.message}", error)
                finishWithoutPending(onResult, StorePurchaseResult.Failed(genericPaymentError()))
            }
        }
    }

    suspend fun retryPendingOrders(): Int = withContext(Dispatchers.Main.immediate) {
        if (!ensureBillingReady()) return@withContext 0
        var successCount = 0
        orderStore.pendingOrders().forEach { order ->
            if (order.cancelPending) {
                retryCancelOrder(order)
                return@forEach
            }
            val updated = retryStoredOrder(order)
            if (updated) successCount += 1
        }
        successCount
    }

    private suspend fun startPurchase(
        activity: Activity,
        request: StorePurchaseRequest,
        onResult: (StorePurchaseResult) -> Unit,
    ) {
        val paymentCheck = when (val check = repository.checkPaymentType(request)) {
            is AppResult.Failure -> {
                finishWithoutPending(onResult, StorePurchaseResult.Failed(check.message))
                return
            }
            is AppResult.Success -> check.data
        }
        val selectedPaymentType = if (paymentCheck.requiresSelection) {
            selectPaymentMethod(paymentCheck)
        } else {
            BillingPaymentType.GooglePlay
        }
        if (selectedPaymentType == null) {
            finishWithoutPending(onResult, StorePurchaseResult.Canceled)
            return
        }
        val selectedRequest = request.copy(paymentType = selectedPaymentType)

        val order = when (val created = repository.createOrder(selectedRequest)) {
            is AppResult.Failure -> {
                finishWithoutPending(onResult, StorePurchaseResult.Failed(created.message))
                return
            }
            is AppResult.Success -> created.data
        }
        if (selectedPaymentType == BillingPaymentType.ThirdParty) {
            handleExternalPayment(activity, order, onResult)
            return
        }

        val useFakePayment = BuildConfig.DEBUG && isFakePaymentEnabled()
        analyticsTracker?.track(
            AnalyticsEvent.OrderSubmit(
                goodsId = order.goodsId,
                productId = order.productId,
                orderNo = order.tranNo,
                isSandboxData = useFakePayment,
            ),
        )
        val stored = order.toStoredOrder(selectedRequest, BillingOrderStatus.Created)
        orderStore.upsert(stored)

        if (useFakePayment) {
            completeFakePurchase(selectedRequest, order, stored, onResult)
            return
        }

        if (!ensureBillingReady()) {
            failAndCancel(stored, onResult, APP_ERROR_BILLING_UNAVAILABLE, BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
            return
        }
        val productDetails = queryProductDetails(order.productId, order.payType)
        if (productDetails == null) {
            failAndCancel(stored, onResult, APP_ERROR_PRODUCT_UNAVAILABLE, BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
            return
        }
        pendingPurchase = PendingPurchase(request = request, order = order, storedOrder = stored, onResult = onResult)
        val billingResult = billingClient.launchBillingFlow(
            activity,
            buildBillingFlowParams(productDetails, order.payType),
        )
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> Unit
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                failAndCancel(stored, onResult, APP_ERROR_USER_CANCELED, billingResult.responseCode, canceled = true)
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                handleAlreadyOwnedPurchase()
            }
            else -> {
                failAndCancel(stored, onResult, APP_ERROR_LAUNCH_FAILED, billingResult.responseCode)
            }
        }
    }

    private suspend fun handleExternalPayment(
        activity: Activity,
        order: GooglePayOrder,
        onResult: (StorePurchaseResult) -> Unit,
    ) {
        val url = order.externalPaymentUrl
        val opened = url != null && runCatching {
            val uri = Uri.parse(url)
            require(uri.scheme.equals("https", ignoreCase = true) || uri.scheme.equals("http", ignoreCase = true))
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.onFailure { error ->
            AppLogger.e(TAG, "external checkout open failed: ${error.javaClass.simpleName}", error)
        }.isSuccess
        if (opened) {
            analyticsTracker?.track(
                AnalyticsEvent.OrderSubmit(
                    goodsId = order.goodsId,
                    productId = order.productId,
                    orderNo = order.tranNo,
                ),
            )
            finishWithoutPending(onResult, StorePurchaseResult.Canceled)
            return
        }
        repository.cancelOrder(
            tranNo = order.tranNo,
            appErrorCode = APP_ERROR_EXTERNAL_URL_MISSING,
            googleCode = BillingClient.BillingResponseCode.ERROR,
        )
        finishWithoutPending(onResult, StorePurchaseResult.Failed(genericPaymentError()))
    }

    /**
     * Debug-only shortcut for environments where real Google Play Billing is unavailable
     * (e.g. sideloaded builds, channel packages not registered in Play Console). `check` and
     * `create` above already hit the real backend; here we skip only the BillingClient/Play
     * Store interaction (connection, product query, purchase flow, consume/acknowledge) since
     * there is no real Play purchase to drive it, and go straight to `/google/verify` with a
     * synthetic order id/purchase token, following the exact same success/failure handling
     * (`verifyStoredOrder`) as a real purchase would.
     */
    private suspend fun completeFakePurchase(
        request: StorePurchaseRequest,
        order: GooglePayOrder,
        stored: StoredBillingOrder,
        onResult: (StorePurchaseResult) -> Unit,
    ) {
        AppLogger.d(TAG, "Fake payment switch enabled, verifying tranNo=${order.tranNo} without Google Play Billing")
        val fakeToken = "$FAKE_PURCHASE_TOKEN_PREFIX${order.tranNo}"
        val purchased = stored.copy(
            status = BillingOrderStatus.Purchased,
            playOrderId = fakeToken,
            packageName = appContext.packageName,
            purchaseToken = fakeToken,
        )
        orderStore.upsert(purchased)
        pendingPurchase = PendingPurchase(request = request, order = order, storedOrder = purchased, onResult = onResult)
        verifyStoredOrder(purchased, onResult)
    }

    private suspend fun handlePurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>,
    ) {
        val pending = pendingPurchase ?: return
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases.firstOrNull { it.products.contains(pending.order.productId) }
                    ?: purchases.firstOrNull()
                if (purchase == null) {
                    failAndCancel(
                        pending.storedOrder,
                        pending.onResult,
                        APP_ERROR_EMPTY_PURCHASES,
                        billingResult.responseCode,
                    )
                    return
                }
                handleCompletedPurchase(pending, purchase)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                failAndCancel(
                    pending.storedOrder,
                    pending.onResult,
                    APP_ERROR_USER_CANCELED,
                    billingResult.responseCode,
                    canceled = true,
                )
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> handleAlreadyOwnedPurchase()
            else -> {
                failAndCancel(pending.storedOrder, pending.onResult, APP_ERROR_BILLING_GENERIC, billingResult.responseCode)
            }
        }
    }

    private suspend fun handleAlreadyOwnedPurchase() {
        val pending = pendingPurchase ?: return
        val purchase = queryOwnedPurchase(pending.storedOrder)
        if (purchase?.purchaseState == Purchase.PurchaseState.PURCHASED) {
            handleCompletedPurchase(pending, purchase)
        } else {
            finishPending(StorePurchaseResult.Failed(genericPaymentError()))
        }
    }

    private suspend fun handleCompletedPurchase(
        pending: PendingPurchase,
        purchase: Purchase,
    ) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                val orderId = purchase.orderId.orEmpty()
                if (orderId.isBlank()) {
                    failAndCancel(
                        pending.storedOrder,
                        pending.onResult,
                        APP_ERROR_MISSING_ORDER_ID,
                        BillingClient.BillingResponseCode.ERROR,
                    )
                    return
                }
                val purchased = pending.storedOrder.copy(
                    status = BillingOrderStatus.Purchased,
                    playOrderId = orderId,
                    packageName = appContext.packageName,
                    purchaseToken = purchase.purchaseToken,
                )
                orderStore.upsert(purchased)
                val confirmed = confirmPlayPurchase(pending.order.payType, purchase, purchased)
                if (confirmed != null) {
                    verifyStoredOrder(confirmed, pending.onResult)
                } else {
                    finishPending(StorePurchaseResult.Failed(genericPaymentError()))
                }
            }
            Purchase.PurchaseState.PENDING -> {
                val updated = pending.storedOrder.copy(
                    status = BillingOrderStatus.Unknown,
                    packageName = appContext.packageName,
                    purchaseToken = purchase.purchaseToken,
                )
                orderStore.upsert(updated)
                finishPending(StorePurchaseResult.Failed(genericPaymentError()))
            }
            else -> {
                failAndCancel(
                    pending.storedOrder,
                    pending.onResult,
                    APP_ERROR_INVALID_PURCHASE_STATE,
                    BillingClient.BillingResponseCode.ERROR,
                )
            }
        }
    }

    private suspend fun confirmPlayPurchase(
        payType: BillingPayType,
        purchase: Purchase,
        stored: StoredBillingOrder,
    ): StoredBillingOrder? =
        when (payType) {
            BillingPayType.InApp -> {
                val result = consumePurchase(purchase.purchaseToken)
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    stored.copy(status = BillingOrderStatus.Consumed).also { orderStore.upsert(it) }
                } else {
                    stored.copy(status = BillingOrderStatus.Unknown).also { orderStore.upsert(it) }
                    null
                }
            }
            BillingPayType.Subscription -> {
                if (purchase.isAcknowledged) {
                    stored.copy(status = BillingOrderStatus.Acknowledged).also { orderStore.upsert(it) }
                } else {
                    val result = acknowledgePurchase(purchase.purchaseToken)
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        stored.copy(status = BillingOrderStatus.Acknowledged).also { orderStore.upsert(it) }
                    } else {
                        stored.copy(status = BillingOrderStatus.Unknown).also { orderStore.upsert(it) }
                        null
                    }
                }
            }
        }

    private suspend fun verifyStoredOrder(
        order: StoredBillingOrder,
        onResult: ((StorePurchaseResult) -> Unit)?,
    ): Boolean {
        val result = repository.verifyOrder(
            tranNo = order.tranNo,
            orderId = order.playOrderId,
            packageName = order.packageName.ifBlank { appContext.packageName },
            purchaseToken = order.purchaseToken,
        )
        return when (result) {
            is AppResult.Success -> {
                orderStore.remove(order.tranNo)
                analyticsTracker?.track(
                    AnalyticsEvent.Pay(
                        goodsId = order.goodsId,
                        productId = order.productId,
                        orderNo = order.tranNo,
                        revenue = order.price.toDoubleOrNull(),
                        currency = order.currency.ifBlank { null },
                        isSandboxData = order.purchaseToken.startsWith(FAKE_PURCHASE_TOKEN_PREFIX),
                    ),
                )
                markVipStatusIfNeeded(order)
                onPurchaseVerified?.invoke()
                runCatching { onPaymentSucceeded?.invoke() }
                    .onFailure { error -> AppLogger.e(TAG, "balance refresh after purchase failed", error) }
                onResult?.let { finishPending(StorePurchaseResult.Success(order.uiId)) }
                true
            }
            is AppResult.Failure -> {
                orderStore.upsert(order.copy(status = BillingOrderStatus.Unknown))
                BillingOrderRetryWorker.enqueueImmediate(appContext)
                onResult?.let { finishPending(StorePurchaseResult.Failed(result.message)) }
                false
            }
        }
    }

    private suspend fun retryStoredOrder(order: StoredBillingOrder): Boolean {
        val orderWithPayload = if (order.purchaseToken.isBlank()) {
            val isStaleCreated = order.status == BillingOrderStatus.Created &&
                System.currentTimeMillis() - order.updatedAtMillis > STALE_CREATED_ORDER_MS
            if (isStaleCreated) {
                repository.cancelOrder(order.tranNo, APP_ERROR_STALE_CREATED_ORDER, BillingClient.BillingResponseCode.ERROR)
                orderStore.remove(order.tranNo)
            }
            val purchase = queryOwnedPurchase(order) ?: return false
            order.copy(
                status = BillingOrderStatus.Purchased,
                playOrderId = purchase.orderId.orEmpty(),
                packageName = appContext.packageName,
                purchaseToken = purchase.purchaseToken,
            ).also { orderStore.upsert(it) }
        } else {
            order
        }

        val purchase = queryOwnedPurchase(orderWithPayload)
        val confirmed = if (purchase?.purchaseState == Purchase.PurchaseState.PURCHASED) {
            confirmPlayPurchase(
                payType = BillingPayType.forProduct(BillingProductType.fromApi(orderWithPayload.productTypeApi)),
                purchase = purchase,
                stored = orderWithPayload.copy(
                    playOrderId = purchase.orderId.orEmpty().ifBlank { orderWithPayload.playOrderId },
                    packageName = appContext.packageName,
                    purchaseToken = purchase.purchaseToken,
                ),
            )
        } else {
            orderWithPayload
        } ?: orderWithPayload.copy(status = BillingOrderStatus.Unknown).also { orderStore.upsert(it) }

        if (confirmed.playOrderId.isBlank() || confirmed.purchaseToken.isBlank()) return false
        return verifyStoredOrder(confirmed, onResult = null)
    }

    private fun markVipStatusIfNeeded(order: StoredBillingOrder) {
        if (!isVipProductType(order.productTypeApi)) return
        vipStatusStore?.markVerifiedOptimistically(isVip = true)
    }

    private suspend fun retryCancelOrder(order: StoredBillingOrder) {
        when (repository.cancelOrder(order.tranNo, order.cancelAppErrorCode, order.cancelGoogleCode)) {
            is AppResult.Success -> orderStore.remove(order.tranNo)
            is AppResult.Failure -> Unit
        }
    }

    private suspend fun failAndCancel(
        order: StoredBillingOrder,
        onResult: (StorePurchaseResult) -> Unit,
        appErrorCode: Int,
        googleCode: Int,
        canceled: Boolean = false,
    ) {
        val cancelResult = repository.cancelOrder(order.tranNo, appErrorCode, googleCode)
        if (cancelResult is AppResult.Success) {
            orderStore.remove(order.tranNo)
        } else {
            orderStore.upsert(
                order.copy(
                    status = if (canceled) BillingOrderStatus.Cancelled else BillingOrderStatus.Failed,
                    cancelPending = true,
                    cancelAppErrorCode = appErrorCode,
                    cancelGoogleCode = googleCode,
                ),
            )
            BillingOrderRetryWorker.enqueueImmediate(appContext)
        }
        val result =
            if (canceled) {
                StorePurchaseResult.Canceled
            } else {
                StorePurchaseResult.Failed(genericPaymentError())
            }
        if (pendingPurchase != null) {
            finishPending(result)
        } else {
            finishWithoutPending(onResult, result)
        }
    }

    private fun genericPaymentError(): String =
        appContext.getString(com.example.demoproject.R.string.payment_generic_error)

    private fun finishPending(result: StorePurchaseResult) {
        val callback = pendingPurchase?.onResult
        pendingPurchase = null
        purchaseActive.set(false)
        callback?.invoke(result)
    }

    private fun finishWithoutPending(
        onResult: (StorePurchaseResult) -> Unit,
        result: StorePurchaseResult,
    ) {
        pendingPurchase = null
        purchaseActive.set(false)
        onResult(result)
    }

    private suspend fun ensureBillingReady(): Boolean {
        if (billingClient.isReady) return true
        val result = suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        continuation.resume(billingResult)
                    }

                    override fun onBillingServiceDisconnected() = Unit
                },
            )
        }
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    private suspend fun queryProductDetails(
        productId: String,
        payType: BillingPayType,
    ): ProductDetails? {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(payType.toPlayBillingProductType())
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        val result = suspendCancellableCoroutine { continuation ->
            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(productDetailsResult.productDetailsList.firstOrNull())
                } else {
                    continuation.resume(null)
                }
            }
        }
        return result
    }

    private fun buildBillingFlowParams(
        productDetails: ProductDetails,
        payType: BillingPayType,
    ): BillingFlowParams {
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .apply {
                if (payType == BillingPayType.Subscription) {
                    productDetails.selectedSubscriptionOfferDetails()?.offerToken?.let(::setOfferToken)
                }
            }
            .build()
        return BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
    }

    private suspend fun queryOwnedPurchase(order: StoredBillingOrder): Purchase? {
        val payType = BillingPayType.forProduct(BillingProductType.fromApi(order.productTypeApi))
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(payType.toPlayBillingProductType())
            .build()
        return suspendCancellableCoroutine { continuation ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    continuation.resume(null)
                    return@queryPurchasesAsync
                }
                continuation.resume(
                    purchases.firstOrNull { purchase ->
                        purchase.purchaseToken == order.purchaseToken ||
                            purchase.products.contains(order.productId)
                    },
                )
            }
        }
    }

    private suspend fun consumePurchase(purchaseToken: String): BillingResult {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        return suspendCancellableCoroutine { continuation ->
            billingClient.consumeAsync(params) { billingResult, _ ->
                continuation.resume(billingResult)
            }
        }
    }

    private suspend fun acknowledgePurchase(purchaseToken: String): BillingResult {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        return suspendCancellableCoroutine { continuation ->
            billingClient.acknowledgePurchase(params) { billingResult ->
                continuation.resume(billingResult)
            }
        }
    }

    private data class PendingPurchase(
        val request: StorePurchaseRequest,
        val order: GooglePayOrder,
        val storedOrder: StoredBillingOrder,
        val onResult: (StorePurchaseResult) -> Unit,
    )
}

private fun GooglePayOrder.toStoredOrder(
    request: StorePurchaseRequest,
    status: BillingOrderStatus,
): StoredBillingOrder =
    StoredBillingOrder(
        uiId = request.uiId,
        tranNo = tranNo,
        productId = productId,
        productTypeApi = productType.apiValue,
        payTypeApi = payType.apiValue,
        goodsId = goodsId,
        price = price,
        currency = currency,
        status = status,
    )

private const val APP_ERROR_USER_CANCELED = 1
private const val APP_ERROR_BILLING_GENERIC = 2
private const val APP_ERROR_PRODUCT_UNAVAILABLE = 3
private const val APP_ERROR_LAUNCH_FAILED = 4
private const val APP_ERROR_EMPTY_PURCHASES = 5
private const val APP_ERROR_MISSING_ORDER_ID = 6
private const val APP_ERROR_INVALID_PURCHASE_STATE = 7
private const val APP_ERROR_STALE_CREATED_ORDER = 8
private const val APP_ERROR_BILLING_UNAVAILABLE = APP_ERROR_BILLING_GENERIC
private const val APP_ERROR_EXTERNAL_URL_MISSING = 9

/** Verified VIP orders should flip local VIP membership immediately (before Me/VIP refresh). */
internal fun isVipProductType(productTypeApi: Int): Boolean =
    BillingProductType.fromApi(productTypeApi) == BillingProductType.Vip
