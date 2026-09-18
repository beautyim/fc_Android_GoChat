package com.example.demoproject.platform.network.interceptor

import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.network.crypto.provider.DeviceFingerprint
import com.example.demoproject.platform.network.crypto.provider.UidProvider
import com.example.demoproject.platform.network.config.NetworkConfig
import com.example.demoproject.platform.network.constants.NetworkHeaders
import com.example.demoproject.platform.network.crypto.ApiBodyCipher
import com.example.demoproject.platform.network.crypto.ApiKeyDeriver
import com.example.demoproject.platform.network.crypto.ApiSigner
import com.example.demoproject.platform.network.crypto.ApiUserAgentEncoder
import com.example.demoproject.platform.network.dto.ApiResponse
import com.example.demoproject.platform.network.result.mapHttpStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlin.collections.iterator

/**
 * OkHttp interceptor that implements the BerryCam sign-and-encrypt transport:
 *
 *  - outgoing JSON bodies are signed (double-MD5) then AES-encrypted
 *  - POST requests that carry no body get a synthesized device-risk payload,
 *    signed and encrypted the same way
 *  - an encrypted, `|`-separated User-Agent is attached as the `ua` header
 *  - the response body is AES-decrypted, stripped of its surrounding quotes,
 *    and rewrapped as application/json so the Retrofit converter sees plaintext
 *  - payloads larger than [MAX_DECRYPTABLE_BYTES] pass through unchanged (file
 *    downloads etc.)
 *  - when decryption succeeds the HTTP status is normalised to 200 so business
 *    logic can rely purely on the envelope's `status` field
 *  - when the decrypted body is a **root JSON array** (`[]` or `[{...}]`) the
 *    server may omit the [com.example.demoproject.platform.network.dto.ApiResponse] wrapper — we fold it into
 *    `{status,msg,result}` using the **wire** HTTP code (see
 *    [normalizeDecryptedPlaintext]) so Retrofit never decodes a bare `[`
 *  - when the wire / decrypted body is **empty** on HTTP 2xx (known gateway bug
 *    on `msg/set`), we synthesize `{"ok":1}` so Retrofit can decode an envelope
 *
 * The interceptor still runs empty-body normalisation when
 * [com.example.demoproject.platform.network.config.NetworkConfig.requestSigningEnabled] is false;
 * sign/encrypt steps are skipped in that mode.
 */
