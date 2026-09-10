package com.example.demoproject.platform.s3

import kotlinx.coroutines.flow.Flow
import java.io.File

interface MediaUploader {
    fun upload(file: File, config: UploadConfig): Flow<UploadEvent>
}

data class UploadConfig(
    val uploadUrl: String? = null,
    val host: String? = null,
    val filePath: String? = null,
    val mediaType: String? = null,
    val authorization: String? = null,
    val date: String? = null,
    val headers: Map<String, String> = emptyMap(),
) {
    fun resolveTargetUrl(): String {
        val direct = uploadUrl?.trim().orEmpty()
        if (direct.isNotEmpty()) return direct
        val hostValue = host?.trim()?.trimEnd('/').orEmpty()
        require(hostValue.isNotEmpty()) { "uploadUrl or host is required" }
        val pathValue = filePath?.trim()?.trimStart('/').orEmpty()
        require(pathValue.isNotEmpty()) { "filePath is required when uploadUrl is empty" }
        return "$hostValue/$pathValue"
    }

    fun resolveHeaders(): Map<String, String> {
        val merged = linkedMapOf<String, String>()
        merged.putAll(headers.filterKeys { it.isNotBlank() })
        if (!authorization.isNullOrBlank() && !merged.containsKey(HEADER_AUTHORIZATION)) {
            merged[HEADER_AUTHORIZATION] = authorization
        }
        if (!date.isNullOrBlank() && !merged.containsKey(HEADER_DATE)) {
            merged[HEADER_DATE] = date
        }
        return merged
    }

    fun resolveRemotePathFallback(): String =
        filePath?.takeIf { it.isNotBlank() } ?: resolveTargetUrl()

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_DATE = "Date"
    }
}

sealed class UploadEvent {
    data class Progress(
        val percent: Int,
        val uploadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
    ) : UploadEvent()

    data class Success(
        val remotePath: String,
        val httpCode: Int? = null,
    ) : UploadEvent()

    data class Failure(val cause: Throwable) : UploadEvent()
}
