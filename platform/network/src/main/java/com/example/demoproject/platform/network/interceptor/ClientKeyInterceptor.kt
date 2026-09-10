package com.example.demoproject.platform.network.interceptor

import com.example.demoproject.platform.network.config.NetworkConfig
import okhttp3.Interceptor
import okhttp3.Response

private const val HEADER_CLIENT_KEY = "X-Client-Key"

/**
 * Optionally attaches the configured client API key header when [com.example.demoproject.platform.network.config.NetworkConfig.clientKey] is non-blank.
 */
class ClientKeyInterceptor(
    private val networkConfig: NetworkConfig,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val key = networkConfig.clientKey
        val request = if (key.isNotBlank()) {
            chain.request().newBuilder().header(HEADER_CLIENT_KEY, key).build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
