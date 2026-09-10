package com.example.demoproject.platform.network.interceptor

import com.example.demoproject.platform.network.constants.NetworkHeaders
import com.example.demoproject.platform.network.provider.AuthTokenProvider
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches `Authorization: Bearer …` when [com.example.demoproject.platform.network.provider.AuthTokenProvider] returns a token.
 */
class AuthInterceptor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (isHandshakeRoute(original.url.encodedPath)) {
            return chain.proceed(original)
        }
        val token = tokenProvider.currentAccessToken()

        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header(NetworkHeaders.AUTHORIZATION, "${NetworkHeaders.BEARER_PREFIX}$token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }

    private fun isHandshakeRoute(path: String): Boolean =
        path == PATH_LOGIN || path == PATH_REGISTER

    private companion object {
        const val PATH_LOGIN = "/auth/login"
        const val PATH_REGISTER = "/auth/register"
    }
}
