package com.example.demoproject.platform.network.error

import java.io.IOException

/**
 * Typed transport-layer failure used before mapping to [com.example.demoproject.platform.network.result.AppResult.NetworkError].
 */
class NetworkException(
    message: String? = null,
    cause: Throwable? = null,
) : IOException(message, cause)
