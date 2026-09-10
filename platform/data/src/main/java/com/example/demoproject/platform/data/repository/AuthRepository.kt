package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.network.result.AppResult

data class AuthLoginResult(
    val token: String,
    val isRegister: Boolean,
    val user: User?,
    val matchFreeCount: Int = 0,
    val callFreeMin: Int = 0,
)

data class GoogleLoginConfig(
    val appKey: String,
    val appSecret: String,
) {
    val isConfigured: Boolean get() = appKey.isNotBlank()
}

interface AuthRepository {

    suspend fun loginWithEmail(email: String, password: String): AppResult<AuthLoginResult>

    suspend fun loginAsGuest(): AppResult<AuthLoginResult>

    suspend fun loginWithGoogle(openId: String, identityToken: String): AppResult<AuthLoginResult>

    suspend fun loginWithApple(openId: String, identityToken: String): AppResult<AuthLoginResult>

    suspend fun sendResetPasswordCode(email: String): AppResult<Unit>

    suspend fun checkEmailCode(email: String, code: String): AppResult<Unit>

    suspend fun resetPassword(email: String, password: String, code: String): AppResult<Unit>

    suspend fun getGoogleLoginConfig(): AppResult<GoogleLoginConfig>

    suspend fun isEmailRegistered(email: String): AppResult<Boolean>
}
