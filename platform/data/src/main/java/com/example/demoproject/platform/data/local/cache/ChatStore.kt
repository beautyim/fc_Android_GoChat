package com.example.demoproject.platform.data.local.cache

import com.example.demoproject.platform.data.model.Conversation
import com.example.demoproject.platform.data.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatStore {
    var lastSyncMtime: Long

    fun observeConversations(): Flow<List<Conversation>>
    fun observeTotalUnread(includeMuted: Boolean): Flow<Int>
    fun observeMessages(conversationId: String): Flow<List<Message>>

    /**
     * Upserts conversations, coalescing blank peer profile fields. Pin / mute
     * from the server row (`is_top` / `msg_notice`) replace local flags.
     * Returns the merged rows that were written.
     */
    suspend fun upsertConversations(list: List<Conversation>): List<Conversation>
    suspend fun upsertMessages(conversationId: String, list: List<Message>)
    suspend fun getMessages(conversationId: String): List<Message>
    /** Latest `createdAt` among Sent/Delivered/Read only (excludes Sending/Failed). */
    suspend fun getLatestSuccessCreatedAt(conversationId: String): Long?
    /** Local outbound rows still awaiting success (`local_*` / Sending|Failed). */
    suspend fun getPendingLocalOutbound(conversationId: String): List<Message>
    suspend fun getConversation(conversationId: String): Conversation?
    suspend fun putConversation(conversation: Conversation)
    suspend fun deleteConversation(conversationId: String)
    suspend fun deleteMessage(conversationId: String, messageId: String)
    suspend fun failSending()
    suspend fun connectionsTotal(): Int
}
