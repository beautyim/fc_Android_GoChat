package com.example.demoproject.platform.data.local.cache

import com.example.demoproject.platform.data.model.Conversation
import com.example.demoproject.platform.data.model.User

/**
 * Conversation list / sync payloads often omit peer profile fields (or ship
 * placeholders). Blind [OnConflictStrategy.REPLACE] would wipe a previously
 * known avatar/nickname. Merge keeps richer local profile while accepting
 * fresher preview / unread / mtime / pin / mute from the server row.
 *
 * Pin (`is_top`) and mute (`msg_notice`) are mapped on the incoming row from
 * nested preview messages — prefer those over stale local flags.
 */
internal fun Conversation.mergedWithExisting(existing: Conversation?): Conversation {
    if (existing == null) return this
    return copy(
        peer = peer.mergedWithExisting(existing.peer),
    )
}

/**
 * For single-row [ChatStore.putConversation] updates that already set pin/mute
 * intentionally — only coalesce blank peer profile fields.
 */
internal fun Conversation.mergedPeerWithExisting(existing: Conversation?): Conversation {
    if (existing == null) return this
    return copy(peer = peer.mergedWithExisting(existing.peer))
}

internal fun User.mergedWithExisting(existing: User): User = copy(
    nickname = nickname.ifBlank { existing.nickname },
    avatar = avatar?.takeIf { it.isNotBlank() } ?: existing.avatar,
    age = age.takeIf { it > 0 } ?: existing.age,
    externalUserId = externalUserId.ifBlank { existing.externalUserId },
    bio = bio.ifBlank { existing.bio },
)
