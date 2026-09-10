package com.example.demoproject.platform.data.network.dto

import com.example.demoproject.platform.data.network.serializer.UserInfosMapSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for `POST /feeds/list`. */
@Serializable
data class FeedsListRequestDto(
    val flag: Int = FLAG_FOR_YOU,
    @SerialName("last_id") val lastId: String = "",
    @SerialName("content_type") val contentType: Int = 0,
    @SerialName("feeds_id") val feedsId: Long = 0,
    @SerialName("private_type") val privateType: Int = PRIVATE_TYPE_ALL,
    val ssr: FeedsSsrDto = FeedsSsrDto(),
) {
    companion object {
        const val FLAG_FOR_YOU: Int = 1
        const val FLAG_SUBSCRIBING: Int = 2
        const val FLAG_FOLLOWING: Int = 3
        const val FLAG_SERIES: Int = 4
        const val FLAG_RECOMMEND: Int = 5
        const val FLAG_PRIVATE: Int = 6
        const val PRIVATE_TYPE_ALL: Int = -1
    }
}

@Serializable
data class FeedsSsrDto(
    val language: String = "",
)

/** Body for `POST /moment/list`. */
@Serializable
data class MomentListRequestDto(
    @SerialName("target_uid") val targetUid: Long,
    @SerialName("current_id") val currentId: Long = 0,
    @SerialName("more_type") val moreType: Int = 0,
    @SerialName("last_id") val lastId: Long = 0,
)

@Serializable
data class FeedsListResponseDto(
    val list: List<PostDto> = emptyList(),
    @SerialName("user_infos")
    @Serializable(with = UserInfosMapSerializer::class)
    val userInfos: Map<String, UserDto> = emptyMap(),
    @SerialName("last_id") val lastId: String = "0",
    @SerialName("first_id") val firstId: Long = 0,
    @SerialName("has_more") val hasMoreRaw: Int = 0,
    @SerialName("has_up") val hasUpRaw: Int = 0,
) {
    val hasMore: Boolean get() = hasMoreRaw == 1
    val lastIdAsLong: Long get() = lastId.toLongOrNull() ?: 0L
}

@Serializable
data class PostDto(
    val id: Long = 0,
    @SerialName("moment_id") val momentId: String = "",
    val uid: Long = 0,
    @SerialName("content_type") val contentType: Int = 0,
    val title: String = "",
    val content: String = "",
    @SerialName("category_id") val categoryId: Int = 0,
    @SerialName("post_category_info") val postCategoryInfo: PostCategoryDto? = null,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("is_liked") val isLikedRaw: Int = 0,
    @SerialName("publish_time") val publishTime: String? = null,
    @SerialName("add_time") val addTime: String? = null,
    val cover: String? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
    val images: List<PostPhotoDto> = emptyList(),
    @SerialName("photo") val photos: List<PostPhotoDto> = emptyList(),
    @SerialName("media") val media: List<PostPhotoDto> = emptyList(),
) {
    val isLiked: Boolean get() = isLikedRaw == 1
    val resolvedId: String
        get() = when {
            id > 0L -> id.toString()
            momentId.isNotBlank() -> momentId
            else -> ""
        }

    fun mediaItemsForDomain(): List<PostPhotoDto> =
        when {
            images.isNotEmpty() -> images
            media.isNotEmpty() -> media
            else -> photos
        }
}

@Serializable
data class PostCategoryDto(
    val id: Int = 0,
    val name: String = "",
    val label: String = "",
    val icon: String? = null,
)

@Serializable
data class PostPhotoDto(
    val url: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("media_type") val mediaType: Int = MEDIA_IMAGE,
    val width: Int = 0,
    val height: Int = 0,
) {
    companion object {
        const val MEDIA_IMAGE: Int = 1
        const val MEDIA_VIDEO: Int = 2
        const val MEDIA_AUDIO: Int = 3
    }
}

/** Body for `POST /moment/create`. */
@Serializable
data class MomentCreateRequestDto(
    @SerialName("content_type") val contentType: Int = CONTENT_TYPE_IMAGE_TEXT,
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val medias: List<MomentCreateMediaDto>,
    @SerialName("view_permission") val viewPermission: Int = 1,
    @SerialName("comment_permission") val commentPermission: Int = 1,
    @SerialName("allow_download") val allowDownload: Int = 1,
    val price: Int = 0,
) {
    companion object {
        const val CONTENT_TYPE_VIDEO: Int = 1
        const val CONTENT_TYPE_AUDIO: Int = 2
        const val CONTENT_TYPE_IMAGE_TEXT: Int = 3
        const val CONTENT_TYPE_DRAMA: Int = 4
    }
}

@Serializable
data class MomentCreateMediaDto(
    @SerialName("media_type") val mediaType: Int,
    val url: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Int = 0,
)

@Serializable
data class MomentCreateResponseDto(
    val id: Long = 0,
)

@Serializable
data class MomentDeleteRequestDto(
    val id: Long,
)

/** Body for `POST /user-operate/like`. */
@Serializable
data class UserOperateLikeRequestDto(
    @SerialName("from_type") val fromType: Int,
    @SerialName("target_id") val targetId: Long,
    val type: Int,
    @SerialName("feed_id") val feedId: Long,
) {
    companion object {
        const val TYPE_LIKE: Int = 1
        const val TYPE_UNLIKE: Int = 2
        const val FROM_TYPE_VIDEO: Int = 1
        const val FROM_TYPE_IMAGE_TEXT: Int = 3
    }
}
