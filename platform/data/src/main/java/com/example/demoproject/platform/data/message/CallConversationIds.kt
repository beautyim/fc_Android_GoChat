package com.example.demoproject.platform.data.message

/**
 * Local-only conversation id for in-call `/call/send-msg` traffic pushed over MQTT with a non-zero
 * [com.example.demoproject.platform.data.network.dto.MessageDto.roomId].
 *
 * These rows must never drive the main chat tab's session list preview; they exist so the
 * in-call overlay can observe messages without polluting the 1:1 thread.
 */
object CallConversationIds {

    private const val PREFIX = "call_room_"

    fun forRoom(roomId: Long): String? =
        roomId.takeIf { it > 0L }?.let { "$PREFIX$it" }

    fun isCallRoomConversation(conversationId: String): Boolean =
        conversationId.startsWith(PREFIX)

    /**
     * JSON call-state / RTC invite envelopes sometimes arrive on chat MQTT topics.
     * They must never be shown as chat bubbles or persisted as user messages.
     */
    fun isCallSignalingWirePayload(rawContent: String): Boolean {
        val t = rawContent.trim()
        if (t.isEmpty() || !t.startsWith("{")) return false
        val lower = t.lowercase()
        if ("\"channel\"" in lower && "\"token\"" in lower && "\"uid\"" in lower) return true
        return false
    }

    /**
     * Chat history rows that describe a completed/missed call and should render
     * as the dedicated video-call bubble in chat detail.
     */
    fun isCallHistoryWirePayload(rawContent: String): Boolean {
        val t = rawContent.trim()
        if (t.isEmpty() || !t.startsWith("{")) return false
        val lower = t.lowercase()
        if ("\"call_type\"" !in lower) return false
        if ("\"channel\"" in lower && "\"token\"" in lower && "\"uid\"" in lower) return false
        return "\"call_status\"" in lower ||
            "\"duration\"" in lower ||
            "\"total_time\"" in lower ||
            "\"call_desc\"" in lower
    }
}
