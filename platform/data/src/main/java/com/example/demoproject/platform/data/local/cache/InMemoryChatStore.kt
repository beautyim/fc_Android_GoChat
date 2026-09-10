package com.example.demoproject.platform.data.local.cache

import com.example.demoproject.platform.data.model.Conversation
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

/** In-memory stand-in; prefer [RoomChatStore] when SQLCipher DB is available. */
class InMemoryChatStore : ChatStore {
    private val conversations = ConcurrentHashMap<String, Conversation>()
    private val messages = ConcurrentHashMap<String, MutableList<Message>>()
    private val tick = MutableStateFlow(0L)
    @Volatile override var lastSyncMtime: Long = 0L

    private fun bump() {
        tick.value = tick.value + 1
    }

    override fun observeConversations(): Flow<List<Conversation>> =
        tick.map {
            conversations.values.sortedWith(
                compareByDescending<Conversation> { it.isPinned }
                    .thenByDescending { it.updatedAt },
            )
        }

    override fun observeTotalUnread(includeMuted: Boolean): Flow<Int> =
        tick.map {
            conversations.values
                .filter { includeMuted || !it.isMuted }
                .sumOf { it.unreadCount }
        }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        tick.map {
            messages[conversationId].orEmpty().sortedBy { it.createdAt }
        }

    override suspend fun upsertConversations(list: List<Conversation>) {
        list.forEach { conversations[it.id] = it }
        bump()
    }

    override suspend fun upsertMessages(conversationId: String, list: List<Message>) {
        val bucket = messages.getOrPut(conversationId) { mutableListOf() }
        val byId = bucket.associateBy { it.id }.toMutableMap()
        list.forEach { byId[it.id] = it.copy(conversationId = conversationId) }
        bucket.clear()
        bucket.addAll(byId.values)
        bump()
    }

    override suspend fun getMessages(conversationId: String): List<Message> =
        messages[conversationId].orEmpty().toList()

    override suspend fun getLatestSuccessCreatedAt(conversationId: String): Long? =
        messages[conversationId]
            .orEmpty()
            .filter {
                it.status == MessageStatus.Sent ||
                    it.status == MessageStatus.Delivered ||
                    it.status == MessageStatus.Read
            }
            .maxOfOrNull { it.createdAt }

    override suspend fun getPendingLocalOutbound(conversationId: String): List<Message> =
        messages[conversationId]
            .orEmpty()
            .filter {
                (it.status == MessageStatus.Sending || it.status == MessageStatus.Failed) &&
                    (it.id.startsWith("local_") || it.id.startsWith("local-"))
            }

    override suspend fun getConversation(conversationId: String): Conversation? =
        conversations[conversationId]

    override suspend fun putConversation(conversation: Conversation) {
        conversations[conversation.id] = conversation
        bump()
    }

    override suspend fun deleteConversation(conversationId: String) {
        conversations.remove(conversationId)
        messages.remove(conversationId)
        bump()
    }

    override suspend fun deleteMessage(conversationId: String, messageId: String) {
        val bucket = messages[conversationId] ?: return
        bucket.removeAll { it.id == messageId }
        bump()
    }

    override suspend fun failSending() {
        messages.forEach { (_, list) ->
            for (i in list.indices) {
                val msg = list[i]
                if (msg.status == MessageStatus.Sending) {
                    list[i] = msg.copy(status = MessageStatus.Failed)
                }
            }
        }
        bump()
    }

    override suspend fun connectionsTotal(): Int =
        conversations.values.count { it.lastMessage != null }
}
