package com.example.demoproject.platform.s3

import com.example.demoproject.platform.common.log.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.IOException
import java.net.URLConnection

class S3MediaUploader(
    private val httpClient: OkHttpClient,
) : MediaUploader {

    override fun upload(file: File, config: UploadConfig): Flow<UploadEvent> = channelFlow {
        if (!file.exists() || !file.isFile) {
            send(UploadEvent.Failure(IllegalArgumentException("file does not exist: ${file.path}")))
            return@channelFlow
        }

        val targetUrl = runCatching { config.resolveTargetUrl() }
            .getOrElse {
                send(UploadEvent.Failure(it))
                return@channelFlow
            }
        AppLogger.i(TAG, "start upload file=${file.name} size=${file.length()} url=$targetUrl")

        val totalBytes = file.length().coerceAtLeast(0L)
        send(UploadEvent.Progress(percent = 0, uploadedBytes = 0L, totalBytes = totalBytes))

        var lastPercent = -1
        val requestBody = ProgressRequestBody(
            file = file,
            contentType = config.mediaType.resolveMimeTypeFallback(file),
        ) { written, total ->
            val safeTotal = total.coerceAtLeast(1L)
            val percent = ((written * 100L) / safeTotal).toInt().coerceIn(0, 100)
            if (percent != lastPercent) {
                lastPercent = percent
                trySend(
                    UploadEvent.Progress(
                        percent = percent,
                        uploadedBytes = written,
                        totalBytes = total,
                    ),
                )
            }
        }

        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .put(requestBody)

        config.resolveHeaders().forEach { (name, value) ->
            if (name.isNotBlank() && value.isNotBlank()) {
                requestBuilder.header(name, value)
            }
        }
        if (!config.mediaType.isNullOrBlank()) {
            requestBuilder.header("Content-Type", config.mediaType)
        }

        runCatching {
            withContext(Dispatchers.IO) {
                httpClient.newCall(requestBuilder.build()).execute()
            }
        }.fold(
            onSuccess = { response ->
                response.use {
                    if (it.isSuccessful) {
                        AppLogger.i(TAG, "upload success code=${it.code} url=$targetUrl")
                        if (lastPercent < 100) {
                            send(UploadEvent.Progress(100, totalBytes, totalBytes))
                        }
                        send(UploadEvent.Success(config.resolveRemotePathFallback(), it.code))
                    } else {
                        val bodySnippet = it.body?.string()?.take(200).orEmpty()
                        AppLogger.w(TAG, "upload rejected code=${it.code} msg=${it.message} body=$bodySnippet")
                        send(
                            UploadEvent.Failure(
                                IOException("S3 upload failed: HTTP ${it.code} ${it.message} $bodySnippet".trim()),
                            ),
                        )
                    }
                }
            },
            onFailure = { error ->
                AppLogger.w(TAG, "s3 upload failed: ${error.message}", error)
                send(UploadEvent.Failure(error))
            },
        )
    }

    private companion object {
        const val TAG = "S3MediaUploader"
    }
}

private class ProgressRequestBody(
    private val file: File,
    private val contentType: String,
    private val onProgress: (written: Long, total: Long) -> Unit,
) : RequestBody() {

    override fun contentType() = contentType.toMediaTypeOrNull()

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val total = file.length()
        var written = 0L
        val buffer = ByteArray(BUFFER_SIZE)
        file.inputStream().use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                written += read
                onProgress(written, total)
            }
        }
    }

    private companion object {
        const val BUFFER_SIZE = 8 * 1024
    }
}

private fun String?.resolveMimeTypeFallback(file: File): String {
    if (!this.isNullOrBlank()) return this
    return URLConnection.guessContentTypeFromName(file.name) ?: "application/octet-stream"
}
