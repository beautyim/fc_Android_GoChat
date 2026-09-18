package com.example.demoproject.product.chat

/**
 * Maps `/msg/detail` `unlock_from_type` and `msg/send` ok=3 `callback.from_type`
 * to the inline VIP unlock prompt variants (Figma 1:2694 / 1:2787).
 *
 * Only `1` and `2` show the card; any other value hides it.
 */
enum class ChatUnlockPromptType {
    /** Peer is waiting for a reply (`from_type` / `unlock_from_type` = 1). */
    WaitingReply,

    /** Current user initiated the chat (`from_type` / `unlock_from_type` = 2). */
    UserInitiated,
}

fun Int?.toChatUnlockPromptTypeOrNull(): ChatUnlockPromptType? = when (this) {
    1 -> ChatUnlockPromptType.WaitingReply
    2 -> ChatUnlockPromptType.UserInitiated
    else -> null
}
