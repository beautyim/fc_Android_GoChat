package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.network.api.GooglePayApi
import com.example.demoproject.platform.data.network.dto.GooglePayCancelRequestDto
import com.example.demoproject.platform.data.network.dto.GooglePayCheckRequestDto
import com.example.demoproject.platform.data.network.dto.GooglePayCheckResponseDto
import com.example.demoproject.platform.data.network.dto.GooglePayCreateRequestDto
import com.example.demoproject.platform.data.network.dto.GooglePayCreateResponseDto
import com.example.demoproject.platform.data.network.dto.GooglePayEventAckRequestDto
import com.example.demoproject.platform.data.network.dto.GooglePayVerifyRequestDto
import com.example.demoproject.platform.network.dto.ApiResponse
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.safeApiCall
import com.example.demoproject.platform.network.safeApiCallUnit
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class BillingRepositoryImpl(
    private val api: GooglePayApi,
) : BillingRepository {

    override suspend fun checkPaymentType(request: StorePurchaseRequest): AppResult<BillingPaymentCheck> =
        safeApiCall {
            api.check(
                GooglePayCheckRequestDto(
                    goodsId = request.goodsId,
                    productType = request.productType.apiValue,
                    type = request.paymentType.apiValue,
                ),
            )
        }.let { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(result.data.toDomain())
                is AppResult.Failure -> result
            }
        }

    override suspend fun createOrder(request: StorePurchaseRequest): AppResult<GooglePayOrder> =
        safeApiCall {
            api.create(
                GooglePayCreateRequestDto(
                    goodsId = request.goodsId,
                    productType = request.productType.apiValue,
                    type = request.paymentType.apiValue,
                    fromType = request.fromType,
                    fromId = request.fromId,
                    orderFrom = request.orderFrom,
                ),
            )
        }.let { result ->
            when (result) {
                is AppResult.Success -> result.data.toDomain(request)
                is AppResult.Failure -> result
            }
        }

    override suspend fun cancelOrder(
        tranNo: String,
        appErrorCode: Int,
        googleCode: Int,
    ): AppResult<Unit> =
        safeApiCallUnit {
            api.cancel(
                GooglePayCancelRequestDto(
                    tranNo = tranNo,
                    errorCode = appErrorCode,
                    googleCode = googleCode,
                ),
            )
        }

    override suspend fun verifyOrder(
        tranNo: String,
        orderId: String,
        packageName: String,
        purchaseToken: String,
    ): AppResult<Unit> =
        callUnitAcceptingOk1OrOk2 {
            api.verify(
                GooglePayVerifyRequestDto(
                    tranNo = tranNo,
                    orderId = orderId,
                    packageName = packageName,
                    purchaseToken = purchaseToken,
                ),
            )
        }

    override suspend fun acknowledgePayEvent(eventId: Long): AppResult<Unit> =
        safeApiCallUnit {
            api.acknowledgeEvent(GooglePayEventAckRequestDto(eventId))
        }
}

private fun GooglePayCheckResponseDto.toDomain(): BillingPaymentCheck {
    if (popType != 2) return BillingPaymentCheck(title = null, methods = emptyList())
    return BillingPaymentCheck(
        title = pop?.title?.takeIf { it.isNotBlank() },
        methods = pop?.list.orEmpty().mapNotNull { method ->
            val paymentType = BillingPaymentType.entries.firstOrNull {
                it.apiValue == method.type
            } ?: return@mapNotNull null
            BillingPaymentMethod(
                title = method.title,
                iconUrl = method.icon.takeIf { it.isNotBlank() },
                type = paymentType,
            )
        },
    )
}

private fun GooglePayCreateResponseDto.toDomain(request: StorePurchaseRequest): AppResult<GooglePayOrder> {
    val resolvedProductType = BillingProductType.fromApi(productType.takeIf { it > 0 } ?: request.productType.apiValue)
    val item = payItem
    val sku = productId.takeIf { it.isNotBlank() }
        ?: item?.productId?.takeIf { it.isNotBlank() }
        ?: item?.sku?.takeIf { it.isNotBlank() }
        ?: request.productId
    val externalUrl = sequenceOf(payUrl, url, link)
        .firstOrNull { it.isNotBlank() }
        ?: callback.findFirstString(EXTERNAL_URL_KEYS)
    // External checkout carries the whole order in its redirect URL and may omit tran_no /
    // product_id entirely; only the Play path needs both to launch and later verify.
    if (externalUrl == null && (tranNo.isBlank() || sku.isBlank())) {
        return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, AppResult.requestFailedMessage())
    }
    return AppResult.Success(
        GooglePayOrder(
            tranNo = tranNo,
            productId = sku,
            productType = resolvedProductType,
            payType = BillingPayType.entries.firstOrNull { it.apiValue == payType }
                ?: BillingPayType.forProduct(resolvedProductType),
            goodsId = goodsId.takeIf { it > 0L }
                ?: item?.goodsId?.takeIf { it > 0L }
                ?: item?.id?.takeIf { it > 0L }
                ?: request.goodsId,
            price = price.takeIf { it.isNotBlank() }
                ?: item?.money?.takeIf { it > 0.0 }?.toString()
                ?: item?.moneyDesc.orEmpty(),
            currency = currency.takeIf { it.isNotBlank() }
                ?: item?.currency?.takeIf { it.isNotBlank() }
                ?: item?.currencyUnit.orEmpty(),
            externalPaymentUrl = externalUrl,
        ),
    )
}

private val EXTERNAL_URL_KEYS = setOf("redirect_url", "pay_url", "url", "link")

private suspend fun callUnitAcceptingOk1OrOk2(
    block: suspend () -> ApiResponse<*>,
): AppResult<Unit> =
    callUnitWithAcceptedOk(setOf(1, 2), block)

private suspend fun callUnitWithAcceptedOk(
    acceptedOk: Set<Int>,
    block: suspend () -> ApiResponse<*>,
): AppResult<Unit> =
    try {
        val response = block()
        val code = response.ok ?: response.status
        if (response.status == 1 || code in acceptedOk) {
            AppResult.Success(Unit)
        } else {
            AppResult.BizError(code, response.msg.ifBlank { AppResult.requestFailedMessage() })
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AppResult.NetworkError(cause = e)
    } catch (e: HttpException) {
        AppResult.BizError(e.code(), e.message())
    } catch (e: Exception) {
        AppLogger.e("BillingRepository", "payment api failed: ${e.javaClass.simpleName}: ${e.message}", e)
        AppResult.UnknownError(cause = e)
    }

private fun JsonElement?.findFirstString(keys: Set<String>): String? {
    val element = this ?: return null
    return when (element) {
        is JsonObject -> {
            keys.firstNotNullOfOrNull { key ->
                (element[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            } ?: element.values.firstNotNullOfOrNull { child -> child.findFirstString(keys) }
        }
        else -> null
    }
}
