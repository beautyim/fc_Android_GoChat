package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.network.api.AppApi
import com.example.demoproject.platform.data.network.dto.AppCloseRequestDto
import com.example.demoproject.platform.data.network.dto.AppInitResponseDto
import com.example.demoproject.platform.data.network.dto.AppOpenRequestDto
import com.example.demoproject.platform.data.network.dto.AppOpenResponseDto
import com.example.demoproject.platform.data.network.dto.AppSocketInfoDto
import com.example.demoproject.platform.data.network.dto.AppStatResponseDto
import com.example.demoproject.platform.data.network.dto.AdjustAddRequestDto
import com.example.demoproject.platform.data.network.mapper.toDomain
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.safeApiCall
import com.example.demoproject.platform.network.safeApiCallNullable
import com.example.demoproject.platform.network.safeApiCallUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSessionRepositoryImpl @Inject constructor(
    private val api: AppApi,
) : AppSessionRepository {

    override suspend fun initApp(): AppResult<AppInitData> =
        safeApiCall { api.init() }
            .map(AppInitResponseDto::toDomain)

    override suspend fun openApp(hasProxy: Boolean, hasVpn: Boolean): AppResult<AppOpenData?> =
        safeApiCallNullable {
            api.open(
                AppOpenRequestDto(
                    hasProxy = hasProxy.toIntFlag(),
                    hasVpn = hasVpn.toIntFlag(),
                ),
            )
        }.map { dto -> dto?.toDomain() }

    override suspend fun closeApp(messageUnread: Int?): AppResult<Unit> =
        safeApiCallUnit {
            api.close(AppCloseRequestDto(messageUnread = messageUnread))
        }

    override suspend fun checkFirstInstall(): AppResult<Boolean> =
        safeApiCall { api.stat() }
            .map(AppStatResponseDto::toDomain)

    override suspend fun addAdjustAttribution(payload: AdjustAttributionPayload): AppResult<Unit> =
        safeApiCallUnit {
            api.addAdjustAttribution(payload.toDto())
        }
}

private fun AppInitResponseDto.toDomain(): AppInitData =
    AppInitData(
        user = userInfo?.toDomain(),
        accountMoney = accountInfo.money,
        messageUnread = messageUnread,
        noticeUnread = noticeUnread,
        refreshToken = refreshToken,
        socket = socketInfo.toDomain(),
        notifyPublicKey = notifyPublicKey,
        shouldUploadMqttActionLog = addMqttActionLog == 1,
        isTurkishOfferUser = needNewRegionVipAlert == 1,
    )

private fun AppSocketInfoDto.toDomain(): AppSocketInfo =
    AppSocketInfo(
        host = brokerHost(),
        username = username,
        password = password,
        clientId = clientId,
        topic = topic,
    )

private fun AppSocketInfoDto.brokerHost(): String {
    if (host.isNotBlank()) return host
    if (url.isNotBlank()) return url
    if (mqttHost.isBlank()) return ""
    return when {
        mqttPortTcp > 0 -> "tcp://$mqttHost:$mqttPortTcp"
        mqttPortWss > 0 -> "wss://$mqttHost:$mqttPortWss"
        else -> mqttHost
    }
}

private fun AppOpenResponseDto.toDomain(): AppOpenData =
    AppOpenData(
        status = status,
        accountMoney = accountInfo.money,
        matchPrice = matchPrice,
        callFreeMin = userInfo.callFreeMin,
        matchFreeCount = userInfo.matchFreeCount,
        shouldUploadMqttAction = uploadMqttAction == 1,
        isOpenFullScreen = isOpenFullScreen == 1,
        clubNoticeCount = alertInfo.clubNotice.size,
    )

private fun AppStatResponseDto.toDomain(): Boolean = isFirstInstall == 1

private fun Boolean.toIntFlag(): Int = if (this) 1 else 0

private fun AdjustAttributionPayload.toDto(): AdjustAddRequestDto =
    AdjustAddRequestDto(
        adid = adid,
        gpsAdid = gpsAdid,
        installReferrer = installReferrer,
        trackerToken = trackerToken,
        trackerName = trackerName,
        network = network,
        campaign = campaign,
        adgroup = adgroup,
        creative = creative,
        clickLabel = clickLabel,
        costType = costType,
        costAmount = costAmount,
        costCurrency = costCurrency,
        fbInstallReferrer = fbInstallReferrer,
    )
