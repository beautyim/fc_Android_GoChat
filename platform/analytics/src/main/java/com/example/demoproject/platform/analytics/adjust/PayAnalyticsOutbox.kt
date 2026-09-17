package com.example.demoproject.platform.analytics.adjust

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.payAnalyticsDataStore by preferencesDataStore(name = "pay_analytics_outbox")

@Serializable
data class PendingPayRecord(
    val orderNo: String,
    val goodsId: Long,
    val productId: String,
    val revenue: Double? = null,
    val currency: String? = null,
    val isSandboxData: Boolean = false,
    val firebaseSent: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

/**
 * Durable queue for Adjust/Firebase `pay` events so a verified purchase can survive
 * process death before [com.adjust.sdk.Adjust.trackEvent] runs, and cold start can retry.
 * [submittedOrderNos] dedupes verify-path vs MQTT/`pay_event` double delivery.
 */
class PayAnalyticsOutbox(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    suspend fun isSubmitted(dedupeKey: String): Boolean {
        if (dedupeKey.isBlank()) return false
        return mutex.withLock {
            loadSubmitted().contains(dedupeKey)
        }
    }

    suspend fun pending(): List<PendingPayRecord> = mutex.withLock {
        loadPending()
    }

    /**
     * Inserts or refreshes a pending record. Returns the record after write
     * (including whether Firebase was already marked sent).
     */
    suspend fun enqueue(record: PendingPayRecord): PendingPayRecord = mutex.withLock {
        val pending = loadPending().toMutableList()
        val existingIndex = pending.indexOfFirst { it.orderNo == record.orderNo }
        val merged = if (existingIndex >= 0) {
            val existing = pending[existingIndex]
            existing.copy(
                goodsId = record.goodsId.takeIf { it > 0L } ?: existing.goodsId,
                productId = record.productId.ifBlank { existing.productId },
                revenue = record.revenue ?: existing.revenue,
                currency = record.currency?.takeIf { it.isNotBlank() } ?: existing.currency,
                isSandboxData = record.isSandboxData || existing.isSandboxData,
                firebaseSent = existing.firebaseSent || record.firebaseSent,
            ).also { pending[existingIndex] = it }
        } else {
            record.also { pending.add(it) }
        }
        savePending(pending)
        merged
    }

    suspend fun markFirebaseSent(dedupeKey: String) = mutex.withLock {
        if (dedupeKey.isBlank()) return@withLock
        val pending = loadPending().map { record ->
            if (record.orderNo == dedupeKey) record.copy(firebaseSent = true) else record
        }
        savePending(pending)
    }

    suspend fun markSubmitted(dedupeKey: String) = mutex.withLock {
        if (dedupeKey.isBlank()) return@withLock
        savePending(loadPending().filterNot { it.orderNo == dedupeKey })
        val submitted = loadSubmitted().toMutableList()
        submitted.removeAll { it == dedupeKey }
        submitted.add(0, dedupeKey)
        while (submitted.size > MAX_SUBMITTED) {
            submitted.removeAt(submitted.lastIndex)
        }
        saveSubmitted(submitted)
    }

    private suspend fun loadPending(): List<PendingPayRecord> {
        val raw = appContext.payAnalyticsDataStore.data.first()[Keys.PENDING_JSON]
        return decodeList(raw, PendingPayRecord.serializer())
    }

    private suspend fun loadSubmitted(): List<String> {
        val raw = appContext.payAnalyticsDataStore.data.first()[Keys.SUBMITTED_JSON]
        return decodeList(raw, String.serializer())
    }

    private suspend fun savePending(pending: List<PendingPayRecord>) {
        appContext.payAnalyticsDataStore.edit { prefs ->
            prefs[Keys.PENDING_JSON] = json.encodeToString(
                ListSerializer(PendingPayRecord.serializer()),
                pending,
            )
        }
    }

    private suspend fun saveSubmitted(submitted: List<String>) {
        appContext.payAnalyticsDataStore.edit { prefs ->
            prefs[Keys.SUBMITTED_JSON] = json.encodeToString(
                ListSerializer(String.serializer()),
                submitted,
            )
        }
    }

    private fun <T> decodeList(
        raw: String?,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): List<T> =
        raw?.takeIf { it.isNotBlank() }?.let {
            runCatching {
                json.decodeFromString(ListSerializer(serializer), it)
            }.getOrDefault(emptyList())
        } ?: emptyList()

    private object Keys {
        val PENDING_JSON = stringPreferencesKey("pending_pay_json")
        val SUBMITTED_JSON = stringPreferencesKey("submitted_pay_order_nos_json")
    }

    private companion object {
        const val MAX_SUBMITTED = 500
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }
    }
}
