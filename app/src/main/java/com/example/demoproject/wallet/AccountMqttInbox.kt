package com.example.demoproject.wallet

import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.match.MatchQuotaStore
import com.example.demoproject.platform.data.vip.VipStatusStore
import com.example.demoproject.platform.data.wallet.AccountBalanceStore
import com.example.demoproject.platform.mqtt.MqttManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Applies account-level MQTT pushes on `{uid}/event`.
 *
 * Envelope `type=7` subtypes:
 * - `data.type=6` — `is_vip` / `vip_exp` → [VipStatusStore]
 * - `data.type=8` — `match_free_count` / `flash_count` → [MatchQuotaStore]
 * - `data.type=11` — coin balance → [AccountBalanceStore]
 *
 * Other subtypes (e.g. level=12) are ignored here.
 */
class AccountMqttInbox(
    private val mqttManager: MqttManager,
    private val accountBalanceStore: AccountBalanceStore,
    private val matchQuotaStore: MatchQuotaStore,
    private val vipStatusStore: VipStatusStore,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            mqttManager.messageFlow.collect { inbound ->
                runCatching { applyAccountPush(inbound.payload) }
                    .onFailure { error ->
                        AppLogger.w(TAG, "applyAccountPush crashed: ${error.message}")
                    }
            }
        }
    }

    private fun applyAccountPush(payload: String) {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()
            ?: return
        if (root.lenientIntOrNull("type") != ENVELOPE_ACCOUNT) return
        val data = root.resolveDataObject() ?: return
        when (data.lenientIntOrNull("type")) {
            DATA_VIP -> applyVip(data)
            DATA_MATCH_QUOTA -> applyMatchQuota(data)
            DATA_BALANCE -> applyBalance(data)
            else -> Unit
        }
    }

    private fun applyVip(data: JsonObject) {
        val isVipRaw = data.lenientIntOrNull("is_vip") ?: return
        val expiry = data.stringOrNull("vip_exp")
            ?.takeIf { it.isNotBlank() && it != "0" }
        vipStatusStore.update(isVip = isVipRaw == 1, expiryText = expiry)
        AppLogger.d(TAG, "mqtt vip type=$DATA_VIP → isVip=${isVipRaw == 1} exp=$expiry")
    }

    private fun applyBalance(data: JsonObject) {
        val balance = data.lenientIntOrNull("balance") ?: return
        if (balance < 0) return
        accountBalanceStore.update(balance)
        AppLogger.d(TAG, "mqtt balance type=$DATA_BALANCE → coins=$balance")
    }

    private fun applyMatchQuota(data: JsonObject) {
        val matchFree = data.lenientIntOrNull("match_free_count")
        val flash = data.lenientIntOrNull("flash_count")
        if (matchFree == null && flash == null) return
        matchQuotaStore.update(matchFreeCount = matchFree, flashCount = flash)
        AppLogger.d(
            TAG,
            "mqtt match quota type=$DATA_MATCH_QUOTA → free=$matchFree flash=$flash",
        )
    }

    private fun JsonObject.resolveDataObject(): JsonObject? {
        val dataElement = this["data"] ?: return null
        return when (dataElement) {
            is JsonObject -> dataElement
            is JsonPrimitive -> {
                val raw = dataElement.contentOrNull?.trim().orEmpty()
                if (raw.isBlank()) null
                else runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
            }
            else -> null
        }
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key].asLenientString()?.takeIf { it.isNotBlank() }

    private fun JsonObject.lenientIntOrNull(key: String): Int? =
        this[key].asLenientLong()?.toInt()

    private fun JsonElement?.asLenientString(): String? {
        val primitive = this as? JsonPrimitive ?: return null
        return primitive.contentOrNull?.trim()
    }

    private fun JsonElement?.asLenientLong(): Long? {
        val primitive = this as? JsonPrimitive ?: return null
        val content = primitive.content.trim()
        if (content.isEmpty()) return null
        return content.toLongOrNull()
            ?: content.toDoubleOrNull()?.toLong()
    }

    private companion object {
        const val TAG = "AccountMqttInbox"
        /** `{uid}/event` account / profile push envelope. */
        const val ENVELOPE_ACCOUNT = 7
        /** Nested `data.type` for VIP membership snapshot. */
        const val DATA_VIP = 6
        /** Nested `data.type` for free-match / flash-match counts. */
        const val DATA_MATCH_QUOTA = 8
        /** Nested `data.type` for coin-balance snapshot. */
        const val DATA_BALANCE = 11
    }
}
