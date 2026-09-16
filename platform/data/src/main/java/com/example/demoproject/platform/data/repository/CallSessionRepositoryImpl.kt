package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.message.CallConversationIds
import com.example.demoproject.platform.data.model.CallRoom
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.network.api.CallApi
import com.example.demoproject.platform.data.network.dto.CallBlurRequestDto
import com.example.demoproject.platform.data.network.dto.CallCreateRequestDto
import com.example.demoproject.platform.data.network.dto.CallEndRequestDto
import com.example.demoproject.platform.data.network.dto.CallMessageRequestDto
import com.example.demoproject.platform.data.network.dto.CallRoomIdRequestDto
import com.example.demoproject.platform.data.network.dto.VipAlertCallbackDto
import com.example.demoproject.platform.data.network.dto.toVipAlertCallbackDtoOrNull
import com.example.demoproject.platform.data.network.mapper.toDomain
import com.example.demoproject.platform.data.network.mapper.toRechargePageData
import com.example.demoproject.platform.data.local.cache.ChatStore
import com.example.demoproject.platform.data.session.SessionManager
import com.example.demoproject.platform.network.dto.ApiResponse
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.INSUFFICIENT_BALANCE_ERROR_CODE
import com.example.demoproject.platform.network.result.MSG_SEND_INSUFFICIENT_BALANCE_ERROR_CODE
import com.example.demoproject.platform.network.safeApiCallNullable
import com.example.demoproject.platform.network.safeApiCallUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import java.io.IOException

