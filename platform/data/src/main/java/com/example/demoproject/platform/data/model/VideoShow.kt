package com.example.demoproject.platform.data.model

/** Absolute CDN URLs for a user's short video show from `/home/list`. */
data class VideoShow(
    val mediaId: String,
    val videoUrl: String,
    val coverUrl: String?,
)
