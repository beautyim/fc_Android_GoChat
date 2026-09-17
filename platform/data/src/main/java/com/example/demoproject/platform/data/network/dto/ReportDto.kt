package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportIndexRequestDto(
    @SerialName("target_id")
    @Serializable(with = LenientLongSerializer::class)
    val targetId: Long,
    @SerialName("from_type")
    @Serializable(with = LenientIntSerializer::class)
    val fromType: Int = FROM_TYPE_USER,
) {
    companion object {
        const val FROM_TYPE_USER: Int = 1
    }
}

@Serializable
data class ReportIndexResponseDto(
    @SerialName("user_info") val userInfo: ReportUserInfoDto? = null,
    @SerialName("report_type") val reportType: List<ReportTypeDto> = emptyList(),
)

@Serializable
data class ReportUserInfoDto(
    @Serializable(with = LenientLongSerializer::class)
    val uid: Long = 0,
    val nickname: String = "",
    val avatar: String? = null,
    @SerialName("small_avatar") val smallAvatar: String? = null,
)

@Serializable
data class ReportTypeDto(
    @Serializable(with = LenientIntSerializer::class)
    val id: Int = 0,
    val title: String = "",
    /** Absolute or CDN-relative reason icon URL when provided by backend. */
    val icon: String? = null,
    @Serializable(with = LenientIntSerializer::class)
    val unused: Int = 0,
)

@Serializable
data class ReportHandleRequestDto(
    @SerialName("target_id")
    @Serializable(with = LenientLongSerializer::class)
    val targetId: Long,
    @SerialName("from_type")
    @Serializable(with = LenientIntSerializer::class)
    val fromType: Int = ReportIndexRequestDto.FROM_TYPE_USER,
    val content: String? = null,
    val type: String? = null,
    val photos: List<String> = emptyList(),
)
