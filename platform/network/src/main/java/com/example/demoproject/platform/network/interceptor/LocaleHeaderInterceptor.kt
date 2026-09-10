package com.example.demoproject.platform.network.interceptor

import com.example.demoproject.platform.network.provider.AppLocaleProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends the effective app locale to backend-owned copy endpoints.
 */
@Singleton
class LocaleHeaderInterceptor @Inject constructor(
    private val appLocaleProvider: AppLocaleProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val languageTag = appLocaleProvider.currentLanguageTag().ifBlank { DEFAULT_LANGUAGE_TAG }
        val request = chain.request()
            .newBuilder()
            .header(HEADER_ACCEPT_LANGUAGE, languageTag)
            .build()
        return chain.proceed(request)
    }

    private companion object {
        const val HEADER_ACCEPT_LANGUAGE = "Accept-Language"
        const val DEFAULT_LANGUAGE_TAG = "en"
    }
}
