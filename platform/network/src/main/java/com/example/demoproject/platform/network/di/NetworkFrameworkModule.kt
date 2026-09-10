package com.example.demoproject.platform.network.di

import com.example.demoproject.platform.network.config.DefaultNetworkConfig
import com.example.demoproject.platform.network.config.NetworkConfig
import com.example.demoproject.platform.network.constants.NetworkHeaders
import com.example.demoproject.platform.network.crypto.ApiBodyCipher
import com.example.demoproject.platform.network.crypto.ApiKeyDeriver
import com.example.demoproject.platform.network.crypto.ApiSigner
import com.example.demoproject.platform.network.crypto.ApiUserAgentEncoder
import com.example.demoproject.platform.network.crypto.provider.DeviceFingerprint
import com.example.demoproject.platform.network.crypto.provider.UidProvider
import com.example.demoproject.platform.network.interceptor.AuthInterceptor
import com.example.demoproject.platform.network.interceptor.ClientKeyInterceptor
import com.example.demoproject.platform.network.interceptor.LocaleHeaderInterceptor
import com.example.demoproject.platform.network.interceptor.SigningEncryptionInterceptor
import com.example.demoproject.platform.network.provider.AuthTokenProvider
import com.example.demoproject.platform.network.provider.RetrofitProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkFrameworkModule {

    @Provides
    @Singleton
    fun provideNetworkConfig(impl: DefaultNetworkConfig): NetworkConfig = impl

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        // Backend may require every proxy_info / nested key to be present; omitting
        // default-valued fields breaks strict validators.
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(networkConfig: NetworkConfig): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (networkConfig.verboseHttpLogging) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            // Bearer tokens are sensitive — never let them surface in logcat
            // even when verbose body logging is on.
            redactHeader(NetworkHeaders.AUTHORIZATION)
        }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenProvider: AuthTokenProvider): AuthInterceptor =
        AuthInterceptor(tokenProvider)

    @Provides
    @Singleton
    fun provideClientKeyInterceptor(networkConfig: NetworkConfig): ClientKeyInterceptor =
        ClientKeyInterceptor(networkConfig)

    @Provides
    @Singleton
    fun provideSigningEncryptionInterceptor(
        networkConfig: NetworkConfig,
        keyDeriver: ApiKeyDeriver,
        signer: ApiSigner,
        cipher: ApiBodyCipher,
        uaEncoder: ApiUserAgentEncoder,
        uidProvider: UidProvider,
        deviceFingerprint: DeviceFingerprint,
    ): SigningEncryptionInterceptor =
        SigningEncryptionInterceptor(
            networkConfig = networkConfig,
            keyDeriver = keyDeriver,
            signer = signer,
            cipher = cipher,
            uaEncoder = uaEncoder,
            uidProvider = uidProvider,
            deviceFingerprint = deviceFingerprint,
        )

    /**
     * Interceptor ordering rationale:
     *  1. [ClientKeyInterceptor] stamps the non-sensitive `X-Client-Key` first,
     *     mirroring a plain routing header.
     *  2. [AuthInterceptor] attaches the bearer token, which must be visible to
     *     the server before any body transformation.
     *  3. [LocaleHeaderInterceptor] attaches the current system language for
     *     backend-localized dynamic copy without an app-level locale override.
     *  4. [SigningEncryptionInterceptor] signs + encrypts the body and injects
     *     the `ua` header. Running AFTER auth means the bearer is preserved
     *     untouched; running BEFORE the logger means the logger only ever sees
     *     ciphertext in production builds, eliminating accidental PII leaks.
     *  5. [HttpLoggingInterceptor] observes the final wire payload.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        networkConfig: NetworkConfig,
        authInterceptor: AuthInterceptor,
        clientKeyInterceptor: ClientKeyInterceptor,
        localeHeaderInterceptor: LocaleHeaderInterceptor,
        signingEncryptionInterceptor: SigningEncryptionInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(networkConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(networkConfig.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(networkConfig.writeTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(clientKeyInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(localeHeaderInterceptor)
            .addInterceptor(signingEncryptionInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        networkConfig: NetworkConfig,
        client: OkHttpClient,
        json: Json,
    ): Retrofit =
        RetrofitProvider.create(networkConfig.baseUrl, client, json)
}
