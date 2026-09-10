package com.example.demoproject.platform.data.model

data class AlbumPhoto(
    val mediaId: Long,
    val mediaType: Int,
    val imageUrl: String?,
    val thumbnailUrl: String? = imageUrl,
    val fullImageUrl: String? = imageUrl,
    val coverUrl: String? = thumbnailUrl,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Int = 0,
    val status: Int = 0,
    val addedAtSeconds: Long = 0,
    val content: String = "",
) {
    val isVideo: Boolean
        get() = mediaType == TYPE_VIDEO

    companion object {
        const val TYPE_IMAGE: Int = 1
        const val TYPE_VIDEO: Int = 2
    }
}
