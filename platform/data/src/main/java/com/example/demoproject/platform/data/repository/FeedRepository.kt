package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.FeedPage
import com.example.demoproject.platform.data.model.Post
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun observeCachedFeed(): Flow<List<Post>>
    suspend fun refreshFeed(page: Int, category: String? = null): AppResult<FeedPage>
    suspend fun getPostDetail(id: String): AppResult<Post>
}
