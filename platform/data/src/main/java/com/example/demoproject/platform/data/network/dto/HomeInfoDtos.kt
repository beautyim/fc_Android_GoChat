package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.network.serializer.LenientBooleanSerializer
import com.example.demoproject.platform.network.serializer.LenientIntSerializer
import com.example.demoproject.platform.network.serializer.LenientLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class HomeInfoRequestDto(
    @SerialName("user_id") val userId: String,
    val ssr: HomeInfoSsrDto = HomeInfoSsrDto(),
    @SerialName("private_type") val privateType: Int = PRIVATE_TYPE_ALL,
) {
    companion object {
        const val PRIVATE_TYPE_ALL: Int = -1
        const val PRIVATE_TYPE_PUBLIC: Int = 0
        const val PRIVATE_TYPE_PRIVATE: Int = 1
    }
}

@Serializable
data class HomeInfoSsrDto(
    val language: String = "en",
)

@Serializable
data class HomeInfoResponseDto(
    @SerialName("user_info") val userInfo: UserDto = UserDto(),
    @SerialName("post_num") val postNum: HomeInfoPostNumDto = HomeInfoPostNumDto(),
    val media: HomeInfoMediaPageDto = HomeInfoMediaPageDto(),
    /** Profile album photos/videos from `/home/info` (`album.list`). */
    val album: AlbumListResponseDto = AlbumListResponseDto(),
    val tab: List<HomeInfoTabDto> = emptyList(),
    val social: List<HomeInfoSocialDto> = emptyList(),
    @SerialName("head_info") val headInfo: JsonElement? = null,
)

@Serializable
data class HomeInfoPostNumDto(
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("video_num") val videoNum: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("text_num") val textNum: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("voice_num") val voiceNum: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("drama_num") val dramaNum: Int = 0,
)

@Serializable
data class HomeInfoMediaPageDto(
    val list: List<HomeInfoMediaDto> = emptyList(),
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("first_id") val firstId: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("last_id") val lastId: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("has_more") val hasMore: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("has_up") val hasUp: Int = 0,
)

@Serializable
data class HomeInfoMediaDto(
    @Serializable(with = LenientLongSerializer::class)
    val id: Long = 0,
    @Serializable(with = LenientLongSerializer::class)
    val uid: Long = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("content_type") val contentType: Int = 0,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("content_id") val contentId: Long = 0,
    val title: String = "",
    val content: String = "",
    val cover: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val duration: Int = 0,
    @SerialName("media_url") val mediaUrl: String = "",
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("view_count") val viewCount: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("like_count") val likeCount: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("gift_count") val giftCount: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("collect_count") val collectCount: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("comment_count") val commentCount: Int = 0,
    val tags: List<String> = emptyList(),
    val images: List<HomeInfoMediaImageDto> = emptyList(),
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("view_permission") val viewPermission: Int = 1,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("comment_permission") val commentPermission: Int = 1,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("allow_download") val allowDownload: Int = 0,
    @SerialName("publish_time") val publishTime: String = "",
    val extra: JsonObject? = null,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("origin_type") val originType: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("view_price") val viewPrice: Int = 0,
    @SerialName("view_content") val viewContent: String = "",
    @Serializable(with = LenientBooleanSerializer::class)
    @SerialName("is_unlock") val isUnlock: Boolean = false,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("private_type") val privateType: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("episode_num") val episodeNum: Int = 0,
) {
    /** True when this media item belongs to the private album feed. */
    val isPrivateAlbum: Boolean
        get() = privateType == HomeInfoRequestDto.PRIVATE_TYPE_PRIVATE

    /** `content_type == 1` per the `/home/info` contract; `media_url` carries the playable video. */
    val isVideo: Boolean
        get() = contentType == MomentCreateRequestDto.CONTENT_TYPE_VIDEO
}

@Serializable
data class HomeInfoMediaImageDto(
    val url: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val width: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val height: Int = 0,
    @Serializable(with = LenientIntSerializer::class)
    val price: Int = 0,
)

@Serializable
data class HomeInfoTabDto(
    val labelKey: String = "",
    val showDropdown: Boolean = false,
    val dropdownOpen: Boolean = false,
    val selected: Boolean = false,
)

@Serializable
data class HomeInfoSocialDto(
    @Serializable(with = LenientLongSerializer::class)
    val id: Long = 0,
    val account: String = "",
    val link: String = "",
    val name: String = "",
    val logo: String = "",
)
