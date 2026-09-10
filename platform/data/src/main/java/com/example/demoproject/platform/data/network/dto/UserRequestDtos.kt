package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.network.dto.ApiBusinessMessage
import com.example.demoproject.platform.network.paging.OffsetPageRequest
import com.example.demoproject.platform.network.serializer.LenientBooleanSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Envelope for profile payloads that nest the user under `user_info`.
 *
 * The backend nests the actual user payload inside `user_info`:
 * ```json
 * {
 *   "is_follow": false, // or 1 / 0 on `/user/my`
 *   "is_black": false,  // or 1 / 0 on `/user/my`
 *   "user_info": { "uid": ..., "nickname": ..., ... },
 *   "photo_count": 0,
 *   "video_count": 0,
 *   "flash_chat_count": 0
 * }
 * ```
 */
@Serializable
data class UserProfileResultDto(
    @SerialName("user_info") val userInfo: UserDto = UserDto(),
    @Serializable(with = LenientBooleanSerializer::class)
    @SerialName("is_follow") val isFollow: Boolean = false,
    @Serializable(with = LenientBooleanSerializer::class)
    @SerialName("is_black") val isBlack: Boolean = false,
    @SerialName("photo_count") val photoCount: Int = 0,
    @SerialName("video_count") val videoCount: Int = 0,
    @SerialName("flash_chat_count") val flashChatCount: Int = 0,
)

/**
 * Body for `POST /home/list` — discover home users.
 */
@Serializable
data class HomeListRequestDto(
    /** 1 online hosts, 2 recommended, 3 offline, 4 following. */
    val flag: Int = FLAG_RECOMMENDED,
    val page: Int = OffsetPageRequest.FIRST_PAGE,
    @SerialName("filter_list") val filterList: JsonElement = JsonObject(emptyMap()),
) {
    companion object {
        const val FLAG_ONLINE_HOSTS: Int = 1
        const val FLAG_RECOMMENDED: Int = 2
        const val FLAG_OFFLINE: Int = 3
        const val FLAG_FOLLOWING: Int = 4
    }
}

/** Body for `POST /focus/follow`. */
@Serializable
data class FocusFollowRequestDto(
    @SerialName("target_uid") val targetUid: Long,
    val flag: String = FLAG_ADD,
) {
    companion object {
        const val FLAG_ADD: String = "add"
        const val FLAG_DELETE: String = "del"
    }
}

/** Body for `POST /user/search`. */
@Serializable
data class UserSearchRequestDto(
    @SerialName("search_uid") val searchUid: Long,
    @SerialName("club_id") val clubId: Long = 0,
)

@Serializable
data class UserSearchResponseDto(
    @SerialName("user_info") val userInfo: UserDto = UserDto(),
)

/** Body for `POST /focus/list` and `POST /focus/fans-list`. */
@Serializable
data class FocusListRequestDto(
    val page: Int = 1,
    @SerialName("target_uid") val targetUid: Long? = null,
    @SerialName("search_uid") val searchUid: Long? = null,
)

/** Body for `POST /user/visitor`. */
@Serializable
data class VisitorListRequestDto(
    val page: Int = 1,
)

/** Body for `POST /user/black-list`. */
@Serializable
data class BlackListRequestDto(
    val page: Int = 1,
)

/** Body for `POST /user/black`. */
@Serializable
data class UserBlackRequestDto(
    @SerialName("target_uid") val targetUid: Long,
    val flag: String = FLAG_ADD,
) {
    companion object {
        const val FLAG_ADD: String = "add"
        const val FLAG_DELETE: String = "del"
    }
}

/** Body for `POST privacy/update`. */
@Serializable
data class PrivacyUpdateRequestDto(
    val type: Int,
    val status: Int,
) {
    companion object {
        const val TYPE_HIDE_ONLINE_STATUS: Int = 14
        const val STATUS_ON: Int = 1
        const val STATUS_OFF: Int = 0
    }
}

/** Body for `POST profile/update-email-code` and `POST profile/change-pwd-code`. */
@Serializable
data class ProfileEmailCodeRequestDto(
    val email: String,
)

@Serializable
data class ProfileEmailCodeResponseDto(
    val callback: ApiCallbackDto? = null,
) : ApiBusinessMessage {
    override val message: String
        get() = callback?.funcData?.message.orEmpty()
}

/** Body for `POST profile/update-email`. */
@Serializable
data class ProfileUpdateEmailRequestDto(
    val email: String,
    val password: String? = null,
    @SerialName("email_code") val emailCode: Int,
)

/** Body for `POST profile/change-pwd`. */
@Serializable
data class ProfileChangePasswordRequestDto(
    val email: String,
    val code: String,
    @SerialName("old_password") val oldPassword: String? = null,
    val password: String,
)

/** Body for `POST user/sync-permission`; all fields are optional patches. */
@Serializable
data class UserSyncPermissionRequestDto(
    val notice: Int? = null,
)

/**
 * `/home/list` returns `{list, has_more}` under the common API payload.
 * Keep the serializer flexible because existing middlewares may wrap the
 * payload as a JSON string or nest it under `data` / `result`.
 */
@Serializable(with = UserListResponseDtoSerializer::class)
data class UserListResponseDto(
    val list: List<UserDto> = emptyList(),
    @SerialName("has_more") val hasMoreRaw: Int = 0,
    val total: Int? = null,
    @SerialName("total_num") val totalNum: Int? = null,
) {
    val hasMore: Boolean get() = hasMoreRaw == 1
    val resolvedTotal: Int get() = totalNum ?: total ?: 0
}

