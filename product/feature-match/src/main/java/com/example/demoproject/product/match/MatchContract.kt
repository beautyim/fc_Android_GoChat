package com.example.demoproject.product.match

import com.example.demoproject.platform.data.network.dto.MATCH_SEX_FEMALE
import com.example.demoproject.product.store.CoinPayGuideUiState

/**
 * Match tab "All" chip. `/match/start` only accepts `match_sex` 1/2, so the
 * repository call still falls back to female until the backend supports 0.
 */
const val MATCH_SEX_ALL: Int = 0

enum class MatchPhase {
    Ready,
    Matching,
    Matched,
    Failed,
}

data class MatchUiState(
    val isLoading: Boolean = false,
    val phase: MatchPhase = MatchPhase.Ready,
    val coinBalance: Int = 0,
    val isVip: Boolean = false,
    val chatUnreadCount: Int = 0,
    val matchFreeCount: Int = 0,
    val flashCount: Int = 0,
    /** Coins charged per video match for the current user tier. */
    val matchPrice: Int = 0,
    val payUserMatchPrice: Int = 0,
    val vipMatchPrice: Int = 0,
    /** Backend `match_sex`; defaults to female per product rule. */
    val matchSex: Int = MATCH_SEX_FEMALE,
    /** Pending sheet selection; committed to [matchSex] on Apply. */
    val draftMatchSex: Int = MATCH_SEX_FEMALE,
    val isFilterSheetVisible: Boolean = false,
    /** Gender-guide popup shown before matching when filter is All/Male. */
    val isGenderGuideVisible: Boolean = false,
    /** Gender selected inside the guide; defaults to female. */
    val guideMatchSex: Int = MATCH_SEX_FEMALE,
    /** Coin pay-guide sheet after insufficient balance on match start. */
    val coinPayGuide: CoinPayGuideUiState? = null,
    val statusMessage: String = "",
) {
    val isSearching: Boolean
        get() = phase == MatchPhase.Matching || phase == MatchPhase.Matched
}

sealed interface MatchIntent {
    data object Refresh : MatchIntent
    data object OpenCoins : MatchIntent
    data object OpenFilter : MatchIntent
    data object DismissFilter : MatchIntent
    data class SelectMatchSex(val matchSex: Int) : MatchIntent
    data object ApplyFilters : MatchIntent
    data object ResetFilters : MatchIntent
    data object StartVideoMatch : MatchIntent
    data object CancelVideoMatch : MatchIntent
    data object DismissGenderGuide : MatchIntent
    data class SelectGenderGuideSex(val matchSex: Int) : MatchIntent
    data object ConfirmGenderGuide : MatchIntent
    data object DismissCoinPayGuide : MatchIntent
    data class PurchaseCoinPayGuideCoin(val offerId: Long) : MatchIntent
    data class PurchaseCoinPayGuideSale(val offerId: Long) : MatchIntent
}

sealed interface MatchEffect {
    data object OpenStore : MatchEffect
    data class ShowMessage(val message: String) : MatchEffect
    data class OpenProfile(val externalUserId: String) : MatchEffect
    data class StartVideoCall(
        val entryId: String,
        val userId: String,
        val nickname: String,
        val avatarUrl: String = "",
        val age: Int = 0,
    ) : MatchEffect
}
