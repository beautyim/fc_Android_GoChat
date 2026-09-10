package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.Comment
import com.example.demoproject.platform.data.model.Post
import com.example.demoproject.platform.data.model.PostCategory
import com.example.demoproject.platform.network.result.AppResult

interface PostRepository {
    suspend fun getPostCategories(): AppResult<List<PostCategory>>
    suspend fun createPost(
        content: String,
        images: List<String>,
        categoryId: Int,
        categoryName: String = "",
    ): AppResult<Post>

    suspend fun deletePost(id: String): AppResult<Unit>
    suspend fun likePost(id: String): AppResult<Unit>
    suspend fun unlikePost(id: String): AppResult<Unit>
    suspend fun getComments(postId: String, page: Int = 1): AppResult<List<Comment>>
    suspend fun addComment(postId: String, content: String): AppResult<Comment>
    suspend fun getMyPostsForIcebreakers(limit: Int): AppResult<List<Post>>
    suspend fun getLocalPostRepliesTotal(authorUserId: String): Int
}
