package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.Gender
import com.example.demoproject.platform.data.network.dto.MATCH_SEX_FEMALE
import com.example.demoproject.platform.data.network.dto.MATCH_TYPE_VIDEO
import com.example.demoproject.platform.network.result.AppResult

data class MatchStartCandidate(
    val id: Long?,
    val nickname: String,
    val avatarUrl: String?,
    val gender: Gender,
    val age: Int?,
    val bio: String,
    /** External backend `user_id`; use this for `/home/info`, not numeric `uid`. */
    val profileUserId: String? = null,
    val videoCallGold: Int = 0,
)

data class MatchInfo(
    val matchFreeCount: Int,
    val freeUserMatchPrice: Int,
    val payUserMatchPrice: Int,
    val vipMatchPrice: Int,
    val avatarList: List<String>,
)

enum class MatchStartAction {
    UserProfile,
    DirectVideoCall,
}

data class MatchStartInfo(
    val sessionId: Long?,
    val matchId: Long?,
    val matchedUser: MatchStartCandidate?,
    val nextAction: MatchStartAction,
    val rawPayload: String?,
    /** Remaining free match count when returned by `/match/start` or `/match/next`. */
    val matchFreeCount: Int? = null,
)

interface MatchRepository {

    suspend fun getMatchInfo(source: String = "unknown"): AppResult<MatchInfo>

    suspend fun startMatch(
        matchType: Int = MATCH_TYPE_VIDEO,
        matchSex: Int = MATCH_SEX_FEMALE,
        source: String = "unknown",
    ): MatchStartResult

    suspend fun endMatch(matchSessionId: Long, source: String = "unknown"): AppResult<Unit>

    suspend fun closeMatch(source: String = "unknown"): AppResult<Unit>

    suspend fun nextMatch(
        matchType: Int = MATCH_TYPE_VIDEO,
        matchSex: Int = MATCH_SEX_FEMALE,
        source: String = "unknown",
    ): MatchStartResult
}
