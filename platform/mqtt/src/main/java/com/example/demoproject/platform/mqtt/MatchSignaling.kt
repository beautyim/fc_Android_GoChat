package com.example.demoproject.platform.mqtt

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class MatchSignalPeer(
    val userId: String,
    val nickname: String = "",
    val avatarUrl: String = "",
    val age: Int = 0,
    val subscribeUid: Long = 0L,
)

data class MatchSignalRoom(
    val roomId: String,
    val channelId: String,
    val rtcToken: String,
    val rtcAppId: String = "",
    val localRtcUid: Int = 0,
    val roomSessionId: Long = 0L,
    val fencingToken: String? = null,
    val matchTimeSeconds: Int = 0,
    val nextTimeSeconds: Int = 0,
    val rtcMode: Int = 0,
    val isAmv: Boolean = false,
) {
    val canJoinDirectly: Boolean
        get() = roomId.isNotBlank() && channelId.isNotBlank() && rtcToken.isNotBlank()

    val isReceiveOnly: Boolean
        get() = rtcMode == RTC_MODE_AUDIENCE && isAmv

    private companion object {
        const val RTC_MODE_AUDIENCE = 2
    }
}

sealed interface MatchSignal {
    data class Success(
        val matchSessionId: Long? = null,
        val matchId: Long? = null,
        val peer: MatchSignalPeer? = null,
        val room: MatchSignalRoom? = null,
    ) : MatchSignal

    data class Notice(val message: String, val keepMatching: Boolean) : MatchSignal

    data class Terminal(
        val kind: Kind,
        val matchSessionId: Long? = null,
        val endType: Int? = null,
    ) : MatchSignal {
        enum class Kind { Empty, End, Left }
    }

    data class RoomReady(
        val peer: MatchSignalPeer? = null,
        val room: MatchSignalRoom,
    ) : MatchSignal

    data class PreJoinEnded(
        val roomId: String,
        val endType: Int? = null,
    ) : MatchSignal
}

object MatchSignalParser {
    fun parse(raw: String, json: Json): MatchSignal? {
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
            ?: return null
        return when (root.int("type")) {
            TYPE_MATCH -> parseMatchControl(root)
            TYPE_ACCOUNT -> parseAccountFallback(root)
            TYPE_CALL -> parseMatchCall(root)
            else -> null
        }
    }

    private fun parseMatchControl(root: JsonObject): MatchSignal? {
        val envelope = root.obj("data") ?: return null
        if (envelope.int("a_type") != MATCH_CONTROL_A_TYPE) return null
        val payload = envelope.obj("data") ?: envelope
        val key = payload.int("key")
        return when (key) {
            KEY_SUCCESS -> payload.toSuccess()
            KEY_NOTICE -> payload.notice(keepMatching = true)
            KEY_EMPTY -> payload.terminal(MatchSignal.Terminal.Kind.Empty)
            KEY_END -> payload.terminal(MatchSignal.Terminal.Kind.End)
            KEY_LEFT -> payload.terminal(MatchSignal.Terminal.Kind.Left)
            KEY_POP -> payload.notice(keepMatching = true)
            null -> when {
                payload.hasPeerPayload() -> payload.toSuccess()
                payload.int("end_type") != null ->
                    payload.terminal(MatchSignal.Terminal.Kind.End)
                else -> null
            }
            else -> null
        }
    }

    private fun parseAccountFallback(root: JsonObject): MatchSignal? {
        val payload = root.obj("data") ?: return null
        if (payload.int("type") != ACCOUNT_MATCH_SUCCESS) return null
        return payload.toSuccess()
    }

    private fun parseMatchCall(root: JsonObject): MatchSignal? {
        val packet = root.obj("data") ?: return null
        return when (packet.int("a_type")) {
            CALL_INVITE_A_TYPE -> {
                val peerPayload = packet.peerPayload()
                val room = packet.toRoom(timing = peerPayload ?: packet) ?: return null
                val peer = peerPayload?.toPeer()
                if (packet.int("is_match_call") != 1 && peer == null) return null
                MatchSignal.RoomReady(peer = peer, room = room)
            }
            CALL_END_A_TYPE, CALL_END_COMPAT_A_TYPE -> {
                if (packet.int("is_match_call") != 1) return null
                MatchSignal.PreJoinEnded(
                    roomId = packet.str("room_id").orEmpty(),
                    endType = packet.int("end_type"),
                )
            }
            else -> null
        }
    }

    private fun JsonObject.toSuccess(): MatchSignal.Success {
        val peerPayload = peerPayload()
        // Backend puts match_time / next_time on the matched user, not on room_info.
        val timingSource = peerPayload ?: this
        return MatchSignal.Success(
            matchSessionId = longAny("match_session_id", "session_id", "id"),
            matchId = longAny("match_id"),
            peer = peerPayload?.toPeer(),
            room = (obj("room_info") ?: obj("room"))?.toRoom(timing = timingSource),
        )
    }

    private fun JsonObject.notice(keepMatching: Boolean): MatchSignal.Notice? {
        val message = strAny("msg", "message", "content", "text", "title").orEmpty()
        return message.takeIf { it.isNotBlank() }?.let {
            MatchSignal.Notice(it, keepMatching)
        }
    }

