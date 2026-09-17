package com.example.demoproject.chat

import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.message.ActiveConversationTracker
import com.example.demoproject.platform.data.repository.MessageRepository
import com.example.demoproject.platform.data.session.SessionManager
import com.example.demoproject.platform.mqtt.MqttManager
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Compensates MQTT delivery gaps with HTTP `/msg/sync` (+ `/msg/sync-ack`) and, when a
 * chat is open, `/msg/sync-detail`. Local ingest / unread stay in [ChatMqttInbox].
 *
 * Triggers:
 * - private-chat MQTT push
 * - MQTT reconnect (offline packets are not guaranteed to replay)
 */
class ChatMqttSyncCoordinator(
    private val mqttManager: MqttManager,
    private val messageRepository: MessageRepository,
    private val sessionManager: SessionManager,
    private val activeConversationTracker: ActiveConversationTracker,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    private val syncMutex = Mutex()
    private var collectJob: Job? = null
    private var reconnectJob: Job? = null

    fun start() {
        if (collectJob != null) return
        collectJob = scope.launch(Dispatchers.IO) {
            mqttManager.messageFlow.collect { inbound ->
                if (!isChatPush(inbound.payload)) return@collect
                val owner = sessionManager.currentUserId?.takeIf { it.isNotBlank() } ?: return@collect
                val incomingConversationId = extractIncomingConversationId(
                    payload = inbound.payload,
                    ownerUserId = owner,
                )
                val incomingMtime = extractIncomingMtime(inbound.payload)
                // Fast path for open ChatDetail — do not wait on list-sync mutex.
                val activeId = activeConversationTracker.conversationId.value
                if (activeId != null &&
                    (incomingConversationId == null || incomingConversationId == activeId)
                ) {
                    runCatching {
                        messageRepository.syncLatestMessages(
                            conversationId = activeId,
                            latestMtimeCeiling = chatPushCursorCeiling(incomingMtime),
                        )
                    }.onFailure { error ->
                        AppLogger.w(TAG, "active sync-detail failed: ${error.message}")
                    }
                }
                syncCompensation(
                    incomingConversationId = incomingConversationId,
                    incomingMtime = incomingMtime,
                    forceLatestPage = false,
                )
            }
        }
        reconnectJob = scope.launch(Dispatchers.IO) {
            mqttManager.connectedEvents.collect {
                if (sessionManager.currentUserId.isNullOrBlank()) return@collect
                syncCompensation(
                    incomingConversationId = null,
                    incomingMtime = null,
                    forceLatestPage = true,
                )
            }
        }
    }

    private suspend fun syncCompensation(
        incomingConversationId: String?,
        incomingMtime: Long?,
        forceLatestPage: Boolean,
    ) {
        syncMutex.withLock {
            var synced = false
            for ((index, delayMs) in RETRY_DELAYS_MS.withIndex()) {
                if (delayMs > 0L) delay(delayMs)
                val listResult = runCatching { messageRepository.syncConversationList() }
                    .getOrElse { AppResult.UnknownError(cause = it) }
                val activeId = activeConversationTracker.conversationId.value
                val detailResult = activeId?.let { id ->
                    runCatching {
                        messageRepository.syncLatestMessages(
                            conversationId = id,
                            latestMtimeCeiling = chatSyncCursorCeiling(
                                activeConversationId = id,
                                incomingConversationId = incomingConversationId,
                                incomingMtime = incomingMtime,
                                forceLatestPage = forceLatestPage,
                            ),
                        )
                    }.getOrElse { AppResult.UnknownError(cause = it) }
                }
                val listOk = listResult is AppResult.Success
                val detailOk = detailResult == null || detailResult is AppResult.Success
                if (listOk && detailOk) {
                    if (index > 0) {
                        AppLogger.d(TAG, "chat mqtt sync recovered attempt=${index + 1}")
                    }
                    synced = true
                    break
                }
            }
            if (!synced) {
                AppLogger.w(TAG, "chat mqtt sync incomplete")
            }
        }
    }

    private fun isChatPush(payload: String): Boolean {
        val trimmed = payload.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) return false
        val root = runCatching { json.parseToJsonElement(trimmed).jsonObject }.getOrNull()
            ?: return false
        val candidate = root["data"].asJsonObjectOrNull()
            ?: root["payload"].asJsonObjectOrNull()
            ?: root
        val type = root.lenientInt("type") ?: candidate.lenientInt("type")
        if (type != null && type in NON_CHAT_PUSH_TYPES) return false
        if (type == CHAT_PUSH_TYPE) return true
        return CHAT_FIELD_HINTS.any { candidate.containsKey(it) }
    }

    private fun extractIncomingConversationId(payload: String, ownerUserId: String): String? {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()
            ?: return null
        val candidate = root["data"].asJsonObjectOrNull()
            ?: root["payload"].asJsonObjectOrNull()
            ?: root
        val sendUid = candidate.lenientLong("send_uid")
            ?: root.lenientLong("send_uid")
            ?: return null
        val targetUid = candidate.lenientLong("target_uid")
            ?: root.lenientLong("target_uid")
            ?: 0L
        val chatId = candidate.lenientLong("chat_id")
            ?: root.lenientLong("chat_id")
            ?: 0L
        if (sendUid.toString() == ownerUserId) {
            // Outbound echo: peer is target_uid when present, else chat_id.
            return when {
                targetUid > 0L && targetUid.toString() != ownerUserId -> targetUid.toString()
                chatId > 0L && chatId.toString() != ownerUserId -> chatId.toString()
                else -> null
            }
        }
        // Inbound from peer: prefer send_uid, fall back to chat_id.
        return when {
            sendUid > 0L -> sendUid.toString()
            chatId > 0L && chatId.toString() != ownerUserId -> chatId.toString()
            else -> null
        }
    }

    private fun extractIncomingMtime(payload: String): Long? {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()
            ?: return null
        val candidate = root["data"].asJsonObjectOrNull()
            ?: root["payload"].asJsonObjectOrNull()
            ?: root
        return candidate.lenientLong("mtime")
            ?: candidate.lenientLong("m_time")
            ?: root.lenientLong("mtime")
    }

    private fun JsonElement?.asJsonObjectOrNull(): JsonObject? =
        runCatching { this?.jsonObject }.getOrNull()

    private fun JsonObject.lenientInt(key: String): Int? =
        lenientLong(key)?.toInt()

    private fun JsonObject.lenientLong(key: String): Long? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        val content = primitive.content.trim()
        if (content.isEmpty()) return null
        return content.toLongOrNull()
            ?: content.toDoubleOrNull()?.toLong()
            ?: primitive.contentOrNull?.toLongOrNull()
    }

    private companion object {
        const val TAG = "ChatMqttSync"
        const val CHAT_PUSH_TYPE = 1
        val NON_CHAT_PUSH_TYPES = setOf(3, 7, 13, 26, 36)
        val CHAT_FIELD_HINTS = setOf(
            "send_uid",
            "target_uid",
            "chat_id",
            "msg_type",
            "msg_content",
            "body",
            "mtime",
        )
        val RETRY_DELAYS_MS = listOf(0L, 800L, 1_500L)
    }
}

/**
 * `/msg/sync-detail` returns rows strictly after its cursor. Starting one tick
 * before the MQTT message prevents a concurrent outbound send from advancing the
 * local cursor past that incoming row.
 */
internal fun chatPushCursorCeiling(incomingMtime: Long?): Long? =
    incomingMtime
        ?.takeIf { it > 0L }
        ?.minus(1L)

internal fun chatSyncCursorCeiling(
    activeConversationId: String,
    incomingConversationId: String?,
    incomingMtime: Long?,
    forceLatestPage: Boolean,
): Long? =
    when {
        forceLatestPage -> 0L
        activeConversationId == incomingConversationId -> chatPushCursorCeiling(incomingMtime)
        else -> null
    }
