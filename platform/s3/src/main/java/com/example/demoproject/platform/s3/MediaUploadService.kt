package com.example.demoproject.platform.s3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.safeApiCall
import com.example.demoproject.platform.network.safeApiCallUnit
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class MediaUploadService(
    private val context: Context,
    private val uploadApi: UploadApi,
    private val mediaUploader: MediaUploader,
) {

    suspend fun uploadImageIfNeeded(localUri: String?, flag: String): AppResult<MediaUploadResult> {
        val source = localUri?.trim().orEmpty()
        if (source.isEmpty()) {
            return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, "Missing upload source")
        }
        if (!source.isLocalMediaReference()) {
            return AppResult.Success(MediaUploadResult(remotePath = source))
        }

        val file = source.resolveLocalFile(context)
            ?: return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, "Upload file not found")
        val dimensions = file.readImageDimensions()
        val params = when (
            val result = safeApiCall {
                uploadApi.getUploadParams(
                    GetUploadParamsRequestDto(
                        type = GetUploadParamsRequestDto.TYPE_IMAGE,
                        flag = flag,
                        md5 = file.md5Hex(),
                        width = dimensions?.first,
                        height = dimensions?.second,
                    ),
                )
            }
        ) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }

        if (params.isUpload == 1) {
            return AppResult.Success(
                params.toMediaUploadResult(
                    width = dimensions?.first,
                    height = dimensions?.second,
                ),
            )
        }

        val uploadConfig = params.toUploadConfig(file)
        when (val event = mediaUploader.upload(file, uploadConfig).first { it !is UploadEvent.Progress }) {
            is UploadEvent.Success -> AppLogger.i(TAG, "uploaded media remote=${event.remotePath}")
            is UploadEvent.Failure -> return AppResult.UnknownError(
                message = event.cause.message ?: AppResult.DEFAULT_UNKNOWN_MESSAGE,
                cause = event.cause,
            )
            is UploadEvent.Progress -> Unit
        }

        if (params.uploadId > 0) {
            when (val ack = safeApiCallUnit { uploadApi.uploadSuccess(UploadSuccessRequestDto(listOf(params.uploadId))) }) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return ack
            }
        }

        return AppResult.Success(
            params.toMediaUploadResult(
                width = dimensions?.first,
                height = dimensions?.second,
            ),
        )
    }

    suspend fun uploadVideoIfNeeded(localUri: String?, flag: String): AppResult<MediaUploadResult> {
        val source = localUri?.trim().orEmpty()
        if (source.isEmpty()) {
            return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, "Missing upload source")
        }
        if (!source.isLocalMediaReference()) {
            return AppResult.Success(MediaUploadResult(remotePath = source))
        }

        val file = source.resolveLocalFile(context)
            ?: return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, "Upload file not found")
        val videoMeta = file.readVideoMetadata()
        val params = when (
            val result = safeApiCall {
                uploadApi.getUploadParams(
                    GetUploadParamsRequestDto(
                        type = GetUploadParamsRequestDto.TYPE_VIDEO,
                        flag = flag,
                        md5 = file.md5Hex(),
                        width = videoMeta?.width,
                        height = videoMeta?.height,
                    ),
                )
            }
        ) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }

        if (params.isUpload == 1) {
            return AppResult.Success(
                params.toMediaUploadResult(
                    width = videoMeta?.width,
                    height = videoMeta?.height,
                    durationSec = videoMeta?.durationSec,
                ),
            )
        }

        val uploadConfig = params.toUploadConfig(
            file = file,
            fallbackMediaType = "video/mp4",
        )
        when (val event = mediaUploader.upload(file, uploadConfig).first { it !is UploadEvent.Progress }) {
            is UploadEvent.Success -> AppLogger.i(TAG, "uploaded media remote=${event.remotePath}")
            is UploadEvent.Failure -> return AppResult.UnknownError(
                message = event.cause.message ?: AppResult.DEFAULT_UNKNOWN_MESSAGE,
                cause = event.cause,
            )
            is UploadEvent.Progress -> Unit
        }

        val coverPath = params.resolveCoverPath()
        if (!params.coverToken.isNullOrBlank() && !coverPath.isNullOrBlank()) {
            val coverFile = file.extractVideoCoverFrame(context)
            if (coverFile != null) {
                try {
                    val coverConfig = UploadConfig(
                        host = params.host,
                        filePath = coverPath,
                        mediaType = params.coverUploadType
                            ?: params.dir?.coverUploadType?.takeIf { it.isNotBlank() }
                            ?: "image/jpeg",
                        authorization = params.coverToken,
                        date = params.date,
                    )
                    when (val coverEvent = mediaUploader.upload(coverFile, coverConfig).first { it !is UploadEvent.Progress }) {
                        is UploadEvent.Success -> AppLogger.i(TAG, "uploaded video cover remote=${coverEvent.remotePath}")
                        is UploadEvent.Failure -> {
                            AppLogger.w(TAG, "video cover upload failed: ${coverEvent.cause.message}")
                        }
                        is UploadEvent.Progress -> Unit
                    }
                } finally {
                    coverFile.delete()
                }
            }
        }

        if (params.uploadId > 0) {
            when (val ack = safeApiCallUnit { uploadApi.uploadSuccess(UploadSuccessRequestDto(listOf(params.uploadId))) }) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return ack
            }
        }

        return AppResult.Success(
            params.toMediaUploadResult(
                width = videoMeta?.width,
                height = videoMeta?.height,
                durationSec = videoMeta?.durationSec,
            ),
        )
    }

    private fun UploadParamsDto.toUploadConfig(
        file: File,
        fallbackMediaType: String? = null,
    ): UploadConfig =
        UploadConfig(
            host = host,
            filePath = resolveRemotePath(),
            mediaType = uploadType ?: fallbackMediaType ?: file.name.resolveMimeType(),
            authorization = token,
            date = date,
        )

    private fun UploadParamsDto.resolveRemotePath(): String =
        dir?.path?.takeIf { it.isNotBlank() }
            ?: dir?.smallUrl?.takeIf { it.isNotBlank() }
            ?: ""

    private fun UploadParamsDto.resolveCoverPath(): String? =
        dir?.coverPath?.takeIf { it.isNotBlank() }
            ?: coverUrl?.takeIf { it.isNotBlank() }

    private fun UploadParamsDto.resolveSmallUrl(): String? =
        dir?.smallUrl?.takeIf { it.isNotBlank() }
            ?: smallUrl?.takeIf { it.isNotBlank() }

    private fun UploadParamsDto.toMediaUploadResult(
        width: Int?,
        height: Int?,
        durationSec: Int? = null,
    ): MediaUploadResult =
        MediaUploadResult(
            remotePath = resolveRemotePath(),
            smallUrl = resolveSmallUrl(),
            coverPath = resolveCoverPath(),
            uploadId = uploadId,
            width = width,
            height = height,
            durationSec = durationSec,
        )

    private companion object {
        const val TAG = "MediaUploadService"
    }
}

