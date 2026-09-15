package com.example.demoproject.ui.designsystem.media

data class MediaViewerItem(
    val id: Long,
    val isVideo: Boolean,
    val imageUrl: String?,
    val videoUrl: String?,
    val coverUrl: String?,
    val durationSeconds: Int = 0,
)
