package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.blocked.BlockedUsersStore
import com.example.demoproject.platform.data.blocked.excludingBlockedConversations
import com.example.demoproject.platform.data.local.cache.ChatStore
import com.example.demoproject.platform.data.message.ChatUnreadStore
import com.example.demoproject.platform.data.message.IncomingChatPush
import com.example.demoproject.platform.data.model.Conversation
import com.example.demoproject.platform.data.model.ConversationDetail
import com.example.demoproject.platform.data.model.ConversationListPage
import com.example.demoproject.platform.data.model.Gender
import com.example.demoproject.platform.data.model.GiftConfig
import com.example.demoproject.platform.data.model.GiftFromType
import com.example.demoproject.platform.data.model.GiftSendResult
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.model.MessageStatus
import com.example.demoproject.platform.data.model.MessageType
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.data.model.toMessageTimelineMicros
import com.example.demoproject.platform.data.network.api.MessageApi
import com.example.demoproject.platform.data.network.api.MessageChatRequestDto
import com.example.demoproject.platform.data.network.api.MessageSyncDetailRequestDto
import com.example.demoproject.platform.data.network.api.TranslationApi
import com.example.demoproject.platform.data.network.dto.ConversationReadRequestDto
import com.example.demoproject.platform.data.network.dto.GiftSendRequestDto
import com.example.demoproject.platform.data.network.dto.MessageDeleteRequestDto
import com.example.demoproject.platform.data.network.dto.MessageListRequestDto
import com.example.demoproject.platform.data.network.dto.MessageSyncAckRequestDto
import com.example.demoproject.platform.data.network.dto.MessageSyncRequestDto
import com.example.demoproject.platform.data.network.dto.MsgContentDto
import com.example.demoproject.platform.data.network.dto.MsgSendDataDto
import com.example.demoproject.platform.data.network.dto.MsgSendRequestDto
import com.example.demoproject.platform.data.network.dto.MsgUnreadRequestDto
import com.example.demoproject.platform.data.network.dto.TranslationSubmitRequestDto
import com.example.demoproject.platform.data.network.mapper.toDomain
import com.example.demoproject.platform.data.network.mapper.toDomainConversations
import com.example.demoproject.platform.data.network.mapper.toDomainMessages
import com.example.demoproject.platform.data.network.resolveTranslationTarget
import com.example.demoproject.platform.data.network.toAssetUrlOrNull
import com.example.demoproject.platform.data.network.toRelativeMediaPathOrNull
import com.example.demoproject.platform.data.session.SessionManager
import com.example.demoproject.platform.data.wallet.AccountBalanceStore
import com.example.demoproject.platform.s3.MediaUploadService
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.result.onSuccessSuspend
import com.example.demoproject.platform.network.safeApiCall
import com.example.demoproject.platform.network.safeApiCallNullable
import com.example.demoproject.platform.network.safeApiCallUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Network-first chat repository with [ChatStore] local cache (Room-backed in production wiring).
 */
