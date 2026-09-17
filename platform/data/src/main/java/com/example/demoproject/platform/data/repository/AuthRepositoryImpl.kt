package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.call.CallFreeMinStore
import com.example.demoproject.platform.data.match.MatchQuotaStore
import com.example.demoproject.platform.data.model.Session
import com.example.demoproject.platform.data.network.api.AuthApi
import com.example.demoproject.platform.data.network.dto.ChangePasswordRequestDto
import com.example.demoproject.platform.data.network.dto.CheckEmailCodeRequestDto
import com.example.demoproject.platform.data.network.dto.CheckEmailRequestDto
import com.example.demoproject.platform.data.network.dto.LoginRequestDto
import com.example.demoproject.platform.data.network.dto.LoginResponseDto
import com.example.demoproject.platform.data.network.dto.SendEmailCodeRequestDto
import com.example.demoproject.platform.data.network.dto.ThirdPartyLoginUserInfoDto
import com.example.demoproject.platform.data.network.mapper.toDomain
import com.example.demoproject.platform.data.promotion.PromotionPopupStore
import com.example.demoproject.platform.data.session.SessionManager
import com.example.demoproject.platform.network.crypto.md5Hex
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.safeApiCall
import com.example.demoproject.platform.network.safeApiCallNullable
import com.example.demoproject.platform.network.safeApiCallUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
    private val matchQuotaStore: MatchQuotaStore,
    private val callFreeMinStore: CallFreeMinStore,
    private val promotionPopupStore: PromotionPopupStore,
) : AuthRepository {

    override suspend fun loginWithEmail(email: String, password: String): AppResult<AuthLoginResult> =
        login(
            LoginRequestDto(
                loginFrom = LoginFromEmail,
                email = email,
                password = password.md5Hex(),
            ),
        )

    override suspend fun loginAsGuest(): AppResult<AuthLoginResult> =
        login(LoginRequestDto(loginFrom = LoginFromGuest))

    override suspend fun loginWithGoogle(openId: String, identityToken: String): AppResult<AuthLoginResult> =
        login(
            LoginRequestDto(
                loginFrom = LoginFromGoogle,
                userInfo = ThirdPartyLoginUserInfoDto(openId = openId, identityToken = identityToken),
            ),
        )

    override suspend fun loginWithApple(openId: String, identityToken: String): AppResult<AuthLoginResult> =
        login(
            LoginRequestDto(
                loginFrom = LoginFromApple,
                userInfo = ThirdPartyLoginUserInfoDto(
                    openId = openId,
                    identityToken = identityToken,
                ),
            ),
        )

    override suspend fun sendResetPasswordCode(email: String): AppResult<Unit> =
        safeApiCallNullable {
            authApi.sendChangePasswordCode(SendEmailCodeRequestDto(email = email))
        }.map { Unit }

    override suspend fun checkEmailCode(email: String, code: String): AppResult<Unit> =
        safeApiCallNullable {
            authApi.checkEmailCode(CheckEmailCodeRequestDto(email = email, code = code))
        }.map { Unit }

    override suspend fun resetPassword(email: String, password: String, code: String): AppResult<Unit> =
        safeApiCallUnit {
            authApi.changePassword(
                ChangePasswordRequestDto(
                    email = email,
                    password = password.md5Hex(),
                    code = code,
                ),
            )
        }

    override suspend fun getGoogleLoginConfig(): AppResult<GoogleLoginConfig> =
        safeApiCall { authApi.getGoogleLoginConfig() }
            .map { dto -> GoogleLoginConfig(appKey = dto.appKey, appSecret = dto.appSecret) }

    override suspend fun isEmailRegistered(email: String): AppResult<Boolean> =
        safeApiCall { authApi.checkEmail(CheckEmailRequestDto(email = email)) }
            .map { dto -> dto.isExists == 1 }

    private suspend fun login(body: LoginRequestDto): AppResult<AuthLoginResult> {
        return when (val response = safeApiCall { authApi.login(body) }) {
            is AppResult.Success -> response.data.toLoginResult()
            is AppResult.Failure -> response
        }
    }

    private suspend fun LoginResponseDto.toLoginResult(): AppResult<AuthLoginResult> {
        if (token.isBlank()) {
            return AppResult.BizError(
                code = AppResult.CODE_EMPTY_PAYLOAD,
                message = "Login token is missing",
            )
        }
        val user = userInfo?.toDomain()
        val userId = userInfo?.uid?.takeIf { it > 0L }?.toString().orEmpty()
        // New registration (incl. delete+reregister with reused guest uid) must not keep
        // a consumed treasure schedule from the previous account on this device.
        if (isRegister == 1 && userId.isNotBlank()) {
            promotionPopupStore.clear(userId)
        }
        sessionManager.saveSession(
            Session(
                userId = userId,
                token = token,
                profileComplete = userInfo?.sex != 0,
            ),
        )
        val matchFreeCount = userInfo?.matchFreeCount ?: 0
        val callFreeMin = userInfo?.callFreeMin ?: 0
        matchQuotaStore.update(matchFreeCount = matchFreeCount)
        callFreeMinStore.update(callFreeMin)
        return AppResult.Success(
            AuthLoginResult(
                token = token,
                isRegister = isRegister == 1,
                user = user,
                matchFreeCount = matchFreeCount,
                callFreeMin = callFreeMin,
            ),
        )
    }

    private companion object {
        const val LoginFromEmail: Int = 2
        const val LoginFromGoogle: Int = 3
        const val LoginFromApple: Int = 6
        const val LoginFromGuest: Int = 7
    }
}
