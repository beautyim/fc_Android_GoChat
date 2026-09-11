package com.example.demoproject.platform.data.local.cache

import com.example.demoproject.platform.data.local.db.dao.ConversationDao
import com.example.demoproject.platform.data.local.db.dao.MessageDao
import com.example.demoproject.platform.data.local.db.entity.ConversationEntity
import com.example.demoproject.platform.data.local.db.entity.MessageEntity
import com.example.demoproject.platform.data.local.db.entity.toDomain
import com.example.demoproject.platform.data.local.db.entity.toEntity
import com.example.demoproject.platform.data.model.Conversation
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.model.MessageStatus
import com.example.demoproject.platform.data.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class RoomChatStore(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val sessionManager: SessionManager,
) : ChatStore {
    @Volatile override var lastSyncMtime: Long = 0L

    private fun ownerId(): String = sessionManager.currentUserId.orEmpty()

    override fun observeConversations(): Flow<List<Conversation>> =
        sessionManager.sessionFlow.flatMapLatest { session ->
            val owner = session?.userId.orEmpty()
            if (owner.isBlank()) {
                flowOf(emptyList())
            } else {
                conversationDao.observeAll(owner).map { rows: List<ConversationEntity> ->
                    rows.map { row -> row.toDomain() }
                }
            }
        }

    override fun observeTotalUnread(includeMuted: Boolean): Flow<Int> =
        sessionManager.sessionFlow.flatMapLatest { session ->
            val owner = session?.userId.orEmpty()
            if (owner.isBlank()) {
                flowOf(0)
            } else if (includeMuted) {
                conversationDao.observeTotalUnreadCountIncludingMuted(owner)
            } else {
                conversationDao.observeTotalUnreadCount(owner)
            }
        }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        sessionManager.sessionFlow.flatMapLatest { session ->
            val owner = session?.userId.orEmpty()
            if (owner.isBlank()) {
                flowOf(emptyList<Message>())
            } else {
                messageDao.observeByConversation(owner, conversationId).map { rows: List<MessageEntity> ->
                    rows.map { row -> row.toDomain() }
                }
            }
        }

    override suspend fun upsertConversations(list: List<Conversation>): List<Conversation> {
        val owner = ownerId()
        if (owner.isBlank() || list.isEmpty()) return emptyList()
        val merged = list.map { incoming ->
            val existing = conversationDao.getById(owner, incoming.id)?.toDomain()
            incoming.mergedWithExisting(existing)
        }
        conversationDao.upsertAll(merged.map { conversation -> conversation.toEntity(owner) })
        return merged
    }

    override suspend fun upsertMessages(conversationId: String, list: List<Message>) {
        val owner = ownerId()
        if (owner.isBlank() || list.isEmpty()) return
        // Always bind rows to the caller's conversation id. Server-generated control
        // notices often arrive with send_uid=0, which would otherwise resolve to "0"
        // and never show up under the peer conversation being synced.
        messageDao.upsertAll(
            list.map { message ->
                message.copy(conversationId = conversationId).toEntity(owner)
            },
        )
    }

    override suspend fun getMessages(conversationId: String): List<Message> {
        val owner = ownerId()
        if (owner.isBlank()) return emptyList()
        return messageDao.getByConversation(owner, conversationId).map { row -> row.toDomain() }
    }

    override suspend fun getLatestSuccessCreatedAt(conversationId: String): Long? {
        val owner = ownerId()
        if (owner.isBlank()) return null
        return messageDao.getLatestSuccessTs(owner, conversationId)
    }

    override suspend fun getPendingLocalOutbound(conversationId: String): List<Message> {
        val owner = ownerId()
        if (owner.isBlank()) return emptyList()
        return messageDao.getLocalOutboundPending(owner, conversationId).map { row -> row.toDomain() }
    }

    override suspend fun getConversation(conversationId: String): Conversation? {
        val owner = ownerId()
        if (owner.isBlank()) return null
        return conversationDao.getById(owner, conversationId)?.toDomain()
    }

    override suspend fun putConversation(conversation: Conversation) {
        val owner = ownerId()
        if (owner.isBlank()) return
        val existing = conversationDao.getById(owner, conversation.id)?.toDomain()
        val merged = conversation.mergedPeerWithExisting(existing)
        conversationDao.upsert(merged.toEntity(owner))
    }

    override suspend fun deleteConversation(conversationId: String) {
        val owner = ownerId()
        if (owner.isBlank()) return
        conversationDao.deleteById(owner, conversationId)
        messageDao.deleteByConversation(owner, conversationId)
    }

    override suspend fun deleteMessage(conversationId: String, messageId: String) {
        val owner = ownerId()
        if (owner.isBlank()) return
        messageDao.deleteById(owner, messageId)
    }

    override suspend fun failSending() {
        val owner = ownerId()
        if (owner.isBlank()) return
        messageDao.updateStatusWhere(
            ownerUserId = owner,
            fromStatus = MessageStatus.Sending.name,
            toStatus = MessageStatus.Failed.name,
        )
    }

    override suspend fun connectionsTotal(): Int {
        val owner = ownerId()
        if (owner.isBlank()) return 0
        return messageDao.countMutualConnections(owner)
    }
}
