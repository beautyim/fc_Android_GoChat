package com.example.demoproject.platform.network

import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.network.dto.ApiResponse
import com.example.demoproject.platform.network.parser.ApiEnvelopeParser
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.mapHttpStatus
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

private const val SAFE_API_TAG = "SafeApiCall"

private val envelopeJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

/**
 * Best-effort `from_type` extraction from an envelope's `callback` object (e.g.
 * `msg/send`'s `{"ok":3,"callback":{"from_type":1,...}}`, or
 * `recharge_alert` payloads that nest it under `func_data`). Returns `null` when
 * the callback is missing, isn't an object, or has no numeric `from_type`.
 */
private fun JsonElement?.callbackFromType(): Int? {
    val obj = this as? JsonObject ?: return null
    (obj["from_type"] as? JsonPrimitive)?.intOrNull?.let { return it }
    val funcData = obj["func_data"] as? JsonObject ?: return null
    return (funcData["from_type"] as? JsonPrimitive)?.intOrNull
}

/**
 * Executes a Retrofit suspend call returning [ApiResponse] and maps every failure mode
 * into [AppResult]:
 *  - envelope `status != 1`                 → [AppResult.BizError]
 *  - envelope `status == 1` but `result==null` → [AppResult.BizError] with synthetic code
 *  - HTTP non-2xx                            → [AppResult.BizError] (best-effort body parse)
 *  - I/O                                     → [AppResult.NetworkError]
 *  - Serialization / other                   → [AppResult.UnknownError]
 */
suspend fun <T> safeApiCall(block: suspend () -> ApiResponse<T>): AppResult<T> {
    return try {
        val response = block()
        val payload = response.payload
        when {
            !response.isSuccess ->
                AppResult.BizError(
                    response.failureCode,
                    response.failureMessage(),
                    fromType = response.callback.callbackFromType(),
                    callback = response.callback,
                )

            payload == null ->
                AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, AppResult.DEFAULT_EMPTY_PAYLOAD_MESSAGE)

            else -> AppResult.Success(payload)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        mapHttpException(e)
    } catch (e: IOException) {
        AppResult.NetworkError(message = e.networkFailureMessage(), cause = e)
    } catch (e: SerializationException) {
        // Capture the root-cause class + message so we can pinpoint which
        // field blew up instead of just seeing a generic "Parsing error" in the
        // UI. The stack trace is kept for full context.
        AppLogger.e(SAFE_API_TAG, "serialize failed: ${e.javaClass.simpleName}: ${e.message}", e)
        AppResult.UnknownError(AppResult.DEFAULT_PARSING_MESSAGE, e)
    } catch (e: Exception) {
        AppLogger.e(SAFE_API_TAG, "unknown failure: ${e.javaClass.simpleName}: ${e.message}", e)
        AppResult.UnknownError(cause = e)
    }
}

/**
 * For endpoints where the result payload is optional — the server may return `status = 1`
 * without a `result` field (e.g. `/msg/send`).  Unlike [safeApiCall], a null result is
 * treated as [AppResult.Success] with `data = null` instead of a [AppResult.BizError].
 */
suspend fun <T> safeApiCallNullable(block: suspend () -> ApiResponse<T?>): AppResult<T?> {
    return try {
        val response = block()
        val payload = response.payload
        if (!response.isSuccess) {
            AppResult.BizError(
                response.failureCode,
                response.failureMessage(),
                fromType = response.callback.callbackFromType(),
                callback = response.callback,
            )
        } else {
            AppResult.Success(payload)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        mapHttpException(e)
    } catch (e: IOException) {
        AppResult.NetworkError(message = e.networkFailureMessage(), cause = e)
    } catch (e: SerializationException) {
        AppLogger.e(SAFE_API_TAG, "serialize failed: ${e.javaClass.simpleName}: ${e.message}", e)
        AppResult.UnknownError(AppResult.DEFAULT_PARSING_MESSAGE, e)
    } catch (e: Exception) {
        AppLogger.e(SAFE_API_TAG, "unknown failure: ${e.javaClass.simpleName}: ${e.message}", e)
        AppResult.UnknownError(cause = e)
    }
}

/**
 * For endpoints whose success semantics are defined purely by `status == 1` (body optional).
 */
suspend fun safeApiCallUnit(block: suspend () -> ApiResponse<Unit?>): AppResult<Unit> {
    return try {
        val response = block()
        if (response.isSuccess) {
            AppResult.Success(Unit)
        } else {
            AppResult.BizError(response.failureCode, response.failureMessage())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        mapHttpException(e)
    } catch (e: IOException) {
        AppResult.NetworkError(message = e.networkFailureMessage(), cause = e)
    } catch (e: SerializationException) {
        AppLogger.e(SAFE_API_TAG, "serialize failed: ${e.javaClass.simpleName}: ${e.message}", e)
        AppResult.UnknownError(AppResult.DEFAULT_PARSING_MESSAGE, e)
    } catch (e: Exception) {
        AppLogger.e(SAFE_API_TAG, "unknown failure: ${e.javaClass.simpleName}: ${e.message}", e)
        AppResult.UnknownError(cause = e)
    }
}

private fun ApiResponse<*>.failureMessage(): String =
    businessMessage.ifBlank {
        callback.callbackTitleOrNull().orEmpty()
    }.ifBlank {
        when (failureCode) {
            99 -> mapHttpStatus(401)
            else -> AppResult.requestFailedMessage()
        }
    }

/** `recharge_alert` / alert payloads often put the user-facing copy under `func_data.title`. */
private fun JsonElement?.callbackTitleOrNull(): String? {
    val obj = this as? JsonObject ?: return null
    val funcData = obj["func_data"] as? JsonObject ?: return null
    val title = (funcData["title"] as? JsonPrimitive)?.content?.trim().orEmpty()
    if (title.isNotEmpty()) return title
    val message = (funcData["msg"] as? JsonPrimitive)?.content?.trim().orEmpty()
        .ifEmpty { (funcData["message"] as? JsonPrimitive)?.content?.trim().orEmpty() }
    return message.takeIf { it.isNotEmpty() }
}

internal fun parseErrorBody(body: ResponseBody?): String? =
    try {
        body?.use { it.string().takeIf { s -> s.isNotBlank() } }
    } catch (_: Exception) {
        null
    }

private fun IOException.networkFailureMessage(): String {
    var t: Throwable? = this
    while (t != null) {
        when (t) {
            is SSLHandshakeException,
            is SSLPeerUnverifiedException,
            is CertPathValidatorException,
            -> return AppResult.SSL_CERT_FAILURE_MESSAGE
        }
        t = t.cause
    }
    return AppResult.DEFAULT_NETWORK_MESSAGE
}

private fun <T> mapHttpException(e: HttpException): AppResult<T> {
    val raw = parseErrorBody(e.response()?.errorBody())
    val parsed = raw?.let { ApiEnvelopeParser.parseCodeMessage(envelopeJson, it) }

    return when {
        parsed != null ->
            AppResult.BizError(parsed.first, parsed.second.ifBlank { mapHttpStatus(e.code()) })

        else ->
            AppResult.BizError(e.code(), mapHttpStatus(e.code()))
    }
}
