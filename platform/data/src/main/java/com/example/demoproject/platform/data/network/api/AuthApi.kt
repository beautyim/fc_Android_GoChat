package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.ChangePasswordRequestDto
import com.example.demoproject.platform.data.network.dto.CheckEmailCodeRequestDto
import com.example.demoproject.platform.data.network.dto.CheckEmailRequestDto
import com.example.demoproject.platform.data.network.dto.CheckEmailResponseDto
import com.example.demoproject.platform.data.network.dto.GoogleLoginConfigDto
import com.example.demoproject.platform.data.network.dto.LoginRequestDto
import com.example.demoproject.platform.data.network.dto.LoginResponseDto
import com.example.demoproject.platform.data.network.dto.PasswordResetCodeResponseDto
import com.example.demoproject.platform.data.network.dto.SendEmailCodeRequestDto
import com.example.demoproject.platform.network.dto.ApiResponse
import kotlinx.serialization.json.JsonArray
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("login/handle")
    suspend fun login(@Body body: LoginRequestDto): ApiResponse<LoginResponseDto>

    @POST("login/change-pwd")
    suspend fun changePassword(@Body body: ChangePasswordRequestDto): ApiResponse<Unit?>

    @POST("login/change-pwd-code")
    suspend fun sendChangePasswordCode(
        @Body body: SendEmailCodeRequestDto,
    ): ApiResponse<PasswordResetCodeResponseDto?>

    @POST("login/check-email-code")
    suspend fun checkEmailCode(
        @Body body: CheckEmailCodeRequestDto,
    ): ApiResponse<PasswordResetCodeResponseDto?>

    @POST("login/google-index")
    suspend fun getGoogleLoginConfig(@Body body: JsonArray = JsonArray(emptyList())): ApiResponse<GoogleLoginConfigDto>

    @POST("login/check-email")
    suspend fun checkEmail(@Body body: CheckEmailRequestDto): ApiResponse<CheckEmailResponseDto>
}
