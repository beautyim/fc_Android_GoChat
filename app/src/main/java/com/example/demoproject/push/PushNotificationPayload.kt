package com.example.demoproject.push

sealed interface PushNotificationPayload {
    val eventType: String

    data class NewMessage(
        val conversationId: String,
        val externalUserId: String,
        val messageId: String?,
    ) : PushNotificationPayload {
        override val eventType: String = TYPE_NEW_MESSAGE
    }

    data class Like(
        val senderId: String?,
        val eventId: String?,
    ) : PushNotificationPayload {
        override val eventType: String = TYPE_LIKE
    }

    companion object {
        const val TYPE_NEW_MESSAGE = "new_message"
        const val TYPE_LIKE = "like"
        const val EXTRA_PUSH_TYPE = "com.example.demoproject.push.TYPE"
        const val EXTRA_CONVERSATION_ID = "com.example.demoproject.push.CONVERSATION_ID"
        const val EXTRA_EXTERNAL_USER_ID = "com.example.demoproject.push.EXTERNAL_USER_ID"
        const val EXTRA_SENDER_ID = "com.example.demoproject.push.SENDER_ID"

        fun fromData(data: Map<String, String>): PushNotificationPayload? {
            val type = data["type"].orEmpty().lowercase()
            return when (type) {
                TYPE_NEW_MESSAGE -> {
                    val conversationId = data["conversation_id"].orEmpty().ifBlank {
                        data["chat_id"].orEmpty()
                    }
                    val senderId = data["sender_id"].orEmpty().ifBlank {
                        data["send_uid"].orEmpty()
                    }
                    if (conversationId.isBlank() && senderId.isBlank()) {
                        null
                    } else {
                        NewMessage(
                            conversationId = conversationId.ifBlank { senderId },
                            externalUserId = senderId.ifBlank { conversationId },
                            messageId = data["message_id"]?.takeIf { it.isNotBlank() },
                        )
                    }
                }
                TYPE_LIKE -> Like(
                    senderId = data["sender_id"]?.takeIf { it.isNotBlank() }
                        ?: data["target_id"]?.takeIf { it.isNotBlank() },
                    eventId = data["event_id"]?.takeIf { it.isNotBlank() },
                )
                else -> null
            }
        }
    }
}
