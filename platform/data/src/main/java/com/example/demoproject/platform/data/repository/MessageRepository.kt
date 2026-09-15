package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.message.IncomingChatPush
import com.example.demoproject.platform.data.model.Conversation
import com.example.demoproject.platform.data.model.ConversationDetail
import com.example.demoproject.platform.data.model.ConversationListPage
import com.example.demoproject.platform.data.model.GiftConfig
import com.example.demoproject.platform.data.model.GiftFromType
import com.example.demoproject.platform.data.model.GiftSendResult
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.model.MessageType
import com.example.demoproject.platform.data.network.dto.TranslationSubmitRequestDto
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Chat / messaging operations backed by the `/msg/list`, `/msg/sync`,
 * `/msg/detail`, `/msg/sync-detail`, `/msg/send`, `/msg/delete`, and
 * `/msg/clear-unread` endpoints.
 *
 * Conventions:
 *  - `conversationId: String` carries the peer's `target_uid` as a
 *    decimal string — this keeps the UI's existing string id contract
 *    stable while matching the YAML's `target_uid: Long`.
 *  - The backend pages via `mtime` cursors; page numbers are bridged
 *    inside [MessageRepositoryImpl]. Callers use `page == 1` to refresh
 *    from the top.
 */
interface MessageRepository {

    /**
     * `/msg/list` — conversation list (cursor paging inside [MessageRepositoryImpl]).
     */
    suspend fun getConversations(page: Int): AppResult<ConversationListPage>

    /** Room-backed conversation list for the UI (MQTT/HTTP only write into Room). */
    fun observeConversations(): Flow<List<Conversation>>

    /**
     * Server-authoritative total unread (`ChatUnreadStore`).
     * [includeMuted] is unused — mute filtering is applied by the backend unread口径.
     */
    fun observeTotalUnreadCount(includeMuted: Boolean = false): Flow<Int>

    /**
     * Pull and persist `/msg/sync` using the repository's cursor state, then acknowledge
     * successful synchronization through `/msg/sync-ack`.
     */
    suspend fun syncConversationList(): AppResult<Unit>

    /** Latest `last_sync_mtime` from the last successful [getConversations] response (else `0`). */
    fun getLastSyncMtime(): Long

    /** `/msg/detail` — must be called when entering a chat, per YAML. */
    suspend fun getConversationDetail(conversationId: String): AppResult<ConversationDetail>

    /** Observe the Room-cached message list for a conversation. */
    fun observeMessages(conversationId: String): Flow<List<Message>>

    /**
     * Marks outbound messages stuck in [com.example.demoproject.platform.data.model.MessageStatus.Sending]
     * as Failed so the chat UI can show the resend control.
     *
     * Call after a cold start (or session restore): in-flight upload/send work from a
     * previous process cannot resume, so a permanent "sending" spinner is incorrect.
     */
    suspend fun failInterruptedSendingMessages()

    /**
     * Enter chat: sync latest messages using the local latest success mtime.
     *
     * Rules:
     * - Request: `start_mtime = latestTs`, `next_mtime = 0`.
     * - After writing to DB, if server `has_more = 1` ensure a gap marker exists.
     * - [latestMtimeCeiling] optionally caps the local cursor. MQTT uses the
     *   instant before the pushed message so a concurrent outbound send cannot
     *   advance the cursor past that incoming message.
     *
     * @return `true` if server still has older history.
     */
    suspend fun syncLatestMessages(
        conversationId: String,
        latestMtimeCeiling: Long? = null,
    ): AppResult<Boolean>

    /**
     * Load older history using the current gap marker.
     *
     * Rules:
     * - Request: `start_mtime = 0`, `next_mtime = gapMarkerTs`.
     * - After writing to DB, move the gap marker to the new oldest message when server has more.
     *
     * @return `true` if server still has more history.
     */
    suspend fun loadMoreMessages(conversationId: String): AppResult<Boolean>

    /** Records a locally blocked outbound message without calling `/msg/send`. */
    suspend fun recordBlockedOutboundMessage(
        conversationId: String,
        content: String,
        type: MessageType,
    ): AppResult<Message>

    /** `/msg/send` — plain text (`msg_type = 1`). */
    suspend fun sendTextMessage(
        conversationId: String,
        text: String,
        localMessageId: String? = null,
    ): AppResult<Message>

