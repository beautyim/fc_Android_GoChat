package com.example.demoproject.platform.data.model

/**
 * Domain model for a single entry in the Notifications feed
 * (Figma node 4:2436).
 *
 * Each row has two visual variants depending on [kind]:
 *  - [NotificationKind.AvatarLike] / [NotificationKind.AvatarComment] /
 *    [NotificationKind.AvatarFollow] — render a user avatar on the left.
 *  - [NotificationKind.EmojiReaction] / [NotificationKind.EmojiSystem] —
 *    render a decorative emoji badge on the left.
 *
 * The UI further distinguishes read vs. unread presentation — unread rows
 * carry a purple-tinted background and an avatar dot badge.
 */
data class Notification(
    val id: String,
    val kind: NotificationKind,
    /** Source user when [kind] is an `Avatar*` type, `null` for emoji rows. */
    val actor: User?,
    /**
     * Canonical emoji character when [kind] is an `Emoji*` type, `null`
     * otherwise. E.g. "🔥", "✨", "💬".
     */
    val emoji: String?,
    /**
     * Title segments. Rendered with alternating bold/regular weights — see
     * [NotificationSegment]. The whole title is laid out as a single
     * paragraph so line-wrap is natural.
     */
    val titleSegments: List<NotificationSegment>,
    /** Optional italic quoted text shown below the title. */
    val quotedText: String?,
    val createdAt: Long,
    val isRead: Boolean,
)

/** A single run inside a notification title. */
data class NotificationSegment(
    val text: String,
    val emphasized: Boolean,
)

enum class NotificationKind {
    AvatarLike,
    AvatarComment,
    AvatarFollow,
    EmojiReaction,
    EmojiSystem,
}
