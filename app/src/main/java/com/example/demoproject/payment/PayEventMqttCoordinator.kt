package com.example.demoproject.payment

import com.example.demoproject.platform.analytics.AnalyticsEvent
import com.example.demoproject.platform.analytics.AnalyticsTracker
import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.billing.PayEvent
import com.example.demoproject.platform.data.billing.PayEventStore
import com.example.demoproject.platform.data.repository.BillingRepository
import com.example.demoproject.platform.data.repository.CoinRepository
import com.example.demoproject.platform.data.repository.VipRepository
import com.example.demoproject.platform.mqtt.MqttManager
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class PayEventMqttCoordinator(
    private val mqttManager: MqttManager,
    private val payEventStore: PayEventStore,
    private val billingRepository: BillingRepository,
    private val coinRepository: CoinRepository,
    private val vipRepository: VipRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    private val trackedEventIds = ConcurrentHashMap.newKeySet<Long>()

    fun start() {
        scope.launch {
            mqttManager.messageFlow.collect { inbound ->
                val events = runCatching { parseEvents(inbound.payload) }
                    .onFailure { AppLogger.w(TAG, "pay event parse failed: ${it.message}") }
                    .getOrDefault(emptyList())
                events.forEach { event -> process(event) }
            }
        }
        scope.launch {
            payEventStore.events.collect(::process)
        }
    }

    private suspend fun process(event: PayEvent) {
        if (trackedEventIds.add(event.eventId)) {
            analyticsTracker.track(
                AnalyticsEvent.Pay(
                    goodsId = event.goodsId,
                    productId = event.itemId,
                    orderNo = event.transactionId,
                    revenue = event.value,
                    currency = event.currency,
                ),
            )
            runCatching {
                coinRepository.getRechargePage()
                vipRepository.getVipPage()
            }.onFailure { AppLogger.w(TAG, "catalog refresh after pay event failed: ${it.message}") }
        }
        billingRepository.acknowledgePayEvent(event.eventId)
    }

    private fun parseEvents(payload: String): List<PayEvent> {
        val root = json.parseToJsonElement(payload).jsonObject
        val type = root.long("type")?.toInt()
        if (type != PAY_EVENT_TYPE && root.findArray("pay_event_list") == null) return emptyList()
        return root.findArray("pay_event_list").orEmpty().mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val eventId = obj.long("event_id")?.takeIf { it > 0L } ?: return@mapNotNull null
            val items = obj["items"] as? JsonObject
            PayEvent(
                eventId = eventId,
                transactionId = obj.string("transaction_id").orEmpty(),
                currency = obj.string("currency"),
                value = obj.string("value")?.toDoubleOrNull()
                    ?: obj.string("money")?.toDoubleOrNull(),
                goodsId = obj.long("goods_id") ?: 0L,
                itemId = items?.string("item_id").orEmpty(),
            )
        }
    }

    private fun JsonObject.findArray(key: String): JsonArray? {
        (this[key] as? JsonArray)?.let { return it }
        values.forEach { child ->
            val nested = child as? JsonObject ?: return@forEach
            nested.findArray(key)?.let { return it }
        }
        return null
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.long(key: String): Long? =
        string(key)?.toLongOrNull() ?: string(key)?.toDoubleOrNull()?.toLong()

    private companion object {
        const val TAG = "PayEventMqtt"
        const val PAY_EVENT_TYPE = 34
    }
}
