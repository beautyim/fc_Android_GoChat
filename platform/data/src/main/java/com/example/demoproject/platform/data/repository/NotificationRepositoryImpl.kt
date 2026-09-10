package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.Notification
import com.example.demoproject.platform.data.model.NotificationKind
import com.example.demoproject.platform.data.model.NotificationSegment
import com.example.demoproject.platform.data.network.api.NotificationApi
import com.example.demoproject.platform.data.network.dto.MsgNoticeDto
import com.example.demoproject.platform.data.network.dto.MsgNoticeListRequestDto
import com.example.demoproject.platform.data.model.toMessageTimelineMillis
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.safeApiCall

class NotificationRepositoryImpl(
    private val notificationApi: NotificationApi,
) : NotificationRepository {
    @Volatile private var lastMtime: Long = 0L

    override suspend fun getNotifications(page: Int): AppResult<List<Notification>> {
        val requestMtime = if (page <= 1) 0L else lastMtime
        return safeApiCall {
            notificationApi.getClubNotices(
                MsgNoticeListRequestDto(
                    mtime = requestMtime,
                    lastMtime = requestMtime,
                ),
            )
        }.map { dto ->
            if (page <= 1) {
                lastMtime = dto.lastMtime
            } else if (dto.lastMtime > 0L) {
                lastMtime = dto.lastMtime
            }
            dto.list.map { it.toDomain() }
        }
    }

    override suspend fun markAllAsRead(): AppResult<Unit> =
        // No dedicated mark-all endpoint in current API surface; treat as local success.
        AppResult.Success(Unit)
}

private fun MsgNoticeDto.toDomain(): Notification {
    val kind = when (msgType) {
        1 -> NotificationKind.AvatarLike
        2 -> NotificationKind.AvatarComment
        3 -> NotificationKind.AvatarFollow
        else -> NotificationKind.EmojiSystem
    }
    return Notification(
        id = mtime.toString(),
        kind = kind,
        actor = null,
        emoji = if (kind == NotificationKind.EmojiSystem || kind == NotificationKind.EmojiReaction) "🔔" else null,
        titleSegments = listOf(
            NotificationSegment(
                text = msgContent.ifBlank { "Notification" },
                emphasized = false,
            ),
        ),
        quotedText = null,
        createdAt = mtime.toMessageTimelineMillis(),
        isRead = msgStatus == 1,
    )
}
