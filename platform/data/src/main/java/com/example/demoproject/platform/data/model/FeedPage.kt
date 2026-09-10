package com.example.demoproject.platform.data.model

data class FeedPage(
    val posts: List<Post>,
    val hasMore: Boolean,
)