class SigningEncryptionInterceptor @Inject constructor(
    private val networkConfig: NetworkConfig,
    private val keyDeriver: ApiKeyDeriver,
    private val signer: ApiSigner,
    private val cipher: ApiBodyCipher,
    private val uaEncoder: ApiUserAgentEncoder,
    private val uidProvider: UidProvider,
    private val deviceFingerprint: DeviceFingerprint,
) : Interceptor {

    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val incoming = chain.request()
        if (!networkConfig.requestSigningEnabled) {
            return foldEmptySuccessBody(chain.proceed(incoming), incoming.url.toUrl().path)
        }
        val path = incoming.url.toUrl().path
        val uid = uidProvider.currentUid()
        val aesKey = keyDeriver.derive(
            channelName = networkConfig.channelName,
            uid = uid,
            apiEncryptionKey = networkConfig.apiEncryptionKey,
        )
        val outgoing = decorateRequest(incoming, path, aesKey)
        val raw = chain.proceed(outgoing)
        return decryptResponse(raw, path, uid, aesKey)
    }

    // ---------------------------------------------------------------------
    // Request pipeline
    // ---------------------------------------------------------------------

    private fun decorateRequest(incoming: Request, path: String, aesKey: String): Request {
        if (networkConfig.verboseHttpLogging) {
            val plaintextUa = uaEncoder.buildPlaintext(
                channelName = networkConfig.channelName,
                webVersion = networkConfig.webVersion,
            )
            AppLogger.d(TAG, "userAgent-->$plaintextUa")
        }
        val builder = incoming.newBuilder().header(
            NetworkHeaders.USER_AGENT,
            uaEncoder.encode(
                channelName = networkConfig.channelName,
                webVersion = networkConfig.webVersion,
                aesKey = aesKey,
                prefix = networkConfig.apiUserAgentPrefix,
            ),
        )
        var encryptedBody: String? = null
        if (methodPermitsBody(incoming.method)) {
            encryptedBody = encipherOutgoingBody(incoming, path, aesKey)
            builder.method(incoming.method, encryptedBody.toRequestBody(JSON_MEDIA))
        }
        return builder.build().also {
            logOutgoingRequest(path, encryptedBody)
        }
    }

    private fun encipherOutgoingBody(request: Request, path: String, aesKey: String): String {
        val original = request.body
        val signed = when {
            original != null && isJsonBody(original) -> signJsonBody(original, path)
            request.method.equals(METHOD_POST, ignoreCase = true) -> signRiskSignals(path)
            else -> null
        }
        if (signed.isNullOrEmpty()) return ""
        return cipher.encrypt(aesKey, signed)
    }

    private fun signJsonBody(body: RequestBody, path: String): String {
        val charset = body.contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
        val plaintext = Buffer().also { body.writeTo(it) }.readString(charset)
        val parsed = runCatching { json.parseToJsonElement(plaintext) as? JsonObject }.getOrNull()
        val signed = signer.sign(parsed ?: JsonObject(emptyMap()), networkConfig.signKey)
        val output = json.encodeToString(JsonObject.serializer(), signed)
        logChunkedPlaintext("Signed JSON->$path", output)
        return output
    }

    private fun signRiskSignals(path: String): String {
        // Preserve insertion order so the canonical string is stable across
        // runs — the signer only sorts the top level, which matches the JS
        // reference implementation behaviour.
        val payload = buildJsonObject {
            for ((key, value) in deviceFingerprint.riskSignals()) {
                put(key, value.toJsonPrimitive())
            }
        }
        val signed = signer.sign(payload, networkConfig.signKey)
        val output = json.encodeToString(JsonObject.serializer(), signed)
        // Legacy was silent on the risk branch; we log under the same
        // "Signed JSON->$path" prefix because the payload is structurally
        // identical to a signed JSON body — just synthesized, not user-authored.
        logChunkedPlaintext("Signed JSON->$path", output)
        return output
    }

    // ---------------------------------------------------------------------
    // Response pipeline
    // ---------------------------------------------------------------------

    private fun decryptResponse(
        response: Response,
        path: String,
        uid: Long,
        aesKey: String,
    ): Response {
        val body = response.body ?: return response
        val declared = body.contentLength()
        if (declared > MAX_DECRYPTABLE_BYTES) {
            AppLogger.d(TAG, "Response too large to decrypt: $declared")
            return response
        }
        val contentType = body.contentType()
        val rawBytes = try {
            readAllAndClose(body)
        } catch (io: IOException) {
            AppLogger.e(TAG, "failed to read response body: $path", io)
            throw io
        }
        val cleaned = unwrapQuotes(String(rawBytes, StandardCharsets.UTF_8))
        if (cleaned.isEmpty()) {
            return foldEmptySuccessBody(
                response.newBuilder().body(rawBytes.toResponseBody(contentType)).build(),
                path,
            )
        }
        return try {
            val plaintext = cipher.decrypt(aesKey, cleaned)
            if (plaintext.isBlank()) {
                onDecryptEmpty(response, path, uid, aesKey, cleaned, rawBytes, contentType)
            } else {
                val normalized = normalizeDecryptedPlaintext(plaintext, response.code, path)
                logChunkedPlaintext("Response Body->$path", normalized)
                response.newBuilder()
                    .code(HTTP_OK)
                    .body(normalized.toResponseBody(JSON_MEDIA))
                    .build()
            }
        } catch (t: Throwable) {
            onDecryptFailure(response, path, uid, aesKey, cleaned, rawBytes, contentType, t)
        }
    }

    private fun onDecryptEmpty(
        response: Response,
        path: String,
        uid: Long,
        aesKey: String,
        cipherText: String,
        rawBytes: ByteArray,
        contentType: MediaType?,
    ): Response {
        if (response.isSuccessful) {
            AppLogger.e(
                TAG,
                "Decrypt failed -> path=$path uid=$uid keyPrefix=${aesKey.take(4)}" +
                    " cipherLen=${cipherText.length} cipherHead=${cipherText.take(CIPHER_PREVIEW)}",
            )
            throw IOException("Decrypt failed for $path")
        }
        AppLogger.w(
            TAG,
            "non-2xx (${response.code}) empty plaintext, pass-through: path=$path" +
                " uid=$uid keyPrefix=${aesKey.take(4)}",
        )
        return response.newBuilder().body(rawBytes.toResponseBody(contentType)).build()
    }

    private fun onDecryptFailure(
        response: Response,
        path: String,
        uid: Long,
        aesKey: String,
        cipherText: String,
        rawBytes: ByteArray,
        contentType: MediaType?,
        cause: Throwable,
    ): Response {
        if (!response.isSuccessful) {
            AppLogger.w(
                TAG,
                "non-2xx (${response.code}) decrypt error, pass-through: path=$path" +
                    " uid=$uid keyPrefix=${aesKey.take(4)}",
                cause,
            )
            return response.newBuilder().body(rawBytes.toResponseBody(contentType)).build()
        }
        AppLogger.e(
            TAG,
            "Decrypt exception -> path=$path uid=$uid keyPrefix=${aesKey.take(4)}" +
                " cipherLen=${cipherText.length} cipherHead=${cipherText.take(CIPHER_PREVIEW)}",
            cause,
        )
        throw if (cause is IOException) cause else IOException("Decrypt failed for $path", cause)
    }

    /**
     * Retrofit decodes every body as [com.example.demoproject.platform.network.dto.ApiResponse] (a JSON object). The backend
     * occasionally returns HTTP 500 with a ciphertext whose plaintext is a bare
     * array — e.g. `[]` — which decrypts successfully, but `[]` is not an
     * object, so kotlinx.serialization throws before [safeApiCall] runs.
     *
     * We wrap root-level JSON arrays into a canonical envelope. The **original**
     * HTTP code decides success vs failure: 2xx → `status = 1` with the array
     * as `result`; otherwise `status = 0` with [com.example.demoproject.platform.network.result.mapHttpStatus].
     */
    private fun normalizeDecryptedPlaintext(
        plaintext: String,
        httpCode: Int,
        path: String,
    ): String {
        val trim = plaintext.trim()
        if (trim.startsWith('{')) {
            // Some failures (e.g. private-album/unlock ok=0) nest recharge/VIP
            // `callback` under `data`. ApiResponse / safeApiCall only read the
            // envelope-root field — promote when root omits it.
            return hoistDataCallbackToEnvelope(trim, path)
        }
        if (!trim.startsWith('[')) return plaintext

        val parsed = runCatching { json.parseToJsonElement(trim) }.getOrNull()
            ?: return plaintext

        return if (httpCode in HTTP_OK_MIN..HTTP_OK_MAX) {
            AppLogger.w(
                TAG,
                "root JSON array → ApiResponse envelope (wire http ok): path=$path len=${trim.length}",
            )
            val envelope = buildJsonObject {
                put("status", JsonPrimitive(ApiResponse.Companion.SUCCESS_STATUS))
                put("msg", JsonPrimitive(""))
                put("result", parsed)
            }
            json.encodeToString(envelope)
        } else {
            AppLogger.w(
                TAG,
                "root JSON array → ApiResponse failure envelope: path=$path wireHttp=$httpCode " +
                    "preview=${trim.take(JSON_PREVIEW_LEN)}",
            )
            val envelope = buildJsonObject {
                put("status", JsonPrimitive(0))
                put("msg", JsonPrimitive(mapHttpStatus(httpCode)))
                put("result", JsonNull)
            }
            json.encodeToString(envelope)
        }
    }

    /**
     * Promotes `data.callback` to the envelope root when the root has no
     * `callback`. Safe for success payloads that lack a nested callback (no-op).
     */
    private fun hoistDataCallbackToEnvelope(plaintext: String, path: String): String {
        val root = runCatching { json.parseToJsonElement(plaintext) as? JsonObject }.getOrNull()
            ?: return plaintext
        if (root.containsKey("callback")) return plaintext
        val data = root["data"] as? JsonObject ?: return plaintext
        val nestedCallback = data["callback"] ?: return plaintext
        AppLogger.d(TAG, "hoist data.callback → envelope root: path=$path")
        val hoisted = JsonObject(root.toMutableMap().apply { put("callback", nestedCallback) })
        return json.encodeToString(JsonObject.serializer(), hoisted)
    }

    // ---------------------------------------------------------------------
    // Low-level helpers
    // ---------------------------------------------------------------------

    private fun readAllAndClose(body: ResponseBody): ByteArray {
        body.use { live ->
            val source = live.source()
            source.request(Long.MAX_VALUE)
            return source.buffer.clone().readByteArray()
        }
    }

    private fun logChunkedPlaintext(label: String, text: String) {
        if (!networkConfig.verboseHttpLogging) return
        if (text.length <= LOG_CHUNK_SIZE) {
            AppLogger.d(TAG, "$label: $text")
            return
        }
        // Long payload: emit a header line first, matching the legacy
        // "Response Body->$path (len=5432, parts=3)" preamble, then the chunks
        // so the reader can recombine parts after logcat truncation.
        val total = (text.length + LOG_CHUNK_SIZE - 1) / LOG_CHUNK_SIZE
        AppLogger.d(TAG, "$label (len=${text.length}, parts=$total)")
        var cursor = 0
        var part = 1
        while (cursor < text.length) {
            val end = minOf(cursor + LOG_CHUNK_SIZE, text.length)
            AppLogger.d(TAG, "$label [Part $part/$total]: ${text.substring(cursor, end)}")
            cursor = end
            part++
        }
    }

    private fun logOutgoingRequest(path: String, encryptedBody: String?) {
        if (!networkConfig.verboseHttpLogging) return
        // Method, URL, headers, content-length and the raw wire body are all
        // already covered by HttpLoggingInterceptor. We only add a labeled,
        // chunk-aware dump of the encrypted body so it survives logcat
        // truncation and is easy to grep by route.
        if (!encryptedBody.isNullOrEmpty()) {
            logChunkedPlaintext("Encrypted Body->$path", encryptedBody)
        }
    }

    /**
     * Gateway `msg/set` (and potentially similar bugs) returns HTTP 2xx with an
     * empty body on success. Retrofit always expects an [ApiResponse] JSON object,
     * so we synthesize a minimal success envelope.
     */
    private fun foldEmptySuccessBody(response: Response, path: String): Response {
        val body = response.body ?: return response
        val declared = body.contentLength()
        // contentLength() == 0 is definitive; -1 (unknown) still needs a peek.
        if (declared > 0L) return response
        val contentType = body.contentType()
        val rawBytes = try {
            readAllAndClose(body)
        } catch (io: IOException) {
            AppLogger.e(TAG, "failed to read empty-check body: $path", io)
            throw io
        }
        if (rawBytes.isNotEmpty() && unwrapQuotes(String(rawBytes, StandardCharsets.UTF_8)).isNotEmpty()) {
            return response.newBuilder().body(rawBytes.toResponseBody(contentType)).build()
        }
        if (!response.isSuccessful) {
            return response.newBuilder().body(rawBytes.toResponseBody(contentType)).build()
        }
        AppLogger.w(TAG, "empty HTTP ${response.code} body → ok=1 envelope: path=$path")
        return response.newBuilder()
            .code(HTTP_OK)
            .body(EMPTY_SUCCESS_ENVELOPE.toResponseBody(JSON_MEDIA))
            .build()
    }

    private fun unwrapQuotes(input: String): String {
        val length = input.length
        return if (length >= 2 && input[0] == '"' && input[length - 1] == '"') {
            input.substring(1, length - 1)
        } else {
            input
        }
    }

    private fun isJsonBody(body: RequestBody): Boolean =
        body.contentType()?.subtype?.equals("json", ignoreCase = true) == true

    private fun methodPermitsBody(method: String): Boolean = when (method.uppercase()) {
        "GET", "HEAD" -> false
        else -> true
    }

    private fun Any.toJsonPrimitive(): JsonPrimitive = when (this) {
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        else -> JsonPrimitive(toString())
    }

    private companion object {
        const val TAG = "SignCrypto"
        const val METHOD_POST = "POST"
        const val HTTP_OK = 200
        const val MAX_DECRYPTABLE_BYTES = 1024L * 1024L
        const val LOG_CHUNK_SIZE = 2000
        // Number of Base64 characters from the opaque ciphertext to include in
        // failure diagnostics. Short enough not to leak a meaningful amount of
        // a legitimate payload, long enough to match against the server log.
        const val CIPHER_PREVIEW = 32
        const val HTTP_OK_MIN = 200
        const val HTTP_OK_MAX = 299
        const val JSON_PREVIEW_LEN = 64
        /** Synthesized envelope for empty HTTP 2xx bodies (e.g. `msg/set` success). */
        const val EMPTY_SUCCESS_ENVELOPE = """{"ok":1}"""
        private val JSON_MEDIA: MediaType = "application/json; charset=utf-8".toMediaType()
    }
}
