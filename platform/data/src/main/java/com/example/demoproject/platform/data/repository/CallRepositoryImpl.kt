package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.CallRecordPage
import com.example.demoproject.platform.data.network.api.CallApi
import com.example.demoproject.platform.data.network.dto.CallRecordsRequestDto
import com.example.demoproject.platform.data.network.mapper.toDomainPage
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepositoryImpl @Inject constructor(
    private val callApi: CallApi,
) : CallRepository {

    override suspend fun getCallRecords(page: Int, type: Int): AppResult<CallRecordPage> {
        val safePage = page.coerceAtLeast(1)
        if (safePage > CallRecordsRequestDto.MAX_PAGE) {
            return AppResult.Success(CallRecordPage(records = emptyList(), hasMore = false))
        }
        return safeApiCall {
            callApi.records(
                CallRecordsRequestDto(
                    page = safePage,
                    type = type,
                    isMissed = if (type == CallRepository.CALL_RECORD_TYPE_MISSED) 1 else 0,
                    isActive = if (type == CallRepository.CALL_RECORD_TYPE_OUTGOING) 1 else 0,
                    isMatch = if (type == CallRepository.CALL_RECORD_TYPE_MATCH) 1 else 0,
                ),
            )
        }.map { response -> response.toDomainPage() }
    }
}
