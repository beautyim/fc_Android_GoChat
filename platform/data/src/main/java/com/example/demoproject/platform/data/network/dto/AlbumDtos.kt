package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlbumListRequestDto(
    @SerialName("page_count") val pageCount: Int = 20,
    @SerialName("target_uid") val targetUid: Long? = null,
    @SerialName("last_time") val lastTime: Int = 0,
    @SerialName("media_type") val mediaType: Int = MEDIA_TYPE_ALL,
) {
    companion object {
        const val MEDIA_TYPE_ALL: Int = 0
    }
}

@Serializable
data class AlbumPage(
    val items: List<AlbumMediaDto>,
    val hasMore: Boolean,
    val lastTime: Int,
    val totalCount: Int,
)

@Serializable
data class AlbumListResponseDto(
    val list: List<AlbumMediaDto> = emptyList(),
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("has_more") val hasMore: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("last_time") val lastTime: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val count: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val photo: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val video: Int = 0,
)

@Serializable
data class AlbumMediaDto(
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("from_type") val fromType: Int = 0,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("media_id") val mediaId: Long = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("media_type") val mediaType: Int = MEDIA_IMAGE,
    @Serializable(with = LenientIntSerializer::class)
    val width: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val height: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val duration: Int = 0,
    val url: String? = null,
    @SerialName("small_url") val smallUrl: String? = null,
    @SerialName("small_photo_url") val smallPhotoUrl: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("add_time") val addTime: Long = 0,
    @Serializable(with = LenientLongSerializer::class)
    val uid: Long = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("view_type") val viewType: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val status: Int = 0,
    val content: String? = null,
    val md5: String? = null,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("upload_id") val uploadId: Long = 0,
) {
    companion object {
        const val MEDIA_IMAGE: Int = 1
        const val MEDIA_VIDEO: Int = 2
    }

    val isVideo: Boolean
        get() = mediaType == MEDIA_VIDEO
}

@Serializable
data class AlbumAddMediaDto(
    @SerialName("media_type") val mediaType: Int,
    val url: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
    val content: String? = null,
    val md5: String? = null,
    @SerialName("upload_id") val uploadId: Long? = null,
)

@Serializable
data class AlbumAddRequestDto(
    @SerialName("album_list") val albumList: List<AlbumAddMediaDto>,
)

@Serializable
data class AlbumDeleteRequestDto(
    @SerialName("media_ids") val mediaIds: List<Long>,
)
