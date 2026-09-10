package com.example.demoproject.platform.s3

import android.content.Context
import com.example.demoproject.platform.network.config.NetworkConfig
import com.example.demoproject.platform.network.constants.NetworkHeaders
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Non-Hilt factory for S3 upload stack.
 */
object S3Runtime {
    fun createMediaUploadService(
        context: Context,
        retrofit: Retrofit,
        networkConfig: NetworkConfig,
    ): MediaUploadService {
        val uploadApi = retrofit.create(UploadApi::class.java)
        val logging = HttpLoggingInterceptor().apply {
            level = if (networkConfig.verboseHttpLogging) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader(NetworkHeaders.AUTHORIZATION)
        }
        val uploadClient = OkHttpClient.Builder()
            .connectTimeout(networkConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(networkConfig.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(networkConfig.writeTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
        val uploader: MediaUploader = S3MediaUploader(uploadClient)
        return MediaUploadService(
            context = context.applicationContext,
            uploadApi = uploadApi,
            mediaUploader = uploader,
        )
    }
}
