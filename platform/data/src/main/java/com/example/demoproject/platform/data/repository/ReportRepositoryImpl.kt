package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.network.api.ReportApi
import com.example.demoproject.platform.data.network.dto.ReportHandleRequestDto
import com.example.demoproject.platform.data.network.dto.ReportIndexRequestDto
import com.example.demoproject.platform.data.network.dto.ReportIndexResponseDto
import com.example.demoproject.platform.data.network.toPicUrlOrNull
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.safeApiCall
import com.example.demoproject.platform.network.safeApiCallUnit
import com.example.demoproject.platform.s3.MediaUploadService

class ReportRepositoryImpl(
    private val reportApi: ReportApi,
    private val mediaUploadService: MediaUploadService,
) : ReportRepository {

    override suspend fun loadReportInit(targetUid: Long, fromType: Int): AppResult<ReportInitData> {
        if (targetUid <= 0L) {
            return AppResult.BizError(
                code = AppResult.CODE_EMPTY_PAYLOAD,
                message = "Invalid user ID",
            )
        }
        return safeApiCall {
            reportApi.getReportIndex(
                ReportIndexRequestDto(targetId = targetUid, fromType = fromType),
            )
        }.map { it.toDomain(fallbackUid = targetUid) }
    }

    override suspend fun submitReport(
        targetUid: Long,
        content: String,
        reasonIds: List<Int>,
        photoUris: List<String>,
        fromType: Int,
    ): AppResult<Unit> {
        if (targetUid <= 0L) {
            return AppResult.BizError(
                code = AppResult.CODE_EMPTY_PAYLOAD,
                message = "Invalid user ID",
            )
        }
        val validReasonIds = reasonIds.filter { it > 0 }
        val trimmed = content.trim()
        if (trimmed.isEmpty() && validReasonIds.isEmpty()) {
            return AppResult.BizError(
                code = AppResult.CODE_EMPTY_PAYLOAD,
                message = "Please select a reason",
            )
        }
        val remarks = trimmed.ifEmpty {
            // UI may omit free-text remarks; keep a non-blank payload for the handle API.
            "Report"
        }
        val photoUrls = ArrayList<String>(photoUris.size)
        for (uri in photoUris) {
            when (val uploaded = mediaUploadService.uploadImageIfNeeded(uri, flag = "report")) {
                is AppResult.Success -> {
                    val path = uploaded.data.remotePath.trim()
                    if (path.isNotEmpty()) photoUrls += path
                }
                is AppResult.Failure -> return uploaded
            }
        }
        val reasonType = validReasonIds.joinToString(",").takeIf { it.isNotBlank() }
        return safeApiCallUnit {
            reportApi.submitReport(
                ReportHandleRequestDto(
                    targetId = targetUid,
                    fromType = fromType,
                    content = remarks,
                    type = reasonType,
                    photos = photoUrls,
                ),
            )
        }
    }
}

private fun ReportIndexResponseDto.toDomain(fallbackUid: Long): ReportInitData {
    val user = userInfo
    return ReportInitData(
        targetUid = user?.uid?.takeIf { it > 0L } ?: fallbackUid,
        nickname = user?.nickname.orEmpty(),
        avatarUrl = (user?.smallAvatar ?: user?.avatar).toPicUrlOrNull() ?: user?.avatar,
        reasons = reportType
            .filter { it.id > 0 && it.title.isNotBlank() }
            .map {
                ReportReason(
                    id = it.id,
                    title = it.title,
                    iconUrl = it.icon.toPicUrlOrNull() ?: it.icon?.takeIf { url -> url.isNotBlank() },
                )
            },
    )
}
