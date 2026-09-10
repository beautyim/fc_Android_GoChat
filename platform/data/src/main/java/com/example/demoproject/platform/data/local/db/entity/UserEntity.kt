package com.example.demoproject.platform.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.demoproject.platform.data.model.Gender
import com.example.demoproject.platform.data.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val nickname: String,
    val avatar: String?,
    val gender: String,
    val age: Int,
    val bio: String,
    val birthday: String? = null,
    val email: String? = null,
    val vipExpireTime: String? = null,
    val countryCode: String? = null,
    val countryName: String? = null,
    val isAuthVerified: Boolean = false,
    val isOnline: Boolean,
    val lastActiveAt: Long,
    val isVip: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis(),
)

fun UserEntity.toDomain(): User = User(
    id = id,
    nickname = nickname,
    avatar = avatar,
    gender = runCatching { Gender.valueOf(gender) }.getOrDefault(Gender.Other),
    age = age,
    bio = bio,
    birthday = birthday,
    email = email,
    vipExpireTime = vipExpireTime,
    countryCode = countryCode,
    countryName = countryName,
    isAuthVerified = isAuthVerified,
    isOnline = isOnline,
    lastActiveAt = lastActiveAt,
    isVip = isVip,
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    nickname = nickname,
    avatar = avatar,
    gender = gender.name,
    age = age,
    bio = bio,
    birthday = birthday,
    email = email,
    vipExpireTime = vipExpireTime,
    countryCode = countryCode,
    countryName = countryName,
    isAuthVerified = isAuthVerified,
    isOnline = isOnline,
    lastActiveAt = lastActiveAt,
    isVip = isVip,
)