    /** `POST /msg/send` — emoji/expression (`msg_type = 23`). */
    suspend fun sendEmojiMessage(
        conversationId: String,
        emoji: String,
        localMessageId: String? = null,
    ): AppResult<Message>

    /** `POST /gift/config` — current channel gift catalog. */
    suspend fun getGiftConfig(): AppResult<GiftConfig>

    /**
     * `POST /gift/send` — send a gift to [conversationId] (peer uid).
     *
     * For call scene (`fromType = GiftFromType.CALL`), [roomId] is required and must be the
     * same HTTP room key used by `/call/message` / `/call/success` ([CallRoom.effectiveHttpRoomId]),
     * not only the numeric `room_session_id`.
     */
    suspend fun sendGift(
        conversationId: String,
        giftId: Long,
        count: Int = 1,
        fromType: Int = GiftFromType.CHAT,
        roomId: String? = null,
    ): AppResult<GiftSendResult>

    /** Upload with `MediaUploadService` then `/msg/send` (`msg_type = 3`). */
    suspend fun sendImageMessage(
        conversationId: String,
        localUriOrPath: String,
        localMessageId: String? = null,
    ): AppResult<Message>

    /** Upload then `/msg/send` (`msg_type = 18`). */
    suspend fun sendVideoMessage(
        conversationId: String,
        localUriOrPath: String,
        localMessageId: String? = null,
    ): AppResult<Message>

    suspend fun sendVoiceMessage(
        conversationId: String,
        localUriOrPath: String,
        durationSec: Int,
        localMessageId: String? = null,
    ): AppResult<Message>

    /** `/msg/delete` with [mtime] <= `0` deletes the whole conversation. */
    suspend fun deleteConversation(conversationId: String): AppResult<Unit>

    /** `/msg/delete` with [mtime] > `0` (here [messageId]) deletes a single row. */
    suspend fun deleteMessage(
        conversationId: String,
        messageId: String,
    ): AppResult<Unit>

    /**
     * `/msg/clear-unread` — gateway currently no-ops; kept for API parity.
     * Prefer [markConversationRead] / [refreshConversationUnread] for real unread updates.
     */
    suspend fun clearUnread(lastSyncMtime: Long): AppResult<Unit>

    /**
     * `/msg/read` then `/msg/get-unread` — clear the session on the server and overwrite
     * local badge + global total from the response (no local arithmetic).
     */
    suspend fun markConversationRead(conversationId: String): AppResult<Unit>

    /**
     * `/msg/get-unread` — overwrite one conversation badge and the global total from the server.
     */
    suspend fun refreshConversationUnread(conversationId: String): AppResult<Unit>

    /**
     * Ingest a private-chat MQTT push: upsert the message + list preview, then refresh unread
     * from `/msg/get-unread` when the peer sent the row (no local +1).
     */
    suspend fun applyIncomingChatPush(push: IncomingChatPush)

    /** Local-only state: backend contract for private chat pinning is not available yet. */
    suspend fun setConversationPinned(conversationId: String, pinned: Boolean): AppResult<Unit>

    /** Local-only state: backend contract for private chat muting is not available yet. */
    suspend fun setConversationMuted(conversationId: String, muted: Boolean): AppResult<Unit>

    /** Removes cached conversation + messages locally (e.g. block) without calling HTTP. */
    suspend fun deleteLocalConversation(conversationId: String)

    /**
     * Calls `/translation/submit` and caches the result in local `translatedText`.
     * [targetLanguage] defaults to the device system language when null/blank.
     */
    suspend fun translateMessage(
        conversationId: String,
        messageId: String,
        targetLanguage: String? = null,
    ): AppResult<String>

    /**
     * Calls `/translation/submit` for arbitrary standalone text that is not a cached
     * chat message (e.g. a profile bio/self-introduction). Unlike [translateMessage],
     * this does not read from or write into the message cache.
     * [targetLanguage] defaults to the device system language when null/blank.
     * [fromType] defaults to message translation; use profile-sign for bios.
     */
    suspend fun translateText(
        content: String,
        fromUid: Long,
        targetLanguage: String? = null,
        fromType: Int = TranslationSubmitRequestDto.FROM_TYPE_MESSAGE,
    ): AppResult<String>

    /** Local aggregate count of conversations with messages from both sides. */
    suspend fun getLocalConnectionsTotal(ownerUserId: String): Int
}
