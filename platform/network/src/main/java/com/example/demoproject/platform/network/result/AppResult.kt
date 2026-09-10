package com.example.demoproject.platform.network.result

/**
 * Application-wide, transport-agnostic result surface.
 *
 *  - [Success]       — happy path, carries parsed domain data.
 *  - [BizError]      — server returned a non-success envelope or an HTTP error that we could
 *                      attribute to a specific code.
 *  - [NetworkError]  — I/O / timeout / unreachable.
 *  - [UnknownError]  — anything else (parse failure, unexpected exception, …).
 *
 * All three failure variants share the [Failure] parent for logging, branching, and
 * error mapping. UI must not display [Failure.message] directly: first map it through
 * the app locale layer (for example `toUiText()`) or intentionally wrap a backend-owned
 * business message as `UiText.RawBackendMessage`.
 */
sealed interface AppResult<out T> {

    data class Success<T>(val data: T) : AppResult<T>

    sealed interface Failure : AppResult<Nothing> {
        val message: String
    }

    data class BizError(
        val code: Int,
        override val message: String,
        /**
         * Optional `from_type` lifted from the envelope's `callback` object when present
         * (e.g. `msg/send`'s VIP-required failure). Callers that don't care about it can
         * ignore this; it defaults to `null` for every other failure path.
         */
        val fromType: Int? = null,
    ) : Failure

    data class NetworkError(
        override val message: String = DEFAULT_NETWORK_MESSAGE,
        val cause: Throwable? = null,
    ) : Failure

    data class UnknownError(
        override val message: String = DEFAULT_UNKNOWN_MESSAGE,
        val cause: Throwable? = null,
    ) : Failure

    companion object {
        const val DEFAULT_NETWORK_MESSAGE: String = "Network connection failed. Please check your settings."
        const val DEFAULT_REQUEST_FAILED_MESSAGE: String = "Request failed"
        const val DEFAULT_EMPTY_PAYLOAD_MESSAGE: String = "No data available"
        const val DEFAULT_PARSING_MESSAGE: String = "Parsing error"
        /**
         * Shown when TLS fails (e.g. incomplete chain, hostname mismatch) — not the same as Wi‑Fi off.
         */
        const val SSL_CERT_FAILURE_MESSAGE: String =
            "Secure connection validation failed and the server is unreachable. If your network is fine, verify the server HTTPS certificate chain."
        const val DEFAULT_UNKNOWN_MESSAGE: String = "Unknown error"

        /** Synthetic code used when the envelope is successful but `result` is null. */
        const val CODE_EMPTY_PAYLOAD: Int = -10
        /** Synthetic code for unparseable error bodies when HTTP is non-2xx. */
        const val CODE_HTTP_PREFIX_UNKNOWN: Int = -20
    }
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

suspend inline fun <T> AppResult<T>.onSuccessSuspend(action: suspend (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppResult.Failure) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(this)
    return this
}

/**
 * Returns the contained value or `null` when the call did not succeed. Useful for
 * `getOrNull()` style flows in data sources that prefer nullability over sealed
 * matching.
 */
fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data

/** Returns the failure message, or null on success. */
fun AppResult<*>.errorMessageOrNull(): String? = (this as? AppResult.Failure)?.message

fun mapHttpStatus(code: Int): String = when (code) {
    401 -> "Session expired. Please sign in again."
    403 -> "You do not have permission to access this resource."
    404 -> "Requested resource not found."
    422 -> "Request parameters are invalid."
    429 -> "Too many requests. Please try again later."
    in 500..599 -> "Server error. Please try again later."
    else -> "Request failed ($code)"
}

private val REL_LOGIN_EXPIRED_HINTS = listOf("session expired", "sign in again")

/**
 * Whether this failure indicates the bearer token is no longer accepted and the user
 * must sign in again. Matches HTTP 401-style codes and the common `status == 0`
 * envelope where the encrypted transport normalizes wire status to 200.
 */
fun AppResult.Failure.requiresRelogin(): Boolean = when (this) {
    is AppResult.BizError ->
        code == 401 ||
            code == 40101 ||
            (
                (code == 0 || code == 99) &&
                    REL_LOGIN_EXPIRED_HINTS.any { hint -> message.contains(hint, ignoreCase = true) }
                )
    else -> false
}
