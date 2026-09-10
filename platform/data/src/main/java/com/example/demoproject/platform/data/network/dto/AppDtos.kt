package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AppInitResponseDto(
    @SerialName("user_info") val userInfo: UserDto? = null,
    val config: AppConfigDto = AppConfigDto(),
    @SerialName("socket_info") val socketInfo: AppSocketInfoDto = AppSocketInfoDto(),
    @SerialName("account_info") val accountInfo: AppAccountInfoDto = AppAccountInfoDto(),
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("msg_unread") val messageUnread: Int = 0,
    @SerialName("notify_public_key") val notifyPublicKey: String = "",
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("notify_subscribe_status") val notifySubscribeStatus: Int = 0,
    @SerialName("banned_infos") val bannedInfos: JsonElement? = null,
    val theme: JsonElement? = null,
    @SerialName("match_bg") val matchBackground: JsonElement? = null,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("notice_unread") val noticeUnread: Int = 0,
    @SerialName("notice_list") val noticeList: List<AppNoticeDto> = emptyList(),
    @SerialName("privacy_info") val privacyInfo: JsonElement? = null,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("add_mqtt_action_log") val addMqttActionLog: Int = 0,
    @SerialName("refresh_token") val refreshToken: String = "",
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("need_new_region_vip_alert") val needNewRegionVipAlert: Int = 0,
    @SerialName("user_identity_rule") val userIdentityRule: AppUserIdentityRuleDto? = null,
    @SerialName("strength_info") val strengthInfo: JsonElement? = null,
    @SerialName("points_info") val pointsInfo: JsonElement? = null,
)

@Serializable
data class AppConfigDto(
    @SerialName("config_version") val configVersion: JsonElement? = null,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("server_time") val serverTime: Long = 0L,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("share_delay") val shareDelay: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("del_sys_msg") val deleteSystemMessage: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("hide_services") val hideServices: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("mute_options") val muteOptions: Int = 0,
)

@Serializable
data class AppSocketInfoDto(
    val host: String = "",
    val url: String = "",
    @SerialName("mqtt_host") val mqttHost: String = "",
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("mqtt_port_tcp") val mqttPortTcp: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("mqtt_port_wss") val mqttPortWss: Int = 0,
    val username: String = "",
    val password: String = "",
    @SerialName("client_id") val clientId: String = "",
    val topic: String = "",
)

@Serializable
data class AppAccountInfoDto(
    @Serializable(with = LenientIntSerializer::class)
    val money: Int = 0,
)

@Serializable
data class AppNoticeDto(
    @Serializable(with = LenientLongSerializer::class)
    val id: Long = 0L,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("chat_type") val chatType: Int = 0,
    val content: String = "",
    @Serializable(with = LenientLongSerializer::class)
    val mtime: Long = 0L,
)

@Serializable
data class AppUserIdentityRuleDto(
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("total_group") val totalGroup: Int = 0,
    @SerialName("rule_list") val ruleList: List<AppUserIdentityRuleItemDto> = emptyList(),
)

@Serializable
data class AppUserIdentityRuleItemDto(
    val key: String = "",
    val title: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val status: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val group: Int = 0,
)

@Serializable
data class AppOpenRequestDto(
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("auto_greet") val autoGreet: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_support_club") val isSupportClub: Int = 1,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_club_notice") val isClubNotice: Int = 1,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("has_proxy") val hasProxy: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("has_vpn") val hasVpn: Int = 0,
)

@Serializable
data class AppOpenUserInfoDto(
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("call_free_min") val callFreeMin: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("match_free_count") val matchFreeCount: Int = 0,
)

@Serializable
data class AppOpenResponseDto(
    @Serializable(with = LenientIntSerializer::class)
    val status: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("match_price") val matchPrice: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("upload_mqtt_action") val uploadMqttAction: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_open_full_screen") val isOpenFullScreen: Int = 0,
    @SerialName("account_info") val accountInfo: AppAccountInfoDto = AppAccountInfoDto(),
    @SerialName("alert_info") val alertInfo: AppAlertInfoDto = AppAlertInfoDto(),
    @SerialName("user_info") val userInfo: AppOpenUserInfoDto = AppOpenUserInfoDto(),
    @SerialName("club_infos") val clubInfos: JsonElement? = null,
)

@Serializable
data class AppAlertInfoDto(
    @SerialName("strength_notice") val strengthNotice: List<JsonElement> = emptyList(),
    @SerialName("club_notice") val clubNotice: List<AppClubNoticeDto> = emptyList(),
    @SerialName("new_match_notice") val newMatchNotice: List<JsonElement> = emptyList(),
)

@Serializable
data class AppClubNoticeDto(
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("club_id") val clubId: Long = 0L,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("check_status") val checkStatus: Int = 0,
    @SerialName("refuse_reason") val refuseReason: String = "",
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("checked_time") val checkedTime: Long = 0L,
    @Serializable(with = LenientIntSerializer::class)
    val type: Int = 0,
    val reason: String = "",
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("banned_apply") val bannedApply: Int = 0,
)

@Serializable
data class AppCloseRequestDto(
    @SerialName("msg_unread") val messageUnread: Int? = null,
)

@Serializable
data class AppStatResponseDto(
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_first_install") val isFirstInstall: Int = 0,
)

@Serializable
data class AdjustAddRequestDto(
    val adid: String? = null,
    @SerialName("gps_adid") val gpsAdid: String? = null,
    @SerialName("install_referrer") val installReferrer: String? = null,
    @SerialName("tracker_token") val trackerToken: String? = null,
    @SerialName("tracker_name") val trackerName: String? = null,
    val network: String? = null,
    val campaign: String? = null,
    val adgroup: String? = null,
    val creative: String? = null,
    @SerialName("click_label") val clickLabel: String? = null,
    @SerialName("cost_type") val costType: String? = null,
    @SerialName("cost_amount") val costAmount: Double? = null,
    @SerialName("cost_currency") val costCurrency: String? = null,
    @SerialName("fb_install_referrer") val fbInstallReferrer: String? = null,
)
