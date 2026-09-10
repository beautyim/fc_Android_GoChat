package com.example.demoproject.payment

import android.content.Context
import com.example.demoproject.platform.data.device.DefaultDeviceFingerprint
import com.example.demoproject.platform.data.network.api.GooglePayApi
import com.example.demoproject.platform.data.repository.BillingRepository
import com.example.demoproject.platform.data.repository.BillingRepositoryImpl
import com.example.demoproject.platform.network.config.DefaultNetworkConfig
import com.example.demoproject.platform.network.constants.NetworkHeaders
import com.example.demoproject.platform.network.crypto.ApiBodyCipher
import com.example.demoproject.platform.network.crypto.ApiKeyDeriver
import com.example.demoproject.platform.network.crypto.ApiSigner
import com.example.demoproject.platform.network.crypto.ApiUserAgentEncoder
import com.example.demoproject.platform.network.crypto.provider.UidProvider
import com.example.demoproject.platform.network.interceptor.AuthInterceptor
import com.example.demoproject.platform.network.interceptor.ClientKeyInterceptor
import com.example.demoproject.platform.network.interceptor.LocaleHeaderInterceptor
import com.example.demoproject.platform.network.interceptor.SigningEncryptionInterceptor
import com.example.demoproject.platform.network.provider.AppLocaleProvider
import com.example.demoproject.platform.network.provider.AuthTokenProvider
import com.example.demoproject.platform.network.provider.RetrofitProvider
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class BillingNetworkClient(
    context: Context,
    private val tokenProvider: AuthTokenProvider = AuthTokenProvider { null },
) {
    private val appContext = context.applicationContext
    private val networkConfig = DefaultNetworkConfig()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }

    val repository: BillingRepository by lazy {
        BillingRepositoryImpl(createGooglePayApi())
    }

    private fun createGooglePayApi(): GooglePayApi {
        val fingerprint = DefaultDeviceFingerprint(appContext)
        val cipher = ApiBodyCipher()
        val signingEncryptionInterceptor = SigningEncryptionInterceptor(
            networkConfig = networkConfig,
            keyDeriver = ApiKeyDeriver(),
            signer = ApiSigner(),
            cipher = cipher,
            uaEncoder = ApiUserAgentEncoder(cipher, fingerprint),
            uidProvider = UidProvider { UidProvider.ANONYMOUS },
            deviceFingerprint = fingerprint,
        )
        val client = OkHttpClient.Builder()
            .connectTimeout(networkConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(networkConfig.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(networkConfig.writeTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(ClientKeyInterceptor(networkConfig))
            .addInterceptor(AuthInterceptor(tokenProvider))
            .addInterceptor(
                LocaleHeaderInterceptor(
                    object : AppLocaleProvider {
                        override fun currentLanguageTag(): String =
                            Locale.getDefault().toLanguageTag()
                    },
                ),
            )
            .addInterceptor(signingEncryptionInterceptor)
            .addInterceptor(loggingInterceptor())
            .build()

        return RetrofitProvider.create(networkConfig.baseUrl, client, json)
            .create(GooglePayApi::class.java)
    }

    private fun loggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (networkConfig.verboseHttpLogging) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader(NetworkHeaders.AUTHORIZATION)
        }
}