private object UserListResponseDtoSerializer : KSerializer<UserListResponseDto> {
    // Reuse the surrogate's structured descriptor so that composition
    // (e.g. inside `ApiResponse<UserListResponseDto>`) sees a CLASS kind
    // rather than a primitive — otherwise nested decoders complain that
    // the expected kind is wrong.
    override val descriptor: SerialDescriptor =
        UserListResponseDtoSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): UserListResponseDto {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("UserListResponseDtoSerializer only supports JSON")
        val element = unwrapJsonIfString(jsonDecoder, jsonDecoder.decodeJsonElement())
        return decodeUserListResponse(jsonDecoder, element)
    }

    private fun decodeUserListResponse(
        jsonDecoder: JsonDecoder,
        element: JsonElement,
    ): UserListResponseDto {
        return when (element) {
            // Standard `{"list": [...], "has_more": 0/1, "total": n}` envelope.
            is JsonObject -> {
                val listElement = findListElement(element)
                UserListResponseDto(
                    list = decodeUserListOrEmpty(jsonDecoder, listElement),
                    hasMoreRaw = parseHasMore(element),
                    total = element["total"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                        ?: element["count"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                    totalNum = element["total_num"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                )
            }
            // Some rigs return the array directly, e.g. `[{..},{..}]`.
            is JsonArray -> UserListResponseDto(
                list = decodeUserListOrEmpty(jsonDecoder, element),
            )
            // Anything else (JsonNull, "", 0, etc.) is treated as "no data".
            else -> {
                AppLogger.w(
                    TAG,
                    "unexpected /home/list shape, falling back to empty: $element",
                )
                UserListResponseDto()
            }
        }
    }

    private fun parseHasMore(element: JsonObject): Int {
        val raw = element["has_more"]
            ?: element["hasMore"]
            ?: element["has_next"]
            ?: element["hasNext"]
            ?: return 0
        val content = raw.jsonPrimitive.contentOrNull?.lowercase() ?: return 0
        return when (content) {
            "1", "true", "yes" -> 1
            else -> content.toIntOrNull()?.takeIf { it > 0 }?.let { 1 } ?: 0
        }
    }

    /**
     * Some backends/encryption middlewares wrap `result` as a JSON string.
     * If we see a string that itself is JSON, parse it and continue.
     */
    private fun unwrapJsonIfString(jsonDecoder: JsonDecoder, element: JsonElement): JsonElement {
        val primitive = element as? JsonPrimitive ?: return element
        val raw = primitive.contentOrNull?.takeIf { it.isNotBlank() } ?: return element
        if (!(raw.startsWith("{") || raw.startsWith("["))) return element
        return runCatching { jsonDecoder.json.parseToJsonElement(raw) }
            .getOrElse { element }
    }

    /**
     * Tries to locate the user array inside a flexible envelope.
     * Common shapes seen across environments:
     * - `{ "list": [...] }`
     * - `{ "data": { "list": [...] } }`
     * - `{ "result": { "list": [...] } }`
     * - `{ "users": [...] }`
     */
    private fun findListElement(root: JsonObject): JsonElement? {
        root["list"]?.let { return it }
        root["users"]?.let { return it }
        root["items"]?.let { return it }

        val data = root["data"]
        if (data is JsonObject) {
            data["list"]?.let { return it }
            data["users"]?.let { return it }
            data["items"]?.let { return it }
        } else if (data is JsonArray) {
            return data
        }

        val result = root["result"]
        if (result is JsonObject) {
            result["list"]?.let { return it }
            result["users"]?.let { return it }
            result["items"]?.let { return it }
        } else if (result is JsonArray) {
            return result
        }

        return null
    }

    private fun decodeUserListOrEmpty(
        jsonDecoder: JsonDecoder,
        element: JsonElement?,
    ): List<UserDto> {
        if (element == null || element !is JsonArray) return emptyList()
        val normalized = buildJsonArray {
            element.forEach { row ->
                add(unwrapUserPayload(row))
            }
        }
        return runCatching {
            jsonDecoder.json.decodeFromJsonElement(
                ListSerializer(UserDto.serializer()),
                normalized,
            )
        }.onFailure { throwable ->
            AppLogger.w(TAG, "partial /home/list decode failed: $element", throwable)
        }.getOrDefault(emptyList())
    }

    /**
     * Follow/fans endpoints can wrap each row as
     * `{ user_info: {...}, is_follow: 1 }`; decode the nested user payload so
     * [UserDto] fields (uid/nickname/avatar) aren't lost to defaults.
     */
    private fun unwrapUserPayload(element: JsonElement): JsonElement {
        val obj = element as? JsonObject ?: return element
        val nested = obj["user_info"] ?: obj["userInfo"] ?: obj["user"]
        if (nested !is JsonObject) return element
        val merged = nested.toMutableMap()
        listOf("is_follow", "remark_name", "add_time").forEach { key ->
            obj[key]?.let { merged[key] = it }
        }
        return JsonObject(merged)
    }

    override fun serialize(encoder: Encoder, value: UserListResponseDto) {
        encoder.encodeSerializableValue(
            UserListResponseDtoSurrogate.serializer(),
            UserListResponseDtoSurrogate(
                list = value.list,
                hasMoreRaw = value.hasMoreRaw,
                total = value.total,
                totalNum = value.totalNum,
            ),
        )
    }

    private const val TAG = "UserListDto"
}

@Serializable
private data class UserListResponseDtoSurrogate(
    val list: List<UserDto> = emptyList(),
    @SerialName("has_more") val hasMoreRaw: Int = 0,
    val total: Int? = null,
    @SerialName("total_num") val totalNum: Int? = null,
)
