package com.example.demoproject.chat

import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.message.IncomingChatPush
import com.example.demoproject.platform.data.network.dto.MessageDto
import com.example.demoproject.platform.data.network.mapper.resolveConversationId
import com.example.demoproject.platform.data.network.mapper.toDomain
import com.example.demoproject.platform.data.repository.MessageRepository
import com.example.demoproject.platform.data.session.SessionManager
import com.example.demoproject.platform.mqtt.MqttManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Ingests private-chat MQTT envelopes (including control notices `key` 101–108) into
 * [MessageRepository]. Call signaling already consumes the same [MqttManager.messageFlow]
 * for invite/end packets; non-call rows are ignored there and handled here.
 */
class ChatMqttInbox(
    private val mqttManager: MqttManager,
    private val messageRepository: MessageRepository,
    private val sessionManager: SessionManager,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            mqttManager.messageFlow.collect { inbound ->
                val push = parseIncomingChat(inbound.payload) ?: return@collect
                runCatching { messageRepository.applyIncomingChatPush(push) }
                    .onFailure { error ->
                        AppLogger.w(TAG, "applyIncomingChatPush failed: ${error.message}")
                    }
            }
        }
    }

    private fun parseIncomingChat(payload: String): IncomingChatPush? {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()
            ?: return null
        val currentUserId = sessionManager.currentUserId?.takeIf { it.isNotBlank() } ?: return null

        val envelopeType = root.intOrNull("type")
        // type=3 is call signaling (a_type invite/end); leave it to MqttCallSignalingClient.
        if (envelopeType == 3) return null

        val packet = when (envelopeType) {
            1 -> root["data"]?.jsonObject ?: return null
            else -> root
        }

        val msgType = packet.intOrNull("msg_type") ?: return null
        if (msgType in CALL_MSG_TYPES) return null

        val chatType = packet.intOrNull("chat_type") ?: MessageDtoDefaultChatType
        if (chatType != MessageDtoDefaultChatType) return null

        val wireBody = packet.readWireBody()
        if (wireBody.isBlank()) return null

        val sendUid = packet.longOrNull("send_uid") ?: 0L
        val targetUid = packet.longOrNull("target_uid") ?: 0L
        val chatId = packet.longOrNull("chat_id") ?: 0L
        val mtime = packet.longOrNull("mtime")
            ?: packet.longOrNull("m_time")
            ?: 0L
        if (mtime <= 0L) return null

        val dto = MessageDto(
            sendUid = sendUid,
            targetUid = targetUid,
            chatId = chatId,
            chatType = chatType,
            msgType = msgType,
            msgContent = wireBody,
            mtime = mtime,
        )
        val conversationId = dto.resolveConversationId(currentUserId)
            .takeIf { it.isNotBlank() && it != "0" }
            ?: return null
        val message = dto.toDomain(conversationId)
        val fromPeer = sendUid > 0L && sendUid.toString() != currentUserId
        val userInfo = packet["user_info"]?.jsonObject
            ?: packet["user_infos"]?.jsonObject?.get(sendUid.toString())?.jsonObject
        return IncomingChatPush(
            conversationId = conversationId,
            message = message,
            fromPeer = fromPeer,
            peerNickname = userInfo?.stringOrNull("nickname"),
            peerAvatarUrl = userInfo?.stringOrNull("avatar")
                ?: userInfo?.stringOrNull("small_avatar"),
        )
    }

    private fun JsonObject.readWireBody(): String {
        val content = this["msg_content"] ?: this["body"] ?: return ""
        return when (content) {
            is JsonPrimitive -> content.contentOrNull.orEmpty()
            is JsonObject, is JsonArray -> content.toString()
            else -> ""
        }.trim()
    }

    private fun JsonObject.intOrNull(key: String): Int? =
        this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.longOrNull(key: String): Long? =
        this[key]?.jsonPrimitive?.longOrNull

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private companion object {
        const val TAG = "ChatMqttInbox"
        const val MessageDtoDefaultChatType = 1
        /** Call invite / end msg_type values — must not be stored as chat rows. */
        val CALL_MSG_TYPES = setOf(28, 101, 104)
    }
}
