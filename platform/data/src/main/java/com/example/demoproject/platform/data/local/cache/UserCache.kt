package com.example.demoproject.platform.data.local.cache

import com.example.demoproject.platform.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserCache {
    fun observeById(id: String): Flow<User?>
    suspend fun getById(id: String): User?
    suspend fun upsert(user: User)
    suspend fun upsertAll(list: List<User>)
}
