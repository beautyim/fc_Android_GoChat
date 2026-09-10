package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.network.api.GooglePayApi
import com.example.demoproject.platform.data.network.dto.GooglePayCancelRequestDto
import com.example.demoproject.platform.data.network.dto.GooglePayCheckRequestDto
import com.example.demoproject.platform.data.network.dto.GooglePayCreateRequestDto
import com.example.demoproject.platform.data.network.dto.GooglePayCreateResponseDto
import com.example.demoproject.platform.data.network.dto.GooglePayVerifyRequestDto
import com.example.demoproject.platform.network.dto.ApiResponse
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.safeApiCall
import com.example.demoproject.platform.network.safeApiCallUnit
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

class BillingRepositoryImpl(
    private val api: GooglePayApi,
) : BillingRepository {

    override suspend fun checkPaymentType(request: StorePurchaseRequest): AppResult<Unit> =
        callUnitAcceptingOk1 {
            api.check(
                GooglePayCheckRequestDto(
                    goodsId = request.goodsId,
                    productType = request.productType.apiValue,
                    type = request.paymentType.apiValue,
                ),
            )
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
}

private fun GooglePayCreateResponseDto.toDomain(request: StorePurchaseRequest): AppResult<GooglePayOrder> {
    if (callback != null) {
        return AppResult.Success(
            GooglePayOrder(
                tranNo = "",
                productId = request.productId,
                productType = request.productType,
                payType = request.payType,
                goodsId = request.goodsId,
                price = "",
                currency = "",
                callback = callback,
            ),
        )
    }
    if (tranNo.isBlank()) {
        return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, AppResult.DEFAULT_REQUEST_FAILED_MESSAGE)
    }
    val resolvedProductType = BillingProductType.fromApi(productType.takeIf { it > 0 } ?: request.productType.apiValue)
    val item = payItem
    val sku = item?.productId?.takeIf { it.isNotBlank() }
        ?: item?.sku?.takeIf { it.isNotBlank() }
        ?: request.productId
    if (sku.isBlank()) {
        return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, AppResult.DEFAULT_REQUEST_FAILED_MESSAGE)
    }
    return AppResult.Success(
        GooglePayOrder(
            tranNo = tranNo,
            productId = sku,
            productType = resolvedProductType,
            payType = BillingPayType.forProduct(resolvedProductType),
            goodsId = item?.goodsId?.takeIf { it > 0L } ?: item?.id?.takeIf { it > 0L } ?: request.goodsId,
            price = item?.moneyDesc.orEmpty(),
            currency = item?.currency?.takeIf { it.isNotBlank() } ?: item?.currencyUnit.orEmpty(),
        ),
    )
}

private suspend fun callUnitAcceptingOk1(
    block: suspend () -> ApiResponse<*>,
): AppResult<Unit> =
    callUnitWithAcceptedOk(setOf(1), block)

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
            AppResult.BizError(code, response.msg.ifBlank { AppResult.DEFAULT_REQUEST_FAILED_MESSAGE })
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
