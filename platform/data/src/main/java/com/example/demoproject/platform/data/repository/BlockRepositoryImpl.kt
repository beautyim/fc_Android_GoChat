package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.blocked.BlockedUsersStore
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.data.network.api.ProfileApi
import com.example.demoproject.platform.data.network.dto.BlackListRequestDto
import com.example.demoproject.platform.data.network.dto.UserBlackRequestDto
import com.example.demoproject.platform.data.network.dto.UserDto
import com.example.demoproject.platform.data.network.mapper.toDomain
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.result.onSuccess
import com.example.demoproject.platform.network.safeApiCall
import com.example.demoproject.platform.network.safeApiCallUnit
import kotlinx.coroutines.flow.StateFlow

class BlockRepositoryImpl(
    private val profileApi: ProfileApi,
    private val blockedUsersStore: BlockedUsersStore,
) : BlockRepository {

    override val blockedUids: StateFlow<Set<Long>> = blockedUsersStore.blockedUids

    override fun isBlocked(uid: Long): Boolean = blockedUsersStore.isBlocked(uid)

    override fun isBlocked(userId: String): Boolean = blockedUsersStore.isBlocked(userId)

    override suspend fun getBlockedUsers(page: Int): AppResult<List<User>> =
        safeApiCall {
            profileApi.getBlackList(BlackListRequestDto(page = page))
        }.map { dto ->
            val users = dto.list.map(UserDto::toDomain)
            if (page <= 1) {
                blockedUsersStore.replaceAll(users.mapNotNull { it.id.toLongOrNull() })
            } else {
                blockedUsersStore.addAll(users.mapNotNull { it.id.toLongOrNull() })
            }
            users
        }

    override suspend fun blockUser(targetUid: Long): AppResult<Unit> {
        if (targetUid <= 0L) {
            return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, "Invalid user ID")
        }
        return safeApiCallUnit {
            profileApi.updateBlackUser(
                UserBlackRequestDto(
                    targetUid = targetUid,
                    flag = UserBlackRequestDto.FLAG_ADD,
                ),
            )
        }.onSuccess { blockedUsersStore.block(targetUid) }
    }

    override suspend fun unblockUser(targetUid: Long): AppResult<Unit> {
        if (targetUid <= 0L) {
            return AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, "Invalid user ID")
        }
        return safeApiCallUnit {
            profileApi.updateBlackUser(
                UserBlackRequestDto(
                    targetUid = targetUid,
                    flag = UserBlackRequestDto.FLAG_DELETE,
                ),
            )
        }.onSuccess { blockedUsersStore.unblock(targetUid) }
    }

    override fun replaceLocalBlocked(uids: Collection<Long>) {
        blockedUsersStore.replaceAll(uids)
    }

    override fun clearLocal() {
        blockedUsersStore.clear()
    }
}