private data class VideoMetadata(
    val width: Int?,
    val height: Int?,
    val durationSec: Int?,
)

private fun String.isLocalMediaReference(): Boolean =
    startsWith("content://") || startsWith("file://") || File(this).exists()

private fun String.resolveLocalFile(context: Context): File? {
    val raw = this
    if (raw.startsWith("file://")) return File(Uri.parse(raw).path.orEmpty()).takeIf { it.exists() }
    val direct = File(raw)
    if (direct.exists()) return direct
    if (!raw.startsWith("content://")) return null

    val uri = Uri.parse(raw)
    val cacheFile = File(context.cacheDir, "upload_${System.nanoTime()}")
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
        } ?: return null
        cacheFile
    }.getOrNull()
}

private fun File.readImageDimensions(): Pair<Int, Int>? =
    runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        if (options.outWidth > 0 && options.outHeight > 0) {
            options.outWidth to options.outHeight
        } else {
            null
        }
    }.getOrNull()

private fun File.readVideoMetadata(): VideoMetadata? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?.takeIf { it > 0L }
        val durationSec = durationMs?.let { ((it + 999L) / 1000L).toInt().coerceAtLeast(1) }
        VideoMetadata(width = width, height = height, durationSec = durationSec)
    } catch (_: Throwable) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun File.extractVideoCoverFrame(context: Context): File? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: return null
        val coverFile = File(context.cacheDir, "video_cover_${System.nanoTime()}.jpg")
        FileOutputStream(coverFile).use { output ->
            frame.compress(Bitmap.CompressFormat.JPEG, 85, output)
        }
        frame.recycle()
        coverFile.takeIf { it.exists() && it.length() > 0L }
    } catch (_: Throwable) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun File.md5Hex(): String =
    runCatching {
        val digest = MessageDigest.getInstance("MD5")
        inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }.getOrDefault(name)

private fun String.resolveMimeType(): String =
    when (substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "mp4" -> "video/mp4"
        else -> "application/octet-stream"
    }
