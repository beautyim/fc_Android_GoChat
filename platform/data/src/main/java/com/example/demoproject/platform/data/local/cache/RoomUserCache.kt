package com.example.demoproject.platform.data.local.cache

import com.example.demoproject.platform.data.local.db.dao.UserDao
import com.example.demoproject.platform.data.local.db.entity.toDomain
import com.example.demoproject.platform.data.local.db.entity.toEntity
import com.example.demoproject.platform.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomUserCache(
    private val userDao: UserDao,
) : UserCache {
    override fun observeById(id: String): Flow<User?> =
        userDao.observeById(id).map { it?.toDomain() }

    override suspend fun getById(id: String): User? =
        userDao.getById(id)?.toDomain()

    override suspend fun upsert(user: User) {
        userDao.upsert(user.toEntity())
    }

    override suspend fun upsertAll(list: List<User>) {
        if (list.isEmpty()) return
        userDao.upsertAll(list.map { it.toEntity() })
    }
}
