package com.example.demoproject.platform.data.message

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground private-chat id for MQTT → HTTP compensation.
 * [ChatMqttSyncCoordinator] syncs `/msg/sync-detail` for this id when set.
 */
class ActiveConversationTracker {
    private val _conversationId = MutableStateFlow<String?>(null)
    val conversationId: StateFlow<String?> = _conversationId.asStateFlow()

    fun set(conversationId: String?) {
        _conversationId.value = conversationId?.takeIf { it.isNotBlank() }
    }

    fun clearIfMatch(conversationId: String) {
        val id = conversationId.takeIf { it.isNotBlank() } ?: return
        if (_conversationId.value == id) {
            _conversationId.value = null
        }
    }
}
