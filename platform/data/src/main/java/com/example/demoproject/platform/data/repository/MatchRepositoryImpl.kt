package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.match.MatchQuotaStore
import com.example.demoproject.platform.data.network.api.MatchApi
import com.example.demoproject.platform.data.network.dto.MatchEndRequestDto
import com.example.demoproject.platform.data.network.dto.MatchHeartRequestDto
import com.example.demoproject.platform.data.network.dto.MatchNextRequestDto
import com.example.demoproject.platform.data.network.dto.MatchStartRequestDto
import com.example.demoproject.platform.data.network.dto.toVipAlertCallbackDtoOrNull
import com.example.demoproject.platform.data.network.mapper.MatchStartPayloadParser
import com.example.demoproject.platform.data.network.mapper.toRechargePageData
import com.example.demoproject.platform.data.network.toPicUrlOrNull
import com.example.demoproject.platform.network.dto.ApiResponse
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.INSUFFICIENT_BALANCE_ERROR_CODE
import com.example.demoproject.platform.network.result.MSG_SEND_INSUFFICIENT_BALANCE_ERROR_CODE
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.safeApiCall
import com.example.demoproject.platform.network.safeApiCallUnit
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchRepositoryImpl @Inject constructor(
    private val matchApi: MatchApi,
    private val matchQuotaStore: MatchQuotaStore,
) : MatchRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    override suspend fun getMatchInfo(source: String): AppResult<MatchInfo> =
        safeApiCall { matchApi.info() }
            .map { dto ->
                matchQuotaStore.update(matchFreeCount = dto.matchFreeCount)
                MatchInfo(
                    matchFreeCount = dto.matchFreeCount,
                    freeUserMatchPrice = dto.freeUserMatchPrice,
                    payUserMatchPrice = dto.payUserMatchPrice,
                    vipMatchPrice = dto.vipMatchPrice,
                    avatarList = dto.avatarList
                        .mapNotNull { it.toPicUrlOrNull() }
                        .distinct(),
                )
            }

    override suspend fun startMatch(
        matchType: Int,
        matchSex: Int,
        source: String,
    ): MatchStartResult =
        executeMatchStart(
            parsePayload = MatchStartPayloadParser::parse,
        ) {
            matchApi.start(MatchStartRequestDto(matchType = matchType, matchSex = matchSex))
        }

    override suspend fun endMatch(matchSessionId: Long, source: String): AppResult<Unit> =
        safeApiCallUnit { matchApi.end(MatchEndRequestDto(matchSessionId = matchSessionId)) }

    override suspend fun heartMatch(matchSessionId: Long, source: String): AppResult<Unit> =
        safeApiCallUnit { matchApi.heart(MatchHeartRequestDto(matchSessionId = matchSessionId)) }

    override suspend fun closeMatch(source: String): AppResult<Unit> =
        safeApiCallUnit { matchApi.close() }

    override suspend fun nextMatch(
        matchType: Int,
        matchSex: Int,
        source: String,
    ): MatchStartResult =
        executeMatchStart(
            parsePayload = MatchStartPayloadParser::parseNext,
        ) {
            matchApi.next(MatchNextRequestDto(matchType = matchType, matchSex = matchSex))
        }

    private suspend fun executeMatchStart(
        parsePayload: (JsonElement?, String?) -> MatchStartInfo,
        block: suspend () -> ApiResponse<JsonElement?>,
    ): MatchStartResult =
        try {
            mapMatchStartResponse(block(), parsePayload)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            MatchStartResult.Failure(
                message = AppResult.DEFAULT_NETWORK_MESSAGE,
                code = 0,
            )
        } catch (e: SerializationException) {
            MatchStartResult.Failure(
                message = AppResult.DEFAULT_PARSING_MESSAGE,
                code = 0,
            )
        } catch (e: Exception) {
            MatchStartResult.Failure(
                message = AppResult.DEFAULT_UNKNOWN_MESSAGE,
                code = 0,
            )
        }

    private fun mapMatchStartResponse(
        response: ApiResponse<JsonElement?>,
        parsePayload: (JsonElement?, String?) -> MatchStartInfo,
    ): MatchStartResult {
        val payload = response.payload
        if (response.isSuccess && payload != null) {
            val rawPayload = json.encodeToString(JsonElement.serializer(), payload)
            val info = parsePayload(payload, rawPayload)
            info.matchFreeCount?.let { matchQuotaStore.update(matchFreeCount = it) }
            return MatchStartResult.Success(info)
        }
        val callbackDto = response.callback.toVipAlertCallbackDtoOrNull()
        val rechargePageData = callbackDto?.toRechargePageData()
            ?: fallbackRechargePageDataOrNull(response.failureCode)
        val message = response.businessMessage.ifBlank {
            callbackDto?.funcData?.message.orEmpty()
        }.ifBlank {
            callbackDto?.funcData?.title.orEmpty()
        }.ifBlank {
            AppResult.requestFailedMessage()
        }
        return MatchStartResult.Failure(
            message = message,
            code = response.failureCode,
            rechargePageData = rechargePageData,
        )
    }

    private fun fallbackRechargePageDataOrNull(failureCode: Int): RechargePageData? {
        if (failureCode != INSUFFICIENT_BALANCE_ERROR_CODE &&
            failureCode != MSG_SEND_INSUFFICIENT_BALANCE_ERROR_CODE
        ) {
            return null
        }
        return RechargePageData(
            balance = 0,
            hotProducts = emptyList(),
            products = emptyList(),
            saleItems = emptyList(),
        )
    }
}
