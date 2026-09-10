package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.Comment
import com.example.demoproject.platform.data.model.Gender
import com.example.demoproject.platform.data.model.Post
import com.example.demoproject.platform.data.model.PostCategory
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.data.network.api.PostApi
import com.example.demoproject.platform.data.network.dto.MomentCreateMediaDto
import com.example.demoproject.platform.data.network.dto.PostPhotoDto
import com.example.demoproject.platform.data.network.dto.MomentCreateRequestDto
import com.example.demoproject.platform.data.network.dto.MomentDeleteRequestDto
import com.example.demoproject.platform.data.network.dto.UserOperateLikeRequestDto
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.safeApiCall
import com.example.demoproject.platform.network.safeApiCallUnit

class PostRepositoryImpl(
    private val postApi: PostApi,
) : PostRepository {
    override suspend fun getPostCategories(): AppResult<List<PostCategory>> =
        AppResult.Success(fallbackPostCategories)

    override suspend fun createPost(
        content: String,
        images: List<String>,
        categoryId: Int,
        categoryName: String,
    ): AppResult<Post> {
        val medias = images.map { url ->
            MomentCreateMediaDto(
                mediaType = PostPhotoDto.MEDIA_IMAGE,
                url = url,
            )
        }
        val body = MomentCreateRequestDto(
            contentType = MomentCreateRequestDto.CONTENT_TYPE_IMAGE_TEXT,
            content = content,
            tags = listOfNotNull(categoryName.takeIf { it.isNotBlank() } ?: categoryId.takeIf { it > 0 }?.toString()),
            medias = medias,
        )
        return safeApiCall { postApi.createMoment(body) }.map { dto ->
            Post(
                id = dto?.id?.toString().orEmpty().ifBlank { System.currentTimeMillis().toString() },
                author = placeholderUser,
                content = content,
                images = images,
                category = categoryName.ifBlank { categoryId.toString() },
                likeCount = 0,
                commentCount = 0,
                isLiked = false,
                createdAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun deletePost(id: String): AppResult<Unit> {
        val momentId = id.toLongOrNull()
            ?: return AppResult.UnknownError(message = "invalid post id")
        return safeApiCallUnit { postApi.deleteMoment(MomentDeleteRequestDto(id = momentId)) }
    }

    override suspend fun likePost(id: String): AppResult<Unit> = likeOrUnlike(id, like = true)

    override suspend fun unlikePost(id: String): AppResult<Unit> = likeOrUnlike(id, like = false)

    private suspend fun likeOrUnlike(id: String, like: Boolean): AppResult<Unit> {
        val targetId = id.toLongOrNull()
            ?: return AppResult.UnknownError(message = "invalid post id")
        return safeApiCallUnit {
            postApi.likeContent(
                UserOperateLikeRequestDto(
                    fromType = UserOperateLikeRequestDto.FROM_TYPE_IMAGE_TEXT,
                    targetId = targetId,
                    type = if (like) {
                        UserOperateLikeRequestDto.TYPE_LIKE
                    } else {
                        UserOperateLikeRequestDto.TYPE_UNLIKE
                    },
                    feedId = targetId,
                ),
            )
        }
    }

    override suspend fun getComments(postId: String, page: Int): AppResult<List<Comment>> =
        AppResult.Success(emptyList())

    override suspend fun addComment(postId: String, content: String): AppResult<Comment> =
        AppResult.Success(
            Comment(
                id = System.currentTimeMillis().toString(),
                author = placeholderUser,
                content = content,
                likeCount = 0,
                isLiked = false,
                createdAt = System.currentTimeMillis(),
            ),
        )

    override suspend fun getMyPostsForIcebreakers(limit: Int): AppResult<List<Post>> =
        AppResult.Success(emptyList())

    override suspend fun getLocalPostRepliesTotal(authorUserId: String): Int = 0

    private companion object {
        val fallbackPostCategories = listOf(
            PostCategory(1, "Dreams"),
            PostCategory(2, "Feelings"),
            PostCategory(3, "Memories"),
            PostCategory(4, "Lifestyle"),
        )

        val placeholderUser = User(
            id = "0",
            nickname = "",
            avatar = null,
            gender = Gender.Other,
            age = 0,
            bio = "",
            isOnline = false,
            lastActiveAt = 0L,
        )
    }
}
