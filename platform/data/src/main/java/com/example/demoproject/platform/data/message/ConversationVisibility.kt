package com.example.demoproject.platform.data.message

internal const val AI_SERVICE_CONVERSATION_UID: Long = 10_000_000L

internal fun isVisibleConversationPeer(peerUid: Long): Boolean =
    peerUid > 0L && peerUid != AI_SERVICE_CONVERSATION_UID
