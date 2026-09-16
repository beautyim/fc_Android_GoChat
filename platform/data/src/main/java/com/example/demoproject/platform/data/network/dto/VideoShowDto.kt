package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.JsonNumberOrStringAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Video-show media object.
 *
 * - `/home/list` nests it as `video_show`
 * - `/call/create` and incoming-call MQTT nest it as `user_info.video`
 *
 * Relative `url` / `video_url` / `cover_url` are resolved in mappers via CDN helpers.
 */
@Serializable
data class VideoShowDto(
    @Serializable(with = JsonNumberOrStringAsStringSerializer::class)
    @SerialName("media_id") val mediaId: String = "",
    val url: String = "",
    /** Some call payloads mirror the play path as `video_url`. */
    @SerialName("video_url") val videoUrl: String = "",
    @SerialName("cover_url") val coverUrl: String = "",
)
