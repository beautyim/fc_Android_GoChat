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

class ReportRepositoryImpl(
    private val reportApi: ReportApi,
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
        photoUrls: List<String>,
        fromType: Int,
    ): AppResult<Unit> {
        if (targetUid <= 0L) {
            return AppResult.BizError(
                code = AppResult.CODE_EMPTY_PAYLOAD,
                message = "Invalid user ID",
            )
        }
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return AppResult.BizError(
                code = AppResult.CODE_EMPTY_PAYLOAD,
                message = "Remarks are required",
            )
        }
        val reasonType = reasonIds
            .filter { it > 0 }
            .joinToString(",")
            .takeIf { it.isNotBlank() }
        return safeApiCallUnit {
            reportApi.submitReport(
                ReportHandleRequestDto(
                    targetId = targetUid,
                    fromType = fromType,
                    content = trimmed,
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
            .map { ReportReason(id = it.id, title = it.title) },
    )
}
