package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.network.api.FirebaseApi
import com.example.demoproject.platform.data.network.dto.FirebaseTokenRequestDto
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.safeApiCallUnit

/**
 * Uploads FCM device tokens to the backend.
 * Token acquisition (FirebaseMessaging) stays in the app/push layer.
 */
interface PushTokenRepository {
    suspend fun uploadFirebaseToken(token: String): AppResult<Unit>
}

class PushTokenRepositoryImpl(
    private val firebaseApi: FirebaseApi,
) : PushTokenRepository {
    override suspend fun uploadFirebaseToken(token: String): AppResult<Unit> {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, "Empty FCM token")
        }
        return safeApiCallUnit {
            firebaseApi.uploadToken(FirebaseTokenRequestDto(token = trimmed))
        }
    }
}
