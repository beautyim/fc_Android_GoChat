package com.example.demoproject.platform.data.message

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Server-authoritative chat unread total (`msg/list.unread`, `msg/get-unread.total`,
 * `app/init.msg_unread`). UI only collects this snapshot — never sum local conversation badges
 * or increment on MQTT.
 */
class ChatUnreadStore {
    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total.asStateFlow()

    fun update(total: Int) {
        _total.value = total.coerceAtLeast(0)
    }

    fun clear() {
        _total.value = 0
    }
}
