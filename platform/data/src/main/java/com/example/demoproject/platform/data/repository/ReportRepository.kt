package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.network.result.AppResult

data class ReportInitData(
    val targetUid: Long,
    val nickname: String,
    val avatarUrl: String?,
    val reasons: List<ReportReason>,
)

data class ReportReason(
    val id: Int,
    val title: String,
)

interface ReportRepository {
    suspend fun loadReportInit(targetUid: Long, fromType: Int = FROM_TYPE_USER): AppResult<ReportInitData>

    /**
     * Submits a report. [photoUrls] are already-uploaded remote paths/URLs
     * (S3 upload stays in the app/UI layer until `:platform:s3` is wired).
     */
    suspend fun submitReport(
        targetUid: Long,
        content: String,
        reasonIds: List<Int>,
        photoUrls: List<String> = emptyList(),
        fromType: Int = FROM_TYPE_USER,
    ): AppResult<Unit>

    companion object {
        const val FROM_TYPE_USER: Int = 1
    }
}
