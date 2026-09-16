package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import com.example.demoproject.platform.data.network.serializer.JsonAnyAsStringSerializer
import com.example.demoproject.platform.data.network.serializer.JsonNumberOrStringAsStringSerializer
import com.example.demoproject.platform.data.network.serializer.JsonStringOrStringArrayAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Canonical user payload returned by `/user/my`, `/user/home`, and embedded
 * in `/post/notice-list` (under `user_infos`) and `/post/list` (under
 * `author`).
 *
 * Field set mirrors the YAML — anything the server may omit for a given
 * endpoint is defaulted so the parse never blows up on a partial shape.
 * Adapter methods such as [isVip] / [isOnline] hide the "int-boolean"
 * backend quirk from callers.
 *
 * Numeric fields use [LenientIntSerializer] / [LenientLongSerializer]
 * because the BerryCam backend is historically inconsistent about whether it
 * ships int-coded flags as JSON numbers, quoted strings, or `null`.
 * The legacy `module_network` proved this with custom Gson adapters —
 * this DTO carries the same tolerance into kotlinx.serialization so a
 * single poisoned field never collapses a user-list envelope
 * into a generic "data parsing error" on the UI.
 *
 * The embedded [UpdateRequest] encodes the body accepted by `/user/update`
 * (not a response shape).
 */
@Serializable
data class UserDto(
    @Serializable(with = LenientLongSerializer::class)
    val uid: Long = 0,
    /** Public display ID (`user_id`); tolerates string or number JSON shapes. */
    @Serializable(with = JsonAnyAsStringSerializer::class)
    @SerialName("user_id") val userId: String = "",
    val nickname: String = "",
    val avatar: String? = null,
    @SerialName("small_avatar") val smallAvatar: String? = null,
    @Serializable(with = LenientIntSerializer::class)
    val popular: Int = 0,
    /** `YYYY-MM-DD`; may be blank when the user hasn't finished onboarding. */
    val birthday: String? = null,
    /**
     * Precomputed age from some list/home payloads. Prefer deriving from [birthday] when present;
     * fall back to this when birthday is missing or unparseable.
     */
    @Serializable(with = LenientIntSerializer::class)
    val age: Int = 0,
    /** `1` = male, `2` = female, `3` = other / non-binary (per `/perfect/run`). */
    @Serializable(with = LenientIntSerializer::class)
    val sex: Int = 0,
    /**
     * Whether `sex` can still be changed via `/user/update-info`, per `/user/info`:
     * `1` = not modified yet (editable), `0` = already modified once (locked).
     * Defaults to `1` since most endpoints omit this field entirely.
     */
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("modify_sex") val modifySex: Int = 1,
    val email: String? = null,
    @SerialName("about_me") val aboutMe: String? = null,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("online_status") val onlineStatus: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_vip") val isVipRaw: Int = 0,
    @Serializable(with = JsonNumberOrStringAsStringSerializer::class)
    @SerialName("vip_expire_time") val vipExpireTime: String? = null,
    @Serializable(with = JsonNumberOrStringAsStringSerializer::class)
    @SerialName("vip_exp") val vipExp: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("country_name") val countryName: String? = null,
    @SerialName("country_flag") val countryFlag: String? = null,
    val sign: String? = null,
    val background: String? = null,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("fans_num") val fansNum: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("focus_num") val focusNum: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("visitor_num") val visitorNum: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("visitor_new") val visitorNew: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val height: Int = 0,
    val job: String? = null,
    val language: String? = null,
    @Serializable(with = JsonStringOrStringArrayAsStringSerializer::class)
    @SerialName("speak_language") val speakLanguage: String? = null,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("album_num") val albumNum: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("seeking_sex") val seekingSex: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("recharge_money") val rechargeMoney: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val level: Int = 0,
    @SerialName("avatar_labels") val avatarLabels: String? = null,
    @SerialName("remark_name") val remarkName: String? = null,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("add_time") val addTime: Long = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_follow") val isFollowRaw: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_followed") val isFollowedRaw: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_black") val isBlackRaw: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_be_black") val isBeBlackRaw: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_report") val isReportRaw: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("hide_online") val hideOnlineRaw: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("auth_state") val authState: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("auth_status") val authStatus: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_auth") val isAuthRaw: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("video_call_status") val videoCallStatus: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("voice_call_status") val voiceCallStatus: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("video_call_gold") val videoCallGold: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("voice_call_gold") val voiceCallGold: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("enable_video_call") val enableVideoCall: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("enable_voice_call") val enableVoiceCall: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("match_free_count") val matchFreeCount: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("call_free_min") val callFreeMin: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_deleted") val isDeletedRaw: Int = 0,
    /** Present on `/home/list` when the user has a short video show. */
    @SerialName("video_show") val videoShow: VideoShowDto? = null,
    /**
     * Peer video show on `/call/create` and incoming-call MQTT (`user_info.video`).
     * Same media shape as [videoShow]; prefer whichever field the payload carries.
     */
    @SerialName("video") val video: VideoShowDto? = null,
) {
    val isVip: Boolean get() = isVipRaw == 1
    val isOnline: Boolean get() = onlineStatus > 0
    val canModifySex: Boolean get() = modifySex != 0
    /**
     * Auth badge visibility contract:
     * - `/home/info` can return effective auth only through `auth_status`/`auth_state`.
     * - `/user/my` / `/user/home` may still rely on `is_auth`.
     * Treat the profile as verified when either source reports a verified-like status.
     */
    val isAuth: Boolean get() =
        isAuthRaw == AUTH_VERIFIED_FLAG ||
            authStatus in AUTH_VERIFIED_STATUS_VALUES ||
            authState in AUTH_VERIFIED_STATUS_VALUES
    /**
     * Callable when idle (`video_call_status == 0`) or explicitly enabled.
     * Status `1` means busy on profile/home-info payloads; some list payloads may
     * invert the flag — dialing is not blocked client-side on that field alone.
     */
    val canVideoCall: Boolean get() = enableVideoCall == 1 || videoCallStatus == 0
    val canVoiceCall: Boolean get() = enableVoiceCall == 1 || voiceCallStatus == 0
    val isDeleted: Boolean get() = isDeletedRaw == 1
    val isFollow: Boolean get() = isFollowRaw == 1
    val isFollowed: Boolean get() = isFollowedRaw == 1
    val isBlack: Boolean get() = isBlackRaw == 1
    /** True when the target user has blocked the current account (`is_be_black`). */
    val isBeBlack: Boolean get() = isBeBlackRaw == 1
    val isReport: Boolean get() = isReportRaw == 1
    val hideOnline: Boolean get() = hideOnlineRaw == 1

    companion object {
        const val AUTH_VERIFIED_FLAG = 1
        private val AUTH_VERIFIED_STATUS_VALUES = setOf(1, 4, 9)
    }

    /**
     * Body for `POST /user/update`.
     *
     * YAML contract:
     * ```
     * {
     *   "avatar":   "",
     *   "nickname": "",
     *   "birthday": "",   // birthday (YYYY-MM-DD)
     *   "gender":   1,    // 1-male, 2-female, 3-other
     *   "bio":      ""
     * }
     * ```
     *
     * Every field is nullable so the client can send a partial patch
     * (e.g. only `nickname`). A `null` field is omitted from the JSON.
     */
    @Serializable
    data class UpdateRequest(
        val avatar: String? = null,
        val nickname: String? = null,
        val birthday: String? = null,
        val gender: Int? = null,
        val bio: String? = null,
    )
}
