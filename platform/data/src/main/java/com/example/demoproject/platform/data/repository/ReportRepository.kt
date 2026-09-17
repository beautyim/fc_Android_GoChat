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
    /** Backend icon URL when present; UI falls back to local glyphs otherwise. */
    val iconUrl: String? = null,
)

interface ReportRepository {
    suspend fun loadReportInit(targetUid: Long, fromType: Int = FROM_TYPE_USER): AppResult<ReportInitData>

    /**
     * Submits a report. [photoUris] may be local content URIs or already-remote paths;
     * local images are uploaded via S3 before the handle request.
     * [content] may be blank when at least one [reasonIds] entry is selected — the
     * selected reason titles are then sent as remarks.
     */
    suspend fun submitReport(
        targetUid: Long,
        content: String,
        reasonIds: List<Int>,
        photoUris: List<String> = emptyList(),
        fromType: Int = FROM_TYPE_USER,
    ): AppResult<Unit>

    companion object {
        const val FROM_TYPE_USER: Int = 1
    }
}
