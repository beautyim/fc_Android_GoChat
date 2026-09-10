package com.example.demoproject.platform.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.demoproject.platform.data.local.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY cachedAt DESC")
    fun observeAll(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(users: List<UserEntity>)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM users WHERE cachedAt < :before")
    suspend fun deleteStale(before: Long)
}
