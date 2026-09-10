package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.ReportHandleRequestDto
import com.example.demoproject.platform.data.network.dto.ReportIndexRequestDto
import com.example.demoproject.platform.data.network.dto.ReportIndexResponseDto
import com.example.demoproject.platform.network.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ReportApi {

    /** `/report/index` — report page init data for the given target. */
    @POST("report/index")
    suspend fun getReportIndex(@Body body: ReportIndexRequestDto): ApiResponse<ReportIndexResponseDto>

    /** `/report/handle` — submit a report. */
    @POST("report/handle")
    suspend fun submitReport(@Body body: ReportHandleRequestDto): ApiResponse<Unit?>
}
