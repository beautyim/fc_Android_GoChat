package com.example.demoproject.platform.data.network.mapper

import com.example.demoproject.platform.data.model.CallRecord
import com.example.demoproject.platform.data.model.CallRecordDirection
import com.example.demoproject.platform.data.model.CallRecordPage
import com.example.demoproject.platform.data.model.CallRecordStatus
import com.example.demoproject.platform.data.model.CallRecordType
import com.example.demoproject.platform.data.model.CallRoom
import com.example.demoproject.platform.data.model.Gender
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.data.network.dto.CallRecordDto
import com.example.demoproject.platform.data.network.dto.CallRecordsResponseDto
import com.example.demoproject.platform.data.network.dto.CallRoomDto
import com.example.demoproject.platform.data.network.dto.UserDto

// ── Call room ──

fun CallRoomDto.toDomain(now: java.time.LocalDate = java.time.LocalDate.now()): CallRoom =
    CallRoom(
        roomId = roomId,
        channel = effectiveChannel,
        appId = appId,
        token = token,
        uid = effectiveUid,
        callType = type,
        peer = userInfo?.toDomain(now),
        serverTimeMs = serverTimeMs,
        httpRoomId = httpRoomId.trim(),
        fencingToken = fencingToken?.trim()?.takeIf { it.isNotEmpty() },
        callFreeMin = userInfo?.callFreeMin ?: 0,
    )

// ── Call records ──

fun CallRecordsResponseDto.toDomainPage(
    now: java.time.LocalDate = java.time.LocalDate.now(),
): CallRecordPage {
    val peers: Map<String, User> = userInfos.mapValues { it.value.toDomain(now) }
    return CallRecordPage(
        records = list.map { item ->
            val peer = peers[item.uid.toString()] ?: placeholderPeerForCallRecord(item.uid)
            item.toDomain(peer)
        },
        hasMore = hasMore == 1,
    )
}

private fun CallRecordDto.toDomain(peer: User): CallRecord =
    CallRecord(
        id = id,
        peer = peer,
        roomId = roomId,
        callType = callType.toCallRecordType(),
        status = toCallRecordStatus(),
        direction = callDirection.toCallRecordDirection(),
        description = callDesc.ifBlank { totalTime },
        startedAtSeconds = addTime,
        callPrice = callPrice,
        isMatch = isMatch == 1 || isMatchCall == 1,
    )

private fun Int.toCallRecordType(): CallRecordType = when (this) {
    2 -> CallRecordType.Voice
    else -> CallRecordType.Video
}

private fun Int.toCallRecordDirection(): CallRecordDirection = when (this) {
    1 -> CallRecordDirection.Incoming
    else -> CallRecordDirection.Outgoing
}

private fun CallRecordDto.toCallRecordStatus(): CallRecordStatus {
    val normalizedDesc = callDesc.lowercase()
    return when {
        isMatch == 1 || isMatchCall == 1 -> CallRecordStatus.Match
        "miss" in normalizedDesc -> CallRecordStatus.Missed
        "cancel" in normalizedDesc -> CallRecordStatus.Cancelled
        "declin" in normalizedDesc || "reject" in normalizedDesc -> CallRecordStatus.Declined
        totalTime.isNotBlank() -> CallRecordStatus.Connected
        callStatus == 3 -> CallRecordStatus.Missed
        callStatus == 4 -> CallRecordStatus.Cancelled
        callStatus == 5 -> CallRecordStatus.Match
        status == 7 -> CallRecordStatus.Connected
        else -> CallRecordStatus.Unknown
    }
}

private fun placeholderPeerForCallRecord(uid: Long): User = User(
    id = uid.toString(),
    nickname = "",
    avatar = null,
    gender = Gender.Other,
    age = 0,
    bio = "",
    isOnline = false,
    lastActiveAt = 0L,
)

