package com.example.demoproject.platform.data.network.mapper

import com.example.demoproject.platform.data.model.Gender
import com.example.demoproject.platform.data.network.toPicUrlOrNull
import com.example.demoproject.platform.data.repository.MatchStartAction
import com.example.demoproject.platform.data.repository.MatchStartCandidate
import com.example.demoproject.platform.data.repository.MatchStartInfo
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object MatchStartPayloadParser {

    fun parse(payload: JsonElement?, rawPayload: String? = null): MatchStartInfo {
        return parse(payload = payload, rawPayload = rawPayload, includeMatchedUser = true)
    }

    fun parseNext(payload: JsonElement?, rawPayload: String? = null): MatchStartInfo {
        return parse(payload = payload, rawPayload = rawPayload, includeMatchedUser = false)
    }

    private fun parse(
        payload: JsonElement?,
        rawPayload: String?,
        includeMatchedUser: Boolean,
    ): MatchStartInfo {
        val root = payload.asObjectOrNull() ?: return emptyInfo(rawPayload)
        val sessionId = findLong(root, SESSION_ID_KEYS)
        val matchId = findLong(root, MATCH_ID_KEYS)
        val matchFreeCount = findInt(root, MATCH_FREE_COUNT_KEYS)
        val heartIntervalSeconds = findInt(root, HEART_INTERVAL_KEYS)?.takeIf { it > 0 }
        val matchedUser = if (includeMatchedUser) findMatchedUser(root) else null
        val nextAction = if (matchedUser != null) {
            MatchStartAction.UserProfile
        } else {
            MatchStartAction.DirectVideoCall
        }
        return MatchStartInfo(
            sessionId = sessionId,
            matchId = matchId,
            matchedUser = matchedUser,
            nextAction = nextAction,
            rawPayload = rawPayload,
            matchFreeCount = matchFreeCount,
            heartIntervalSeconds = heartIntervalSeconds,
        )
    }

    private fun emptyInfo(rawPayload: String?) = MatchStartInfo(
        sessionId = null,
        matchId = null,
        matchedUser = null,
        nextAction = MatchStartAction.DirectVideoCall,
        rawPayload = rawPayload,
    )

    private fun findMatchedUser(root: JsonObject): MatchStartCandidate? {
        val direct = root["match_info"]?.asObjectOrNull()
            ?: root["matchInfo"]?.asObjectOrNull()
        val userObject = direct ?: findNestedUserObject(root)
        return userObject?.toMatchStartCandidate()
    }

    private fun findNestedUserObject(root: JsonObject): JsonObject? {
        USER_OBJECT_KEYS.forEach { key ->
            root[key]?.asObjectOrNull()?.let { return it }
        }
        root.values.forEach { element ->
            val nested = element.asObjectOrNull() ?: return@forEach
            USER_OBJECT_KEYS.forEach { key ->
                nested[key]?.asObjectOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun JsonObject.toMatchStartCandidate(): MatchStartCandidate? {
        val id = findLong(this, USER_ID_KEYS) ?: return null
        val nickname = findString(this, NICKNAME_KEYS).orEmpty().ifBlank { "User" }
        val avatarUrl = findString(this, AVATAR_KEYS)?.toPicUrlOrNull()
        val gender = findInt(this, GENDER_KEYS)?.toGender() ?: Gender.Other
        val age = findInt(this, AGE_KEYS)
        val bio = findString(this, BIO_KEYS).orEmpty()
        return MatchStartCandidate(
            id = id,
            nickname = nickname,
            avatarUrl = avatarUrl,
            gender = gender,
            age = age,
            bio = bio,
            profileUserId = findString(this, PROFILE_USER_ID_KEYS) ?: id.toString(),
            videoCallGold = findInt(this, VIDEO_CALL_GOLD_KEYS) ?: 0,
        )
    }

    private fun Int.toGender(): Gender = when (this) {
        1 -> Gender.Male
        2 -> Gender.Female
        else -> Gender.Other
    }

    private fun findLong(root: JsonObject, keys: List<String>): Long? {
        keys.forEach { key ->
            root[key]?.primitiveOrNull()?.longOrNull?.let { return it }
        }
        root.values.forEach { element ->
            val nested = element.asObjectOrNull() ?: return@forEach
            keys.forEach { key ->
                nested[key]?.primitiveOrNull()?.longOrNull?.let { return it }
            }
        }
        return null
    }

    private fun findInt(root: JsonObject, keys: List<String>): Int? {
        keys.forEach { key ->
            root[key]?.primitiveOrNull()?.intOrNull?.let { return it }
        }
        return null
    }

    private fun findString(root: JsonObject, keys: List<String>): String? {
        keys.forEach { key ->
            root[key]?.primitiveOrNull()?.contentOrNull?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    private fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

    private fun JsonElement.primitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive

    private val SESSION_ID_KEYS = listOf(
        "match_session_id",
        "matchSessionId",
        "session_id",
        "sessionId",
        "id",
    )

    private val MATCH_ID_KEYS = listOf(
        "match_id",
        "matchId",
    )

    private val MATCH_FREE_COUNT_KEYS = listOf(
        "match_free_count",
        "matchFreeCount",
    )

    private val HEART_INTERVAL_KEYS = listOf(
        "heart_interval",
        "heartInterval",
        "heart_interval_sec",
    )

    private val USER_OBJECT_KEYS = listOf(
        "user_info",
        "user",
        "target_user",
        "match_user",
        "matched_user",
        "peer",
    )

    private val USER_ID_KEYS = listOf(
        "uid",
        "target_uid",
        "targetUid",
        "match_uid",
        "matchUid",
        "id",
    )

    private val PROFILE_USER_ID_KEYS = listOf(
        "user_id",
        "userId",
    )

    private val NICKNAME_KEYS = listOf("nickname", "name", "username")
    private val AVATAR_KEYS = listOf("avatar", "small_avatar", "avatar_url", "head_img")
    private val GENDER_KEYS = listOf("sex", "gender")
    private val AGE_KEYS = listOf("age")
    private val BIO_KEYS = listOf("about_me", "bio", "sign")
    private val VIDEO_CALL_GOLD_KEYS = listOf(
        "video_call_gold",
        "videoCallGold",
        "video_gold",
        "videoGold",
        "call_price",
        "callPrice",
    )
}
