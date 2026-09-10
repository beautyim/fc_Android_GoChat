package com.example.demoproject.product.chat

import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.model.MessageStatus
import com.example.demoproject.platform.data.model.toMessageTimelineMillis

/**
 * Stable chat-detail ordering.
 *
 * - Sending / Failed: sort by device timestamp used at send time.
 * - All other messages: sort by server timestamp.
 * - Once a non-pending message has a frozen key, later updates do not move it.
 * - When a Sending / Failed message becomes successful, it is re-keyed with the
 *   new server timestamp and may move.
 */
class ChatDetailMessageOrder {
    private val frozenKeys = linkedMapOf<String, Long>()
    private val committedIds = linkedSetOf<String>()

    fun ordered(messages: List<Message>): List<Message> {
        val presentIds = messages.mapTo(linkedSetOf()) { it.id }
        frozenKeys.keys.retainAll(presentIds)
        committedIds.retainAll(presentIds)

        for (message in messages) {
            val millis = message.createdAt.toMessageTimelineMillis()
            val pending = message.status.isPendingSortStatus()
            when {
                pending -> {
                    frozenKeys[message.id] = millis
                    committedIds.remove(message.id)
                }
                message.id !in committedIds -> {
                    frozenKeys[message.id] = millis
                    committedIds.add(message.id)
                }
            }
        }

        return messages.sortedWith(
            compareBy<Message> { frozenKeys[it.id] ?: it.createdAt.toMessageTimelineMillis() }
                .thenBy { it.id },
        )
    }
}

private fun MessageStatus.isPendingSortStatus(): Boolean =
    this == MessageStatus.Sending || this == MessageStatus.Failed
