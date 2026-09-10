package com.example.demoproject.platform.network.provider

/**
 * Supplies the current bearer token synchronously for [com.example.demoproject.platform.network.interceptor.AuthInterceptor].
 * Implemented by session layer in `:core:data`.
 */
fun interface AuthTokenProvider {
    fun currentAccessToken(): String?
}