    private fun JsonObject.terminal(kind: MatchSignal.Terminal.Kind) =
        MatchSignal.Terminal(
            kind = kind,
            matchSessionId = longAny("match_session_id", "session_id"),
            endType = int("end_type"),
        )

    private fun JsonObject.peerPayload(): JsonObject? {
        val user = obj("user_info")
            ?: obj("match_info")
            ?: obj("user")
            ?: obj("matched_user")
            ?: takeIf { !strAny("user_id", "uid").isNullOrBlank() }
            ?: return null
        val uid = user.strAny("user_id", "uid", "id").orEmpty()
        return user.takeIf { uid.isNotBlank() && uid != "0" }
    }

    private fun JsonObject.toPeer(): MatchSignalPeer? {
        val uid = strAny("user_id", "uid", "id").orEmpty()
        if (uid.isBlank() || uid == "0") return null
        return MatchSignalPeer(
            userId = uid,
            nickname = strAny("nickname", "name").orEmpty(),
            avatarUrl = strAny("small_avatar", "avatar", "avatar_url").orEmpty(),
            age = int("age") ?: ageFromBirthday(str("birthday")),
            subscribeUid = longAny("subscribe_uid") ?: 0L,
        )
    }

    private fun JsonObject.toRoom(timing: JsonObject): MatchSignalRoom? {
        val roomId = strAny("room_id", "room_session_id").orEmpty()
        if (roomId.isBlank() || roomId == "0") return null
        return MatchSignalRoom(
            roomId = roomId,
            channelId = strAny("channel", "channel_name").orEmpty(),
            rtcToken = strAny("token", "rtc_token").orEmpty(),
            rtcAppId = strAny("app_id", "rtc_app_id").orEmpty(),
            localRtcUid = intAny("uid", "rtc_uid", "target_uid") ?: 0,
            roomSessionId = longAny("room_session_id") ?: 0L,
            fencingToken = str("fencing_token"),
            matchTimeSeconds = intAny("match_time", "matchTime")
                ?: timing.intAny("match_time", "matchTime")
                ?: 0,
            nextTimeSeconds = intAny("next_time", "nextTime")
                ?: timing.intAny("next_time", "nextTime")
                ?: 0,
            rtcMode = intAny("rtc_mode", "rtcMode") ?: 0,
            isAmv = (intAny("is_amv", "isAmv") ?: 0) == 1,
        )
    }

    /** `user_info` often carries `birthday` instead of a resolved `age`. */
    private fun ageFromBirthday(birthday: String?): Int {
        val year = birthday?.take(4)?.toIntOrNull() ?: return 0
        val month = birthday.drop(5).take(2).toIntOrNull() ?: 1
        val day = birthday.drop(8).take(2).toIntOrNull() ?: 1
        val today = java.time.LocalDate.now()
        val born = runCatching { java.time.LocalDate.of(year, month, day) }.getOrNull() ?: return 0
        return java.time.Period.between(born, today).years.coerceIn(0, 120)
    }

    private fun JsonObject.hasPeerPayload(): Boolean =
        obj("user_info") != null ||
            obj("match_info") != null ||
            obj("matched_user") != null

    private fun JsonObject.obj(key: String): JsonObject? {
        val element = this[key] ?: return null
        return when (element) {
            is JsonObject -> element
            is JsonPrimitive -> {
                val raw = element.contentOrNull?.trim().orEmpty()
                if (!raw.startsWith("{")) null
                else runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull()
            }
            else -> null
        }
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }

    private fun JsonObject.strAny(vararg keys: String): String? {
        keys.forEach { key -> str(key)?.let { return it } }
        return null
    }

    private fun JsonObject.int(key: String): Int? =
        runCatching { this[key]?.jsonPrimitive?.intOrNull }.getOrNull()
            ?: str(key)?.toDoubleOrNull()?.toInt()

    private fun JsonObject.intAny(vararg keys: String): Int? {
        keys.forEach { key -> int(key)?.let { return it } }
        return null
    }

    private fun JsonObject.longAny(vararg keys: String): Long? {
        keys.forEach { key ->
            val element: JsonElement = this[key] ?: return@forEach
            val value = runCatching { element.jsonPrimitive.longOrNull }.getOrNull()
                ?: (element as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()?.toLong()
            value?.let { return it }
        }
        return null
    }

    private const val TYPE_CALL = 3
    private const val TYPE_MATCH = 4
    private const val TYPE_ACCOUNT = 7
    private const val MATCH_CONTROL_A_TYPE = 2
    private const val CALL_INVITE_A_TYPE = 1
    private const val CALL_END_A_TYPE = 2
    private const val CALL_END_COMPAT_A_TYPE = 3
    private const val ACCOUNT_MATCH_SUCCESS = 25
    private const val KEY_SUCCESS = 1
    private const val KEY_NOTICE = 2
    private const val KEY_EMPTY = 3
    private const val KEY_END = 4
    private const val KEY_LEFT = 5
    private const val KEY_POP = 6
}