class MessageRepositoryNetworkImpl(
    private val messageApi: MessageApi,
    private val translationApi: TranslationApi,
    private val sessionManager: SessionManager,
    private val mediaUploadService: MediaUploadService,
    private val chatStore: ChatStore,
    private val blockedUsersStore: BlockedUsersStore,
    private val accountBalanceStore: AccountBalanceStore,
    private val chatUnreadStore: ChatUnreadStore,
) : MessageRepository {

    override suspend fun getConversations(page: Int): AppResult<ConversationListPage> {
        return safeApiCall {
            messageApi.getConversations(
                MessageListRequestDto(
                    lastSyncMtime = if (page <= 1) 0L else chatStore.lastSyncMtime,
                ),
            )
        }.map { dto ->
            val conversations = chatStore.upsertConversations(dto.toDomainConversations())
            chatStore.lastSyncMtime = dto.lastSyncMtime
            // `msg/list` always carries total unread; use it as the global snapshot.
            chatUnreadStore.update(dto.unread)
            ConversationListPage(
                conversations = conversations.excludingBlockedConversations(blockedUsersStore),
                hasMore = dto.hasMore,
                lastSyncMtime = dto.lastSyncMtime,
            )
        }
    }

    override fun observeConversations(): Flow<List<Conversation>> =
        chatStore.observeConversations().map { it.excludingBlockedConversations(blockedUsersStore) }

    override fun observeTotalUnreadCount(includeMuted: Boolean): Flow<Int> =
        chatUnreadStore.total

    override suspend fun syncConversationList(): AppResult<Unit> {
        return safeApiCall {
            messageApi.syncConversations(
                MessageSyncRequestDto(lastSyncMtime = chatStore.lastSyncMtime),
            )
        }.map { dto ->
            chatStore.upsertConversations(dto.toDomainConversations())
            chatStore.lastSyncMtime = dto.lastSyncMtime
            // Do not write dto.unread here: `msg/sync` often omits it and the DTO default
            // would incorrectly wipe ChatUnreadStore to 0.
            Unit
        }.also { result ->
            if (result is AppResult.Success) {
                safeApiCallUnit {
                    messageApi.syncAck(MessageSyncAckRequestDto(lastSyncMtime = chatStore.lastSyncMtime))
                }
            }
        }
    }

    override fun getLastSyncMtime(): Long = chatStore.lastSyncMtime

    override suspend fun getConversationDetail(conversationId: String): AppResult<ConversationDetail> {
        val chatId = conversationId.toLongOrNull() ?: 0L
        return safeApiCall {
            messageApi.getConversationDetail(MessageChatRequestDto(chatId = chatId))
        }.map { dto ->
            val peer = dto.userInfo?.toDomain() ?: placeholderPeer(conversationId)
            val existing = chatStore.getConversation(conversationId)
            val conversation = existing?.copy(peer = peer) ?: Conversation(
                id = conversationId,
                peer = peer,
                lastMessage = null,
                unreadCount = 0,
                updatedAt = System.currentTimeMillis(),
            )
            chatStore.putConversation(conversation)
            ConversationDetail(
                conversation = conversation,
                peerHasReplied = dto.chatIsReply == 1,
                friendStatus = dto.friendStatus,
                unlockFromType = dto.unlockFromType.takeIf { it >= 0 },
                freeMessageCount = dto.freeMessageCount,
            )
        }
    }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        chatStore.observeMessages(conversationId)

    override suspend fun failInterruptedSendingMessages() {
        chatStore.failSending()
    }

    override suspend fun syncLatestMessages(
        conversationId: String,
        latestMtimeCeiling: Long?,
    ): AppResult<Boolean> {
        val chatId = conversationId.toLongOrNull() ?: 0L
        // Pending outbound must survive sync: take a snapshot and re-upsert after merge.
        val pendingOutbound = chatStore.getPendingLocalOutbound(conversationId)
        val localLatest = latestMtimeCeiling
            ?: chatStore.getLatestSuccessCreatedAt(conversationId)
            ?: 0L
        val start = localLatest.toMessageTimelineMicros()
        return safeApiCall {
            // Docs: last_sync_mtime = incremental start; last_mtime = older-page cursor.
            // Keep last_mtime=0 so gateways that only honor last_mtime still return the
            // newest page (incl. control tips). Cursor excludes Sending/Failed so a
            // device-clock pending row cannot skew the sync window.
            messageApi.syncMessages(
                MessageSyncDetailRequestDto(
                    chatId = chatId,
                    lastMtime = 0L,
                    lastSyncMtime = start,
                ),
            )
        }.map { dto ->
            val ownerId = sessionManager.currentUserId.orEmpty()
            val mapped = dto.toDomainMessages(
                currentUserId = ownerId,
                conversationId = conversationId,
            )
            chatStore.upsertMessages(conversationId, mapped)
            if (pendingOutbound.isNotEmpty()) {
                chatStore.upsertMessages(conversationId, pendingOutbound)
            }
            dto.hasMore
        }
    }

    override suspend fun loadMoreMessages(conversationId: String): AppResult<Boolean> {
        val chatId = conversationId.toLongOrNull() ?: 0L
        val pendingOutbound = chatStore.getPendingLocalOutbound(conversationId)
        val oldestSuccess = chatStore.getMessages(conversationId)
            .filter {
                it.status == MessageStatus.Sent ||
                    it.status == MessageStatus.Delivered ||
                    it.status == MessageStatus.Read
            }
            .minOfOrNull { it.createdAt }
            ?: chatStore.getMessages(conversationId).minOfOrNull { it.createdAt }
            ?: 0L
        return safeApiCall {
            messageApi.syncMessages(
                MessageSyncDetailRequestDto(
                    chatId = chatId,
                    lastMtime = oldestSuccess.toMessageTimelineMicros(),
                    lastSyncMtime = 0L,
                ),
            )
        }.map { dto ->
            val ownerId = sessionManager.currentUserId.orEmpty()
            chatStore.upsertMessages(
                conversationId,
                dto.toDomainMessages(
                    currentUserId = ownerId,
                    conversationId = conversationId,
                ),
            )
            if (pendingOutbound.isNotEmpty()) {
                chatStore.upsertMessages(conversationId, pendingOutbound)
            }
            dto.hasMore
        }
    }

    override suspend fun recordBlockedOutboundMessage(
        conversationId: String,
        content: String,
        type: MessageType,
    ): AppResult<Message> {
        val msg = localOutbound(conversationId, content, type, MessageStatus.Failed)
        chatStore.upsertMessages(conversationId, listOf(msg))
        return AppResult.Success(msg)
    }

    override suspend fun sendTextMessage(
        conversationId: String,
        text: String,
        localMessageId: String?,
    ): AppResult<Message> = sendSimple(conversationId, text, MsgSendRequestDto.MSG_TYPE_TEXT, localMessageId)

    override suspend fun sendEmojiMessage(
        conversationId: String,
        emoji: String,
        localMessageId: String?,
    ): AppResult<Message> = sendSimple(conversationId, emoji, MsgSendRequestDto.MSG_TYPE_EMOJI, localMessageId)

    override suspend fun getGiftConfig(): AppResult<GiftConfig> =
        safeApiCall { messageApi.getGiftConfig() }.map { dto ->
            GiftConfig(
                version = dto.version,
                gifts = dto.list.map { gift ->
                    com.example.demoproject.platform.data.model.Gift(
                        id = gift.giftId,
                        title = gift.title,
                        price = gift.price,
                        iconUrl = gift.icon.toAssetUrlOrNull().orEmpty(),
                        svgaName = gift.svgaName,
                        svgaUrl = gift.svgaUrl.toAssetUrlOrNull().orEmpty(),
                        unlitIconUrl = gift.unlitIcon.toAssetUrlOrNull().orEmpty(),
                        isNew = gift.isNew == 1,
                    )
                },
            )
        }

    override suspend fun sendGift(
        conversationId: String,
        giftId: Long,
        count: Int,
        fromType: Int,
        roomId: String?,
    ): AppResult<GiftSendResult> {
        val targetUid = conversationId.toLongOrNull() ?: 0L
        return safeApiCall {
            messageApi.sendGift(
                GiftSendRequestDto(
                    giftId = giftId,
                    toUid = targetUid,
                    number = count,
                    fromType = fromType,
                    roomId = roomId,
                ),
            )
        }.map { dto ->
            val balance = dto.account?.money?.takeIf { it >= 0 } ?: dto.balance
            accountBalanceStore.update(balance)
            GiftSendResult(
                balance = balance,
                mtime = dto.mtime,
                gift = dto.giftInfo?.let { gift ->
                    com.example.demoproject.platform.data.model.Gift(
                        id = gift.giftId,
                        title = gift.title,
                        price = gift.price,
                        iconUrl = gift.icon.toAssetUrlOrNull().orEmpty(),
                        svgaName = gift.svgaName,
                        svgaUrl = gift.svgaUrl.toAssetUrlOrNull().orEmpty(),
                        unlitIconUrl = gift.unlitIcon.toAssetUrlOrNull().orEmpty(),
                        isNew = gift.isNew == 1,
                    )
                },
            )
        }
    }

    override suspend fun sendImageMessage(
        conversationId: String,
        localUriOrPath: String,
        localMessageId: String?,
    ): AppResult<Message> = sendMedia(conversationId, localUriOrPath, MsgSendRequestDto.MSG_TYPE_IMAGE, localMessageId)

    override suspend fun sendVideoMessage(
        conversationId: String,
        localUriOrPath: String,
        localMessageId: String?,
    ): AppResult<Message> = sendMedia(conversationId, localUriOrPath, MsgSendRequestDto.MSG_TYPE_VIDEO, localMessageId)

    override suspend fun sendVoiceMessage(
        conversationId: String,
        localUriOrPath: String,
        durationSec: Int,
        localMessageId: String?,
    ): AppResult<Message> = sendMedia(
        conversationId,
        localUriOrPath,
        MsgSendRequestDto.MSG_TYPE_VOICE,
        localMessageId,
        durationSec = durationSec,
    )

    override suspend fun deleteConversation(conversationId: String): AppResult<Unit> {
        val chatId = conversationId.toLongOrNull() ?: 0L
        return safeApiCallUnit {
            messageApi.deleteMessage(MessageDeleteRequestDto(chatId = chatId, mtime = 0L))
        }.onSuccessSuspend { chatStore.deleteConversation(conversationId) }
    }

    override suspend fun deleteMessage(conversationId: String, messageId: String): AppResult<Unit> {
        val chatId = conversationId.toLongOrNull() ?: 0L
        val mtime = messageId.toLongOrNull() ?: 0L
        return safeApiCallUnit {
            messageApi.deleteMessage(MessageDeleteRequestDto(chatId = chatId, mtime = mtime))
        }.onSuccessSuspend {
            chatStore.deleteMessage(conversationId, messageId)
        }
    }

    override suspend fun clearUnread(lastSyncMtime: Long): AppResult<Unit> =
        safeApiCallUnit { messageApi.clearUnread() }

    override suspend fun markConversationRead(conversationId: String): AppResult<Unit> {
        val chatId = conversationId.toLongOrNull() ?: 0L
        return safeApiCallUnit {
            messageApi.markRead(ConversationReadRequestDto(chatId = chatId))
        }.onSuccessSuspend {
            chatStore.getConversation(conversationId)?.let {
                chatStore.putConversation(it.copy(unreadCount = 0))
            }
            // Authoritative total (and confirm session count) from backend — no local subtract.
            refreshConversationUnread(conversationId)
        }
    }

    override suspend fun refreshConversationUnread(conversationId: String): AppResult<Unit> {
        val chatId = conversationId.toLongOrNull() ?: return AppResult.BizError(
            AppResult.CODE_EMPTY_PAYLOAD,
            "Invalid conversation id",
        )
        return safeApiCall {
            messageApi.getUnread(MsgUnreadRequestDto(chatId = chatId))
        }.map { dto ->
            chatUnreadStore.update(dto.total)
            chatStore.getConversation(conversationId)?.let { existing ->
                chatStore.putConversation(existing.copy(unreadCount = dto.resolvedCount))
            }
            Unit
        }
    }

    override suspend fun applyIncomingChatPush(push: IncomingChatPush) {
        chatStore.upsertMessages(push.conversationId, listOf(push.message))
        val existing = chatStore.getConversation(push.conversationId)
        val peer = existing?.peer?.let { p ->
            p.copy(
                nickname = push.peerNickname?.takeIf { it.isNotBlank() } ?: p.nickname,
                // Never replace a known avatar with blank / unresolved push fields.
                avatar = push.peerAvatarUrl?.takeIf { it.isNotBlank() } ?: p.avatar,
            )
        } ?: placeholderPeer(push.conversationId, push.peerNickname, push.peerAvatarUrl)
        // Keep cached badge until `/msg/get-unread` overwrites — never local +1.
        chatStore.putConversation(
            Conversation(
                id = push.conversationId,
                peer = peer,
                lastMessage = push.message,
                unreadCount = existing?.unreadCount ?: 0,
                updatedAt = push.message.createdAt,
                isPinned = existing?.isPinned ?: false,
                isMuted = existing?.isMuted ?: false,
            ),
        )
        if (push.fromPeer) {
            refreshConversationUnread(push.conversationId)
        }
    }

    override suspend fun setConversationPinned(conversationId: String, pinned: Boolean): AppResult<Unit> {
        chatStore.getConversation(conversationId)?.let {
            chatStore.putConversation(it.copy(isPinned = pinned))
        }
        return AppResult.Success(Unit)
    }

    override suspend fun setConversationMuted(conversationId: String, muted: Boolean): AppResult<Unit> {
        chatStore.getConversation(conversationId)?.let {
            chatStore.putConversation(it.copy(isMuted = muted))
        }
        return AppResult.Success(Unit)
    }

    override suspend fun deleteLocalConversation(conversationId: String) {
        chatStore.deleteConversation(conversationId)
    }

    override suspend fun translateMessage(
        conversationId: String,
        messageId: String,
        targetLanguage: String?,
    ): AppResult<String> {
        val msg = chatStore.getMessages(conversationId).firstOrNull { it.id == messageId }
            ?: return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, "Message not found")
        return translateText(msg.content, msg.senderId.toLongOrNull() ?: 0L, targetLanguage)
            .onSuccessSuspend { translated ->
                chatStore.upsertMessages(
                    conversationId,
                    listOf(msg.copy(translatedText = translated)),
                )
            }
    }

    override suspend fun translateText(
        content: String,
        fromUid: Long,
        targetLanguage: String?,
        fromType: Int,
    ): AppResult<String> {
        val target = resolveTranslationTarget(targetLanguage)
        return safeApiCall {
            translationApi.submitTranslation(
                TranslationSubmitRequestDto(
                    content = content,
                    fromUid = fromUid,
                    target = target,
                    fromType = fromType,
                ),
            )
        }.map { it.text }
    }

    override suspend fun getLocalConnectionsTotal(ownerUserId: String): Int = chatStore.connectionsTotal()

    private suspend fun sendSimple(
        conversationId: String,
        content: String,
        msgType: Int,
        localMessageId: String?,
    ): AppResult<Message> {
        val local = localOutbound(
            conversationId,
            content,
            msgType.toMessageType(),
            MessageStatus.Sending,
            localMessageId,
        )
        chatStore.upsertMessages(conversationId, listOf(local))
        val targetUid = conversationId.toLongOrNull() ?: 0L
        return safeApiCallNullable {
            messageApi.sendMessage(
                MsgSendRequestDto(
                    chatId = targetUid,
                    data = MsgSendDataDto(
                        msgType = msgType,
                        msgContent = MsgContentDto(content = content),
                    ),
                ),
            )
        }.map { dto ->
            val serverId = dto?.mtime?.toString()
            val serverCreatedAt = dto?.mtime
            // Never let server mtime pull the bubble earlier than the local Sending
            // slot — that reorders the row, inserts a fresh TimeSeparator, and breaks
            // stick-to-bottom scroll after success.
            val createdAt = maxOf(
                local.createdAt.toMessageTimelineMicros(),
                (serverCreatedAt ?: 0L).toMessageTimelineMicros(),
            ).takeIf { it > 0L } ?: local.createdAt
            val sent = local.copy(
                id = serverId ?: local.id,
                status = MessageStatus.Sent,
                createdAt = createdAt,
            )
            // Promote the same outbound bubble: drop the local Sending row when the
            // server assigns a new id/timestamp, then upsert the Sent version so the
            // UI does not briefly show two copies.
            if (sent.id != local.id) {
                chatStore.deleteMessage(conversationId, local.id)
            }
            chatStore.upsertMessages(conversationId, listOf(sent))
            sent
        }.let { result ->
            if (result is AppResult.Failure) {
                chatStore.upsertMessages(
                    conversationId,
                    listOf(local.copy(status = MessageStatus.Failed)),
                )
            }
            result
        }
    }

    private suspend fun sendMedia(
        conversationId: String,
        localUriOrPath: String,
        msgType: Int,
        localMessageId: String?,
        durationSec: Int = 0,
    ): AppResult<Message> {
        val local = localOutbound(
            conversationId = conversationId,
            content = localUriOrPath,
            type = msgType.toMessageType(),
            status = MessageStatus.Sending,
            localMessageId = localMessageId,
        )
        chatStore.upsertMessages(conversationId, listOf(local))

        val uploaded = when (msgType) {
            MsgSendRequestDto.MSG_TYPE_VIDEO ->
                mediaUploadService.uploadVideoIfNeeded(localUriOrPath, flag = "chat")
            else ->
                mediaUploadService.uploadImageIfNeeded(localUriOrPath, flag = "chat")
        }
        when (uploaded) {
            is AppResult.Failure -> {
                chatStore.upsertMessages(
                    conversationId,
                    listOf(local.copy(status = MessageStatus.Failed)),
                )
                return uploaded
            }
            is AppResult.Success -> {
                val data = uploaded.data
                val remote = data.remotePath.toRelativeMediaPathOrNull() ?: data.remotePath
                val small = data.smallUrl?.let { it.toRelativeMediaPathOrNull() ?: it }
                val cover = data.coverPath?.let { it.toRelativeMediaPathOrNull() ?: it }
                val duration = data.durationSec?.takeIf { it > 0 }
                    ?: durationSec.takeIf { it > 0 }
                val msgContent = when (msgType) {
                    MsgSendRequestDto.MSG_TYPE_IMAGE -> MsgContentDto(
                        imageUrl = remote,
                        smallUrl = small,
                        imageWidth = data.width,
                        imageHeight = data.height,
                        uploadId = data.uploadId.takeIf { it > 0 },
                    )
                    MsgSendRequestDto.MSG_TYPE_VIDEO -> MsgContentDto(
                        url = remote,
                        cover = cover ?: small,
                        duration = duration,
                        width = data.width,
                        height = data.height,
                        uploadId = data.uploadId.takeIf { it > 0 },
                    )
                    MsgSendRequestDto.MSG_TYPE_VOICE -> MsgContentDto(
                        url = remote,
                        duration = duration,
                    )
                    else -> MsgContentDto(content = remote)
                }
                val displayContent = when (msgType) {
                    MsgSendRequestDto.MSG_TYPE_IMAGE -> mediaWireBody(
                        "image_url" to remote,
                        "small_url" to small,
                    )
                    MsgSendRequestDto.MSG_TYPE_VIDEO -> mediaWireBody(
                        "url" to remote,
                        "cover" to (cover ?: small),
                        "duration" to duration,
                    )
                    else -> remote
                }
                val pending = local.copy(content = displayContent)
                chatStore.upsertMessages(conversationId, listOf(pending))
                val targetUid = conversationId.toLongOrNull() ?: 0L
                return safeApiCallNullable {
                    messageApi.sendMessage(
                        MsgSendRequestDto(
                            chatId = targetUid,
                            data = MsgSendDataDto(
                                msgType = msgType,
                                msgContent = msgContent,
                            ),
                        ),
                    )
                }.map { dto ->
                    val serverId = dto?.mtime?.toString()
                    val serverCreatedAt = dto?.mtime
                    val createdAt = maxOf(
                        pending.createdAt.toMessageTimelineMicros(),
                        (serverCreatedAt ?: 0L).toMessageTimelineMicros(),
                    ).takeIf { it > 0L } ?: pending.createdAt
                    val sent = pending.copy(
                        id = serverId ?: pending.id,
                        status = MessageStatus.Sent,
                        createdAt = createdAt,
                    )
                    if (sent.id != pending.id) {
                        chatStore.deleteMessage(conversationId, pending.id)
                    }
                    chatStore.upsertMessages(conversationId, listOf(sent))
                    sent
                }.let { result ->
                    if (result is AppResult.Failure) {
                        chatStore.upsertMessages(
                            conversationId,
                            listOf(pending.copy(status = MessageStatus.Failed)),
                        )
                    }
                    result
                }
            }
        }
    }

    /** Minimal JSON body so chat bubbles can resolve CDN URLs before sync returns. */
    private fun mediaWireBody(vararg fields: Pair<String, Any?>): String {
        val parts = fields.mapNotNull { (key, value) ->
            when (value) {
                null -> null
                is String -> value.takeIf { it.isNotBlank() }?.let {
                    "\"$key\":\"${it.jsonEscape()}\""
                }
                is Number -> "\"$key\":$value"
                else -> "\"$key\":\"${value.toString().jsonEscape()}\""
            }
        }
        return parts.joinToString(prefix = "{", postfix = "}")
    }

    private fun String.jsonEscape(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")

    private suspend fun localOutbound(
        conversationId: String,
        content: String,
        type: MessageType,
        status: MessageStatus,
        localMessageId: String? = null,
    ): Message {
        // Persist in the same microsecond timeline as server `mtime` so pending rows
        // sort with history after process death / re-enter, and cannot become MIN/MAX
        // cursors that dwarf or undercut server timestamps.
        val latestMicros = chatStore.getMessages(conversationId)
            .maxOfOrNull { it.createdAt.toMessageTimelineMicros() }
            ?: 0L
        val nowMicros = System.currentTimeMillis().toMessageTimelineMicros()
        val createdAt = maxOf(latestMicros + 1L, nowMicros)
        return Message(
            id = localMessageId ?: "local_${UUID.randomUUID()}",
            conversationId = conversationId,
            senderId = sessionManager.currentUserId.orEmpty(),
            content = content,
            type = type,
            status = status,
            createdAt = createdAt,
        )
    }

    private fun placeholderPeer(
        conversationId: String,
        nickname: String? = null,
        avatar: String? = null,
    ): User = User(
        id = conversationId,
        nickname = nickname ?: conversationId,
        avatar = avatar,
        gender = Gender.Other,
        age = 0,
        bio = "",
        isOnline = false,
        lastActiveAt = 0L,
    )

    private fun Int.toMessageType(): MessageType = when (this) {
        MsgSendRequestDto.MSG_TYPE_VOICE -> MessageType.Voice
        MsgSendRequestDto.MSG_TYPE_IMAGE -> MessageType.Image
        MsgSendRequestDto.MSG_TYPE_VIDEO -> MessageType.Video
        MsgSendRequestDto.MSG_TYPE_EMOJI -> MessageType.Emoji
        MsgSendRequestDto.MSG_TYPE_GIFT -> MessageType.Gift
        else -> MessageType.Text
    }
}
