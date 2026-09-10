package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.dto.ApiBusinessMessage
import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    @SerialName("login_from") val loginFrom: Int,
    val email: String? = null,
    val password: String? = null,
    val userInfo: ThirdPartyLoginUserInfoDto? = null,
)

@Serializable
data class ThirdPartyLoginUserInfoDto(
    val openId: String,
    val identityToken: String? = null,
)

@Serializable
data class LoginResponseDto(
    val token: String = "",
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_register") val isRegister: Int = 0,
    @SerialName("user_info") val userInfo: UserDto? = null,
    val callback: ApiCallbackDto? = null,
) : ApiBusinessMessage {
    override val message: String
        get() = callback?.funcData?.message.orEmpty()
}

@Serializable
data class ApiCallbackDto(
    @SerialName("func_name") val funcName: String = "",
    @SerialName("func_data") val funcData: ApiCallbackDataDto? = null,
)

@Serializable
data class ApiCallbackDataDto(
    @SerialName("msg") val message: String = "",
)

@Serializable
data class PasswordResetCodeResponseDto(
    val callback: ApiCallbackDto? = null,
) : ApiBusinessMessage {
    override val message: String
        get() = callback?.funcData?.message.orEmpty()
}

@Serializable
data class ChangePasswordRequestDto(
    val email: String,
    val password: String,
    val code: String,
)

@Serializable
data class SendEmailCodeRequestDto(
    val email: String,
    val type: Int = TYPE_RESET_PASSWORD,
) {
    companion object {
        const val TYPE_REGISTER: Int = 1
        const val TYPE_RESET_PASSWORD: Int = 2
        const val TYPE_BIND_EMAIL: Int = 3
    }
}

@Serializable
data class CheckEmailCodeRequestDto(
    val email: String,
    val code: String,
)

@Serializable
data class GoogleLoginConfigDto(
    @SerialName("app_key") val appKey: String = "",
    @SerialName("app_secret") val appSecret: String = "",
)

@Serializable
data class CheckEmailRequestDto(
    val email: String,
)

@Serializable
data class CheckEmailResponseDto(
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("is_exists") val isExists: Int = 0,
)
