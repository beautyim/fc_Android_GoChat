package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.message.CallConversationIds
import com.example.demoproject.platform.data.model.CallRoom
import com.example.demoproject.platform.data.model.Message
import com.example.demoproject.platform.data.network.api.CallApi
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
import com.example.demoproject.platform.network.safeApiCallUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    ): AppResult<Unit> =
        safeApiCallUnit {
            callApi.success(
                CallRoomIdRequestDto(
                    roomId = roomId,
                    fencingToken = fencingToken?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
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
            AppResult.DEFAULT_REQUEST_FAILED_MESSAGE
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
