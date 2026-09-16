package com.example.demoproject.platform.data.model

data class User(
    val id: String,
    val nickname: String,
    val avatar: String?,
    val gender: Gender,
    val age: Int,
    val bio: String,
    val isOnline: Boolean,
    val lastActiveAt: Long,
    /** Raw `YYYY-MM-DD` from `/user/my` / `/user/home` when present; used for `/user/update` birthday. */
    val birthday: String? = null,
    /** From `email` on [com.example.demoproject.platform.data.network.dto.UserDto]. */
    val email: String? = null,
    /** Raw `vip_expire_time` from `/user/my` (often Unix seconds string). */
    val vipExpireTime: String? = null,
    val countryCode: String? = null,
    val countryName: String? = null,
    /** From `is_auth` on user payload. */
    val isAuthVerified: Boolean = false,
    /** From `is_vip` on [com.example.demoproject.platform.data.network.dto.UserDto]; cache may be false until refreshed. */
    val isVip: Boolean = false,
    /** From `recharge_money`; positive means the user has paid before. */
    val rechargeMoney: Int = 0,
    /** From `is_follow` on `/user/home` and follower-list endpoints. */
    val isFollowing: Boolean = false,
    /** From `is_followed` on `/msg/detail` and list payloads when present. */
    val isFollowedBy: Boolean = false,
    /** From `is_be_black` on `/home/info` when the peer has blocked the current user. */
    val isBlockedByPeer: Boolean = false,
    /** Raw `online_status` from `/home/list`; 0 = offline, 1 = online, 2 = busy/chatting on some environments. */
    val onlineStatusCode: Int = 0,
    /** From `video_call_status` on `/home/list`; true when the video entry should be enabled. */
    val isVideoCallAvailable: Boolean = false,
    /** From `voice_call_status` on `/home/list`; kept for call surfaces that support voice-only entry. */
    val isVoiceCallAvailable: Boolean = false,
    /** From `video_call_gold` on `/home/list`; fallback when `/call/records` has no price for this user. */
    val videoCallGold: Int = 0,
    /** From `voice_call_gold` on `/home/list`; kept for voice-call surfaces. */
    val voiceCallGold: Int = 0,
    /** External user number from backend `user_id`; use this for `/home/info`. */
    val externalUserId: String = id,
    /**
     * Short video show for ringing / overlay backgrounds.
     * Sourced from `/home/list` `video_show` or call `user_info.video`.
     */
    val videoShow: VideoShow? = null,
) {
    /**
     * Presence for Online-tab cards.
     * Chatting covers `online_status == 2` and online-but-not-callable busy payloads.
     */
    fun onlinePresence(): OnlinePresence = when {
        onlineStatusCode <= 0 -> OnlinePresence.Offline
        onlineStatusCode == 2 -> OnlinePresence.Chatting
        !isVideoCallAvailable -> OnlinePresence.Chatting
        else -> OnlinePresence.Online
    }
}

/** Visual / action state on Online discover cards. */
enum class OnlinePresence {
    Online,
    Chatting,
    Offline,
    ;

    /** Offline / Chatting show message entry; Online shows video call. */
    val prefersMessageAction: Boolean
        get() = this == Chatting || this == Offline
}

enum class Gender { Male, Female, Other }
