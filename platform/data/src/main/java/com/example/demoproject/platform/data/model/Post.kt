package com.example.demoproject.platform.data.model

data class Post(
    val id: String,
    val author: User,
    val content: String,
    val images: List<String>,
    val category: String,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val createdAt: Long,
)

data class Comment(
    val id: String,
    val author: User,
    val content: String,
    val likeCount: Int,
    val isLiked: Boolean,
    val createdAt: Long,
)
