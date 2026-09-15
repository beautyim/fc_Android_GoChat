package com.example.demoproject.product.profile.media

import com.example.demoproject.platform.data.model.AlbumPhoto
import com.example.demoproject.ui.designsystem.media.MediaViewerItem

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
