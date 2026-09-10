package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.network.result.AppResult

interface AppSessionRepository {

    /** `POST /app/init` — hydrate app-level session data on authenticated cold start. */
    suspend fun initApp(): AppResult<AppInitData>

    /** `POST /app/open` — report foreground entry and fetch foreground-only prompts. */
    suspend fun openApp(hasProxy: Boolean, hasVpn: Boolean): AppResult<AppOpenData?>

    /** `POST /app/close` — report background entry; unread sync is optional per API docs. */
    suspend fun closeApp(messageUnread: Int? = null): AppResult<Unit>

    /** `POST /app/stat` — check whether the current device is a first-time install. */
    suspend fun checkFirstInstall(): AppResult<Boolean>

    /**
     * `POST /adjust/add` — upload Adjust attribution metadata once per local app installation.
     * Not tied to `/app/stat`'s `is_first_install` (that flag reflects backend device-ID
     * recognition, not local install state).
     */
    suspend fun addAdjustAttribution(payload: AdjustAttributionPayload): AppResult<Unit>
}

data class AdjustAttributionPayload(
    val adid: String? = null,
    val gpsAdid: String? = null,
    val installReferrer: String? = null,
    val trackerToken: String? = null,
    val trackerName: String? = null,
    val network: String? = null,
    val campaign: String? = null,
    val adgroup: String? = null,
    val creative: String? = null,
    val clickLabel: String? = null,
    val costType: String? = null,
    val costAmount: Double? = null,
    val costCurrency: String? = null,
    val fbInstallReferrer: String? = null,
) {
    fun hasSignals(): Boolean = listOf(
        adid,
        gpsAdid,
        installReferrer,
        trackerToken,
        trackerName,
        network,
        campaign,
        adgroup,
        creative,
        clickLabel,
        costType,
        costCurrency,
        fbInstallReferrer,
    ).any { !it.isNullOrBlank() } || costAmount != null

    /**
     * True once Adjust itself has resolved attribution (tracker/campaign/network/cost),
     * as opposed to only having device-level signals (GAID, install referrer) that are
     * available immediately regardless of whether Adjust has responded yet.
     */
    fun hasAdjustAttributionSignals(): Boolean = listOf(
        adid,
        trackerToken,
        trackerName,
        network,
        campaign,
        adgroup,
        creative,
        clickLabel,
        costType,
        costCurrency,
    ).any { !it.isNullOrBlank() } || costAmount != null
}

data class AppInitData(
    val user: User?,
    val accountMoney: Int,
    val messageUnread: Int,
    val noticeUnread: Int,
    val refreshToken: String,
    val socket: AppSocketInfo,
    val notifyPublicKey: String,
    val shouldUploadMqttActionLog: Boolean,
    val isTurkishOfferUser: Boolean,
)

data class AppSocketInfo(
    val host: String,
    val username: String,
    val password: String,
    val clientId: String,
    val topic: String,
)

data class AppOpenData(
    val status: Int = 0,
    val accountMoney: Int = 0,
    /** Per-attempt match price from `/app/open` `match_price`. */
    val matchPrice: Int = 0,
    /** Remaining free video-call count from `/app/open` `user_info.call_free_min`. */
    val callFreeMin: Int = 0,
    /** Remaining free match count from `/app/open` `user_info.match_free_count`. */
    val matchFreeCount: Int = 0,
    val shouldUploadMqttAction: Boolean = false,
    val isOpenFullScreen: Boolean = false,
    val clubNoticeCount: Int = 0,
)
