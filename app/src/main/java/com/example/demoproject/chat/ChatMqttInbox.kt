package com.example.demoproject.chat

import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.message.IncomingChatPush
import com.example.demoproject.platform.data.model.ChatSendLimitNotice
import com.example.demoproject.platform.data.network.dto.MessageDto
import com.example.demoproject.platform.data.network.dto.MsgSendRequestDto
import com.example.demoproject.platform.data.network.mapper.resolveConversationId
import com.example.demoproject.platform.data.network.mapper.toDomain
import com.example.demoproject.platform.data.network.toPicUrlOrNull
import com.example.demoproject.platform.data.repository.MessageRepository
import com.example.demoproject.platform.data.session.SessionManager
import com.example.demoproject.platform.mqtt.MqttManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
                // Never let one poisoned packet cancel the collector — that would
                // permanently stop live peer messages until process death.
                val push = runCatching { parseIncomingChat(inbound.payload) }
                    .onFailure { error ->
                        AppLogger.w(TAG, "parseIncomingChat crashed: ${error.message}")
                    }
                    .getOrNull()
                    ?: return@collect
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

        val envelopeType = root.lenientIntOrNull("type")
        // type=3 is call signaling (a_type invite/end); leave it to MqttCallSignalingClient.
        if (envelopeType == 3) return null

        val packet = resolvePacket(envelopeType, root) ?: return null

        val chatType = packet.lenientIntOrNull("chat_type") ?: MessageDtoDefaultChatType
        // Gateway may omit chat_type or send 0 ("all"/default). Only reject clear non-private.
        if (chatType !in PRIVATE_CHAT_TYPES) return null

        val wireBody = packet.readWireBody()
        if (wireBody.isBlank()) return null

        val msgType = packet.lenientIntOrNull("msg_type")
            ?: MsgSendRequestDto.MSG_TYPE_TEXT
        // Call signaling reuses msg_type 101/104; still accept chat control notices
        // whose body carries key 101–108 (collision with those call types).
        if (msgType in CALL_MSG_TYPES && !ChatSendLimitNotice.shouldShow(wireBody)) {
            return null
        }

        val sendUid = packet.lenientLongOrNull("send_uid") ?: 0L
        val targetUid = packet.lenientLongOrNull("target_uid") ?: 0L
        val chatId = packet.lenientLongOrNull("chat_id") ?: 0L
        val mtime = packet.lenientLongOrNull("mtime")
            ?: packet.lenientLongOrNull("m_time")
            ?: packet.lenientLongOrNull("create_time")
            ?: packet.lenientLongOrNull("timestamp")
            ?: 0L
        if (mtime <= 0L) return null

        val dto = MessageDto(
            sendUid = sendUid,
            targetUid = targetUid,
            chatId = chatId,
            chatType = chatType.takeIf { it > 0 } ?: MessageDtoDefaultChatType,
            msgType = msgType,
            msgContent = wireBody,
            mtime = mtime,
        )
        val conversationId = dto.resolveConversationId(currentUserId)
            .takeIf { it.isNotBlank() && it != "0" }
            ?: return null
        val message = dto.toDomain(conversationId)
        val fromPeer = sendUid > 0L && sendUid.toString() != currentUserId
        val userInfo = packet.objectOrNull("user_info")
            ?: packet.objectOrNull("user_infos")?.objectOrNull(sendUid.toString())
        return IncomingChatPush(
            conversationId = conversationId,
            message = message,
            fromPeer = fromPeer,
            peerNickname = userInfo?.stringOrNull("nickname")?.takeIf { it.isNotBlank() },
            // Resolve CDN keys the same way as UserDto.toDomain — raw relative
            // paths would overwrite a good absolute URL and Coil would show blank.
            peerAvatarUrl = (
                userInfo?.stringOrNull("small_avatar")
                    ?: userInfo?.stringOrNull("avatar")
                ).toPicUrlOrNull(),
        )
    }

    /**
     * `type=1` chat envelopes usually nest fields under `data`, but backends also
     * send stringified `data` or flat roots with the same keys.
     */
    private fun resolvePacket(envelopeType: Int?, root: JsonObject): JsonObject? {
        if (envelopeType != 1) return root
        val dataElement = root["data"] ?: return root
        return when (dataElement) {
            is JsonObject -> dataElement
            is JsonPrimitive -> {
                val raw = dataElement.contentOrNull?.trim().orEmpty()
                if (raw.isBlank()) return root
                runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: root
            }
            else -> root
        }
    }

    private fun JsonObject.readWireBody(): String {
        val content = this["msg_content"] ?: this["body"] ?: this["content"] ?: return ""
        return when (content) {
            is JsonPrimitive -> content.contentOrNull.orEmpty()
            is JsonObject, is JsonArray -> content.toString()
            else -> ""
        }.trim()
    }

    private fun JsonObject.objectOrNull(key: String): JsonObject? {
        val element = this[key] ?: return null
        return when (element) {
            is JsonObject -> element
            is JsonPrimitive -> {
                val raw = element.contentOrNull?.trim().orEmpty()
                if (raw.isBlank() || !raw.startsWith("{")) null
                else runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
            }
            else -> null
        }
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key].asLenientString()?.takeIf { it.isNotBlank() }

    private fun JsonObject.lenientIntOrNull(key: String): Int? =
        this[key].asLenientLong()?.toInt()

    private fun JsonObject.lenientLongOrNull(key: String): Long? =
        this[key].asLenientLong()

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
        const val TAG = "ChatMqttInbox"
        const val MessageDtoDefaultChatType = 1
        /** Private / default private aliases. */
        val PRIVATE_CHAT_TYPES = setOf(0, 1)
        /** Call invite / end msg_type values — must not be stored as chat rows. */
        val CALL_MSG_TYPES = setOf(28, 101, 104)
    }
}
