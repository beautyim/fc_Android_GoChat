package com.example.demoproject.platform.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.demoproject.platform.data.local.db.entity.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE category = :category ORDER BY createdAt DESC")
    fun observeByCategory(category: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getById(id: String): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()

    @Query("DELETE FROM posts WHERE cachedAt < :before")
    suspend fun deleteStale(before: Long)

    @Query("SELECT COALESCE(SUM(commentCount), 0) FROM posts WHERE authorId = :authorId")
    suspend fun getTotalCommentCountByAuthor(authorId: String): Int
}
