package com.example.demoproject.platform.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranslationSubmitRequestDto(
    val content: String,
    val target: String,
    @SerialName("from_type") val fromType: Int = FROM_TYPE_MESSAGE,
    @SerialName("from_uid") val fromUid: Long = 0,
    @SerialName("chat_type") val chatType: Int = CHAT_TYPE_PRIVATE,
    val key: String = "",
) {
    companion object {
        const val FROM_TYPE_MESSAGE = 4
        /**
         * Profile bio/self-introduction translation (not a chat message).
         * TODO: confirm against the backend `/translation/submit` `from_type` enum
         * once documented; not yet covered by an existing YAML/spec in this repo.
         */
        const val FROM_TYPE_PROFILE_SIGN = 5
        const val CHAT_TYPE_PRIVATE = 1
    }
}

@Serializable
data class TranslationSubmitResponseDto(
    val text: String = "",
    val language: String = "",
)
