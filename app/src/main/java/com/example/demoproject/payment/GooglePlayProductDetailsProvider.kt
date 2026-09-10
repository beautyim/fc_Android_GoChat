package com.example.demoproject.payment

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.example.demoproject.platform.data.repository.BillingPayType
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class GooglePlayPriceQuery(
    val productId: String,
    val payType: BillingPayType,
)

data class GooglePlayPriceKey(
    val productId: String,
    val payType: BillingPayType,
)

data class GooglePlayLocalizedPrice(
    val productId: String,
    val payType: BillingPayType,
    val formattedPrice: String,
    val offerToken: String? = null,
)

fun interface GooglePlayPriceProvider {
    suspend fun localizedPrices(queries: Collection<GooglePlayPriceQuery>): Map<GooglePlayPriceKey, GooglePlayLocalizedPrice>
}

object NoopGooglePlayPriceProvider : GooglePlayPriceProvider {
    override suspend fun localizedPrices(
        queries: Collection<GooglePlayPriceQuery>,
    ): Map<GooglePlayPriceKey, GooglePlayLocalizedPrice> = emptyMap()
}

class GooglePlayProductDetailsProvider(
    context: Context,
) : GooglePlayPriceProvider {
    private val appContext = context.applicationContext
    private val detailsCache = ConcurrentHashMap<GooglePlayPriceKey, GooglePlayLocalizedPrice>()

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(appContext)
            .setListener { _, _ -> }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .build()
    }

    override suspend fun localizedPrices(
        queries: Collection<GooglePlayPriceQuery>,
    ): Map<GooglePlayPriceKey, GooglePlayLocalizedPrice> {
        val normalizedQueries = queries
            .filter { it.productId.isNotBlank() }
            .distinctBy { it.productId to it.payType }
        if (normalizedQueries.isEmpty() || !ensureBillingReady()) return emptyMap()

        val resolved = mutableMapOf<GooglePlayPriceKey, GooglePlayLocalizedPrice>()
        normalizedQueries.groupBy(GooglePlayPriceQuery::payType).forEach { (payType, typedQueries) ->
            typedQueries.chunked(MAX_PRODUCT_DETAILS_QUERY_SIZE).forEach { chunk ->
                val missingQueries = chunk.filter { query ->
                    val key = query.toKey()
                    detailsCache[key]?.let { cached ->
                        resolved[key] = cached
                    } == null
                }
                if (missingQueries.isEmpty()) return@forEach
                queryProductDetails(missingQueries, payType).forEach { productDetails ->
                    productDetails.toLocalizedPrice(payType)?.let { price ->
                        val key = GooglePlayPriceKey(price.productId, price.payType)
                        detailsCache[key] = price
                        resolved[key] = price
                    }
                }
            }
        }
        return resolved
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
        queries: List<GooglePlayPriceQuery>,
        payType: BillingPayType,
    ): List<ProductDetails> {
        val products = queries.map { query ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(query.productId)
                .setProductType(payType.toPlayBillingProductType())
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()
        return suspendCancellableCoroutine { continuation ->
            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(productDetailsResult.productDetailsList)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }
    }
}

internal fun GooglePlayPriceQuery.toKey(): GooglePlayPriceKey =
    GooglePlayPriceKey(productId = productId, payType = payType)

internal fun ProductDetails.toLocalizedPrice(payType: BillingPayType): GooglePlayLocalizedPrice? {
    val formattedPrice = localizedFormattedPrice(payType)
        ?.normalizeGooglePlayDisplayPrice()
        ?.takeIf { it.isNotBlank() }
        ?: return null
    return GooglePlayLocalizedPrice(
        productId = productId,
        payType = payType,
        formattedPrice = formattedPrice,
        offerToken = selectedSubscriptionOfferDetails()?.offerToken,
    )
}

internal fun ProductDetails.localizedFormattedPrice(payType: BillingPayType): String? =
    when (payType) {
        BillingPayType.InApp -> oneTimePurchaseOfferDetails?.formattedPrice
        BillingPayType.Subscription -> selectedSubscriptionOfferDetails()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
    }

internal fun ProductDetails.selectedSubscriptionOfferDetails(): ProductDetails.SubscriptionOfferDetails? =
    subscriptionOfferDetails?.firstOrNull()

internal fun String.normalizeGooglePlayDisplayPrice(): String =
    USD_DISPLAY_PREFIX_REGEX.replace(trim()) { "$" }

internal fun BillingPayType.toPlayBillingProductType(): String =
    when (this) {
        BillingPayType.InApp -> BillingClient.ProductType.INAPP
        BillingPayType.Subscription -> BillingClient.ProductType.SUBS
    }

private const val MAX_PRODUCT_DETAILS_QUERY_SIZE = 20
private val USD_DISPLAY_PREFIX_REGEX = Regex("^US\\s*\\$\\s*")
