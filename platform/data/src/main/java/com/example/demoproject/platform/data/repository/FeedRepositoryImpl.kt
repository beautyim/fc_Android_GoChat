package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.FeedPage
import com.example.demoproject.platform.data.model.Post
import com.example.demoproject.platform.data.network.api.FeedApi
import com.example.demoproject.platform.data.network.dto.FeedsListRequestDto
import com.example.demoproject.platform.data.network.mapper.toDomainPosts
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FeedRepositoryImpl(
    private val feedApi: FeedApi,
) : FeedRepository {
    private val cached = MutableStateFlow<List<Post>>(emptyList())
    private var lastId: String = ""

    override fun observeCachedFeed(): Flow<List<Post>> = cached.asStateFlow()

    override suspend fun refreshFeed(page: Int, category: String?): AppResult<FeedPage> {
        val requestLastId = if (page <= 1) "" else lastId
        return safeApiCall {
            feedApi.getFeeds(
                FeedsListRequestDto(lastId = requestLastId),
            )
        }.map { dto ->
            val posts = dto.toDomainPosts()
            cached.value = if (page <= 1) posts else cached.value + posts
            lastId = posts.lastOrNull()?.id.orEmpty().ifBlank { lastId }
            FeedPage(posts = posts, hasMore = posts.isNotEmpty())
        }
    }

    override suspend fun getPostDetail(id: String): AppResult<Post> {
        val cachedHit = cached.value.firstOrNull { it.id == id }
        if (cachedHit != null) return AppResult.Success(cachedHit)
        return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, "Post detail endpoint not wired")
    }
}
