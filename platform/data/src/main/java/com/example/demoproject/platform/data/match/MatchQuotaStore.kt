package com.example.demoproject.platform.data.match

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Global free-match / flash-match quota for the signed-in user.
 * `/app/open`, match APIs, login and MQTT `type=7` / `data.type=8` write here so Match UI
 * stays consistent without page-private copies.
 */
data class MatchQuota(
    val matchFreeCount: Int = 0,
    val flashCount: Int = 0,
)

class MatchQuotaStore {
    private val _quota = MutableStateFlow(MatchQuota())
    val quota: StateFlow<MatchQuota> = _quota.asStateFlow()

    fun update(matchFreeCount: Int? = null, flashCount: Int? = null) {
        if (matchFreeCount == null && flashCount == null) return
        _quota.update { current ->
            current.copy(
                matchFreeCount = matchFreeCount?.coerceAtLeast(0) ?: current.matchFreeCount,
                flashCount = flashCount?.coerceAtLeast(0) ?: current.flashCount,
            )
        }
    }

    fun clear() {
        _quota.value = MatchQuota()
    }
}
