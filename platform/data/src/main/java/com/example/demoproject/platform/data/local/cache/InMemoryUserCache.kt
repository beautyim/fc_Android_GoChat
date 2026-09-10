package com.example.demoproject.platform.data.local.cache

import com.example.demoproject.platform.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

/** In-memory stand-in; prefer [RoomUserCache] when SQLCipher DB is available. */
class InMemoryUserCache : UserCache {
    private val users = ConcurrentHashMap<String, User>()
    private val tick = MutableStateFlow(0L)

    override fun observeById(id: String): Flow<User?> =
        tick.map { users[id] }

    override suspend fun getById(id: String): User? = users[id]

    override suspend fun upsert(user: User) {
        users[user.id] = user
        tick.value = tick.value + 1
    }

    override suspend fun upsertAll(list: List<User>) {
        list.forEach { users[it.id] = it }
        tick.value = tick.value + 1
    }
}
