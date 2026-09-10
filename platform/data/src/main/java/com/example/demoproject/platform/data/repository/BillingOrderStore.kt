package com.example.demoproject.platform.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.billingOrderDataStore by preferencesDataStore(name = "billing_orders")

enum class BillingOrderStatus {
    Created,
    Purchased,
    Consumed,
    Acknowledged,
    Unknown,
    Verified,
    Cancelled,
    Failed,
}

@Serializable
data class StoredBillingOrder(
    val uiId: String,
    val tranNo: String,
    val productId: String,
    val productTypeApi: Int,
    val payTypeApi: Int,
    val goodsId: Long,
    val price: String,
    val currency: String,
    val status: BillingOrderStatus,
    val playOrderId: String = "",
    val packageName: String = "",
    val purchaseToken: String = "",
    val cancelPending: Boolean = false,
    val cancelAppErrorCode: Int = 0,
    val cancelGoogleCode: Int = 0,
    val updatedAtMillis: Long = System.currentTimeMillis(),
) {
    val canRetry: Boolean
        get() = cancelPending || status in RetryableStatuses

    companion object {
        val RetryableStatuses = setOf(
            BillingOrderStatus.Created,
            BillingOrderStatus.Purchased,
            BillingOrderStatus.Consumed,
            BillingOrderStatus.Acknowledged,
            BillingOrderStatus.Unknown,
        )
    }
}

class BillingOrderStore(
    context: Context,
) {
    private val appContext = context.applicationContext

    suspend fun allOrders(): List<StoredBillingOrder> =
        appContext.billingOrderDataStore.data
            .map { prefs -> decodeOrders(prefs[Keys.ORDERS_JSON]) }
            .first()

    suspend fun pendingOrders(): List<StoredBillingOrder> =
        allOrders().filter { it.canRetry }

    suspend fun upsert(order: StoredBillingOrder) {
        mutate { orders ->
            orders.filterNot { it.tranNo == order.tranNo } + order.copy(updatedAtMillis = System.currentTimeMillis())
        }
    }

    suspend fun update(
        tranNo: String,
        transform: (StoredBillingOrder) -> StoredBillingOrder,
    ): StoredBillingOrder? {
        var updated: StoredBillingOrder? = null
        mutate { orders ->
            orders.map { order ->
                if (order.tranNo == tranNo) {
                    transform(order).copy(updatedAtMillis = System.currentTimeMillis()).also { updated = it }
                } else {
                    order
                }
            }
        }
        return updated
    }

    suspend fun remove(tranNo: String) {
        mutate { orders -> orders.filterNot { it.tranNo == tranNo } }
    }

    suspend fun clear() {
        appContext.billingOrderDataStore.edit { it.clear() }
    }

    private suspend fun mutate(transform: (List<StoredBillingOrder>) -> List<StoredBillingOrder>) {
        appContext.billingOrderDataStore.edit { prefs ->
            val orders = decodeOrders(prefs[Keys.ORDERS_JSON])
            prefs[Keys.ORDERS_JSON] = json.encodeToString(
                ListSerializer(StoredBillingOrder.serializer()),
                transform(orders),
            )
        }
    }

    private fun decodeOrders(raw: String?): List<StoredBillingOrder> =
        raw?.takeIf { it.isNotBlank() }?.let {
            runCatching {
                json.decodeFromString(ListSerializer(StoredBillingOrder.serializer()), it)
            }.getOrDefault(emptyList())
        } ?: emptyList()

    private object Keys {
        val ORDERS_JSON = stringPreferencesKey("orders_json")
    }
}

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
