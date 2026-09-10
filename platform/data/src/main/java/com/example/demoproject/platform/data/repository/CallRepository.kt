package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.CallRecordPage
import com.example.demoproject.platform.network.result.AppResult

interface CallRepository {
    suspend fun getCallRecords(page: Int, type: Int = CALL_RECORD_TYPE_ALL): AppResult<CallRecordPage>

    companion object {
        const val CALL_RECORD_TYPE_ALL: Int = 0
        const val CALL_RECORD_TYPE_INCOMING: Int = 1
        const val CALL_RECORD_TYPE_OUTGOING: Int = 2
        const val CALL_RECORD_TYPE_MISSED: Int = 3
        const val CALL_RECORD_TYPE_CANCELLED: Int = 4
        const val CALL_RECORD_TYPE_MATCH: Int = 5
    }
}
