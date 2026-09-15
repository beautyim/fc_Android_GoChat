package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.blocked.BlockedUsersStore
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.flow.StateFlow

interface BlockRepository {
    val blockedUids: StateFlow<Set<Long>>

    fun isBlocked(uid: Long): Boolean
    fun isBlocked(userId: String): Boolean

    suspend fun getBlockedUsers(page: Int = 1): AppResult<List<User>>

    /** Same fetch as [getBlockedUsers], but keeps the server's `has_more` for paging UIs. */
    suspend fun getBlockedUsersPage(page: Int = 1): AppResult<BlockedUsersPage>

    suspend fun blockUser(targetUid: Long): AppResult<Unit>
    suspend fun unblockUser(targetUid: Long): AppResult<Unit>

    /** Replaces in-memory blocked set from a full/paged sync (caller may page). */
    fun replaceLocalBlocked(uids: Collection<Long>)

    fun clearLocal()
}

/** Page from `POST /user/black-list`. */
data class BlockedUsersPage(
    val users: List<User>,
    val hasMore: Boolean,
)
