package com.example.demoproject.platform.data.message

import com.example.demoproject.platform.data.model.Message

/** Parsed private-chat MQTT push ready for local ingest + list preview refresh. */
data class IncomingChatPush(
    val conversationId: String,
    val message: Message,
    /** True when the peer sent this row (unread badge eligible). */
    val fromPeer: Boolean,
    val peerNickname: String? = null,
    val peerAvatarUrl: String? = null,
)
