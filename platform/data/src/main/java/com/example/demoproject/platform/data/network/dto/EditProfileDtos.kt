package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class EditProfileInfoResponseDto(
    @SerialName("user_info") val userInfo: UserDto = UserDto(),
    val tags: List<EditProfileTagDto> = emptyList(),
)

@Serializable
data class EditProfileTagDto(
    val id: Int = 0,
    val name: String = "",
)

@Serializable
data class EditProfileUpdateRequestDto(
    @SerialName("user_info") val userInfo: EditProfileUpdateUserInfoDto,
)

@Serializable
data class EditProfileUpdateUserInfoDto(
    val nickname: String? = null,
    val sign: String? = null,
    val birthday: String? = null,
    @Serializable(with = LenientIntSerializer::class)
    val sex: Int? = null,
    val avatar: String? = null,
    val background: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val email: EditProfileEmailDto? = null,
    @SerialName("speak_language") val speakLanguage: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val tags: List<Int>? = null,
)

@Serializable
data class EditProfileEmailDto(
    val email: String,
    @SerialName("email_code") val emailCode: String,
)

@Serializable
data class EditProfileUpdateResponseDto(
    val callback: EditProfileCallbackDto? = null,
)

@Serializable
data class EditProfileCallbackDto(
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val content: String = "",
    val data: JsonObject? = null,
) {
    val displayMessage: String
        get() = message.ifBlank { content }
}
