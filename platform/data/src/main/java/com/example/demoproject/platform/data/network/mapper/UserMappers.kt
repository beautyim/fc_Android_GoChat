package com.example.demoproject.platform.data.network.mapper

import com.example.demoproject.platform.data.model.Gender
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.data.network.toPicUrlOrNull
import com.example.demoproject.platform.data.network.dto.UserDto

// ── User ──

fun UserDto.toDomain(now: java.time.LocalDate = java.time.LocalDate.now()): User = User(
    id = uid.takeIf { it != 0L }?.toString() ?: userId.ifBlank { nickname },
    nickname = nickname,
    avatar = smallAvatar.toPicUrlOrNull() ?: avatar.toPicUrlOrNull(),
    gender = sex.toGender(),
    age = birthday.toAge(now).takeIf { it > 0 } ?: age.coerceAtLeast(0),
    bio = (aboutMe ?: sign).orEmpty(),
    birthday = birthday?.takeIf { it.isNotBlank() },
    email = email?.takeIf { it.isNotBlank() },
    vipExpireTime = (vipExpireTime ?: vipExp)?.takeIf { it.isNotBlank() },
    countryCode = countryCode?.takeIf { it.isNotBlank() },
    countryName = countryName?.takeIf { it.isNotBlank() },
    isAuthVerified = isAuth,
    isOnline = isOnline,
    // YAML exposes only `online_status` (a state code) — no last-seen
    // timestamp. Keep the domain field for compatibility; it is populated
    // with 0L until the backend surfaces the data.
    lastActiveAt = 0L,
    isVip = isVip,
    rechargeMoney = rechargeMoney,
    onlineStatusCode = onlineStatus,
    isVideoCallAvailable = canVideoCall,
    isVoiceCallAvailable = canVoiceCall,
    videoCallGold = videoCallGold,
    voiceCallGold = voiceCallGold,
    isFollowing = isFollow,
    isFollowedBy = isFollowed,
    isBlockedByPeer = isBeBlack,
    // Public `user_id` for /home/info; profile card UI displays `uid` separately.
    externalUserId = userId,
)

/** `sex` 1-male / 2-female / 3-non-binary (per `/perfect/run`). */
private fun Int.toGender(): Gender = when (this) {
    1 -> Gender.Male
    2 -> Gender.Female
    else -> Gender.Other
}

/**
 * Parses a birthday into whole years at [now]. Accepts `YYYY-MM-DD` and
 * `yyyy-MM-dd HH:mm:ss`. Returns `0` when missing or unparseable —
 * callers should treat 0 as "unknown" rather than "newborn".
 */
private fun String?.toAge(now: java.time.LocalDate): Int {
    if (this.isNullOrBlank()) return 0
    val birth = parseBirthdayDate(this) ?: return 0
    return java.time.Period.between(birth, now).years.coerceAtLeast(0)
}

private fun parseBirthdayDate(raw: String): java.time.LocalDate? {
    val trimmed = raw.trim()
    return runCatching { java.time.LocalDate.parse(trimmed) }.getOrNull()
        ?: runCatching {
            java.time.LocalDateTime.parse(trimmed, BIRTHDAY_DATE_TIME_FORMATTER).toLocalDate()
        }.getOrNull()
        ?: trimmed.take(10).takeIf { it.length == 10 }?.let { head ->
            runCatching { java.time.LocalDate.parse(head) }.getOrNull()
        }
}

private val BIRTHDAY_DATE_TIME_FORMATTER: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