@Singleton
class CallSessionRepositoryImpl @Inject constructor(
    private val callApi: CallApi,
    private val chatStore: ChatStore,
    private val sessionManager: SessionManager,
) : CallSessionRepository {

    override suspend fun createCall(
        targetUid: Long,
        callType: Int,
        fromType: Int,
    ): CallCreateResult =
        try {
            val response = callApi.create(
                CallCreateRequestDto(
                    targetUid = targetUid,
                    callType = callType,
                    fromType = fromType,
                ),
            )
            mapCreateCallResponse(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            CallCreateResult.Failure(
                message = AppResult.DEFAULT_NETWORK_MESSAGE,
                code = 0,
            )
        } catch (e: SerializationException) {
            CallCreateResult.Failure(
                message = AppResult.DEFAULT_PARSING_MESSAGE,
                code = 0,
            )
        } catch (e: Exception) {
            CallCreateResult.Failure(
                message = AppResult.DEFAULT_UNKNOWN_MESSAGE,
                code = 0,
            )
        }

    override suspend fun reportCallSuccess(
        roomId: String,
        fencingToken: String?,
    ): AppResult<CallRoom?> {
        val result = safeApiCallNullable {
            callApi.success(
                CallRoomIdRequestDto(
                    roomId = roomId,
                    fencingToken = fencingToken?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
        }
        return when (result) {
            is AppResult.Success -> {
                val room = result.data?.takeIf { it.hasValidRoom() }?.toCallRoomDto()?.toDomain()
                AppResult.Success(room)
            }
            is AppResult.BizError -> result
            is AppResult.Failure -> result
        }
    }

    override suspend fun acceptCall(roomId: String): AppResult<Unit> =
        safeApiCallUnit {
            callApi.accept(CallRoomIdRequestDto(roomId = roomId))
        }

    override suspend fun fetchAnswerStatus(roomId: String): AppResult<CallAnswerStatus> {
        val result = safeApiCallNullable {
            callApi.answerStatus(CallRoomIdRequestDto(roomId = roomId))
        }
        return when (result) {
            is AppResult.Success -> {
                val data = result.data
                AppResult.Success(
                    CallAnswerStatus(
                        status = data?.status?.trim().orEmpty().ifBlank { "waiting" },
                        answerTimeoutSec = data?.answerTimeoutSec ?: 0,
                    ),
                )
            }
            is AppResult.BizError -> result
            is AppResult.Failure -> result
        }
    }

    override suspend fun heartbeat(
        roomId: String,
        fencingToken: String?,
    ): AppResult<Boolean> =
        when (
            val result = safeApiCallUnit {
                callApi.heart(
                    CallRoomIdRequestDto(
                        roomId = roomId,
                        fencingToken = fencingToken?.trim()?.takeIf { it.isNotEmpty() },
                    ),
                )
            }
        ) {
            is AppResult.Success -> AppResult.Success(true)
            is AppResult.BizError -> {
                if (result.code == 0) AppResult.Success(false) else result
            }
            is AppResult.Failure -> result
        }

    override suspend fun renewRtcToken(
        roomId: String,
        fencingToken: String?,
    ): CallRenewTokenResult {
        return try {
            val response = callApi.renewToken(
                CallRoomIdRequestDto(
                    roomId = roomId,
                    fencingToken = fencingToken?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
            when {
                response.isSuccess -> {
                    val token = response.payload?.effectiveToken.orEmpty()
                    if (token.isBlank()) {
                        CallRenewTokenResult.Retryable(AppResult.requestFailedMessage())
                    } else {
                        CallRenewTokenResult.Renewed(
                            token = token,
                            expireAt = response.payload?.expireAt ?: 0L,
                            renewAheadSec = response.payload?.renewAheadSec ?: 0,
                            serverNow = response.payload?.serverNow ?: 0L,
                        )
                    }
                }
                response.failureCode == 2 -> CallRenewTokenResult.StopRenewal
                else -> CallRenewTokenResult.Retryable(
                    response.businessMessage.ifBlank { AppResult.requestFailedMessage() },
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            CallRenewTokenResult.Retryable(AppResult.DEFAULT_NETWORK_MESSAGE)
        } catch (_: SerializationException) {
            CallRenewTokenResult.Retryable(AppResult.DEFAULT_PARSING_MESSAGE)
        } catch (_: Exception) {
            CallRenewTokenResult.Retryable(AppResult.DEFAULT_UNKNOWN_MESSAGE)
        }
    }

    override suspend fun endCall(roomId: String, durationSeconds: Int): AppResult<Unit> =
        safeApiCallUnit {
            callApi.end(
                CallEndRequestDto(
                    roomId = roomId,
                    duration = durationSeconds.coerceAtLeast(0),
                    endType = CallEndRequestDto.END_TYPE_USER_HANGUP,
                ),
            )
        }

    override suspend fun updateMaskStatus(roomId: String, cameraOn: Boolean): AppResult<Unit> =
        safeApiCallUnit {
            callApi.maskStatus(
                CallBlurRequestDto(
                    roomId = roomId,
                    status = if (cameraOn) 1 else 0,
                ),
            )
        }

    override suspend fun sendCallMessage(
        roomId: String,
        toUid: Long,
        content: String,
        msgType: Int,
    ): AppResult<Unit> =
        safeApiCallUnit {
            callApi.sendMsg(
                CallMessageRequestDto(
                    roomId = roomId,
                    toUid = toUid,
                    content = content,
                    msgType = msgType,
                ),
            )
        }

    override fun observeCallRoomMessages(roomId: Long): Flow<List<Message>> {
        val owner = sessionManager.currentUserId.orEmpty()
        val conversationId = CallConversationIds.forRoom(roomId).orEmpty()
        return chatStore.observeMessages(conversationId)
    }

    override suspend fun upsertCallRoomMessage(message: Message): AppResult<Unit> {
        val owner = sessionManager.currentUserId
            ?: return AppResult.BizError(401, "Not logged in")
        chatStore.upsertMessages(message.conversationId, listOf(message))
        return AppResult.Success(Unit)
    }

    private fun mapCreateCallResponse(
        response: ApiResponse<com.example.demoproject.platform.data.network.dto.CallCreateDataDto?>,
    ): CallCreateResult {
        val payload = response.payload
        if (response.isSuccess && payload != null && payload.hasValidRoom()) {
            return CallCreateResult.Success(payload.toCallRoomDto().toDomain())
        }
        // The backend may nest `callback` inside `data`, or place it on the envelope
        // root (seen for `ok=6` insufficient-balance responses); check both.
        val callbackDto = payload?.callback ?: response.callback.toVipAlertCallbackDtoOrNull()
        val rechargePageData = callbackDto?.toRechargePageData()
            ?: fallbackRechargePageDataOrNull(response.failureCode)
        val alert = resolveCallCreateAlert(
            failureCode = response.failureCode,
            callbackAlert = callbackDto?.toCallCreateAlertOrNull(),
        )
        val message = response.businessMessage.ifBlank {
            alert?.message.orEmpty()
        }.ifBlank {
            callbackDto?.funcData?.message.orEmpty()
        }.ifBlank {
            callbackDto?.funcData?.title.orEmpty()
        }.ifBlank {
            AppResult.requestFailedMessage()
        }
        return CallCreateResult.Failure(
            message = message,
            code = response.failureCode,
            rechargePageData = rechargePageData,
            alert = alert,
        )
    }

    private fun VipAlertCallbackDto.toCallCreateAlertOrNull(): CallCreateAlert? {
        if (!funcName.equals("alert", ignoreCase = true)) return null
        val funcData = funcData ?: return null
        val message = funcData.message.ifBlank { funcData.title }.ifBlank { return null }
        return CallCreateAlert(
            message = message,
            title = funcData.title,
            confirmLabel = funcData.enterTitle,
        )
    }

    /**
     * If the failure code signals insufficient balance but the `callback` payload
     * couldn't be parsed into usable products, still surface an (initially empty)
     * recharge sheet so the UI can fetch the catalog via `/coin/index`, mirroring
     * how chat message sending falls back when its own callback is unusable.
     */
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
