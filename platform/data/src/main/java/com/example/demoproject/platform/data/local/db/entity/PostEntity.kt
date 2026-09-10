package com.example.demoproject.platform.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val content: String,
    val images: String,
    val category: String,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val createdAt: Long,
    val cachedAt: Long = System.currentTimeMillis(),
)
