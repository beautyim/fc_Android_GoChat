package com.example.demoproject.product.profile.media

import com.example.demoproject.platform.data.model.AlbumPhoto

data class MediaViewerItem(
    val id: Long,
    val isVideo: Boolean,
    val imageUrl: String?,
    val videoUrl: String?,
    val coverUrl: String?,
    val durationSeconds: Int = 0,
)

fun AlbumPhoto.toMediaViewerItem(): MediaViewerItem {
    val thumb = thumbnailUrl ?: imageUrl
    val full = fullImageUrl ?: imageUrl ?: thumb
    return MediaViewerItem(
        id = mediaId,
        isVideo = isVideo,
        imageUrl = full,
        videoUrl = if (isVideo) full else null,
        coverUrl = coverUrl ?: thumb,
        durationSeconds = duration,
    )
}
