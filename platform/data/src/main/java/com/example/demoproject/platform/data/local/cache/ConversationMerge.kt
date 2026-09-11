package com.example.demoproject.platform.data.local.cache

import com.example.demoproject.platform.data.model.Conversation
import com.example.demoproject.platform.data.model.User

/**
 * Conversation list / sync payloads often omit peer profile fields (or ship
 * placeholders). Blind [OnConflictStrategy.REPLACE] would wipe a previously
 * known avatar/nickname and flip local pin/mute. Merge keeps richer local
 * profile + pin/mute while accepting fresher preview/unread/mtime.
 */
internal fun Conversation.mergedWithExisting(existing: Conversation?): Conversation {
    if (existing == null) return this
    return copy(
        peer = peer.mergedWithExisting(existing.peer),
        // List/sync DTOs do not carry pin/mute — preserve local flags.
        isPinned = existing.isPinned,
        isMuted = existing.isMuted,
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
