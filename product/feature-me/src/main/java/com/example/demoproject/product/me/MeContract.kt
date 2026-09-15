package com.example.demoproject.product.me

import com.example.demoproject.platform.data.model.Gender

data class MeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val nickname: String = "",
    val age: Int = 0,
    val gender: Gender = Gender.Other,
    val avatarUrl: String? = null,
    val countryFlag: String = "",
    val countryName: String = "",
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val externalUserId: String = "",
    val coinBalance: Int = 0,
    val isVip: Boolean = false,
    /** Display-ready expiry (e.g. `26/12/31`); null when not VIP / unknown. */
    val vipExpiryText: String? = null,
    val chatUnreadCount: Int = 0,
)

sealed interface MeIntent {
    data object Refresh : MeIntent
    data object OpenPublicProfile : MeIntent
    data object OpenFollowers : MeIntent
    data object OpenFollowing : MeIntent
    data object OpenCamera : MeIntent
    data object OpenAddCoins : MeIntent
    data object OpenVip : MeIntent
    data object OpenGift : MeIntent
    data object OpenVerification : MeIntent
    data object OpenSettings : MeIntent
}

sealed interface MeEffect {
    data class OpenPublicProfile(val externalUserId: String) : MeEffect
    data class OpenRelationshipList(
        val type: RelationshipListType,
        val count: Int,
    ) : MeEffect
    data object OpenStore : MeEffect
    data object OpenVip : MeEffect
    data object OpenSettings : MeEffect
    data class ShowMessage(val message: String) : MeEffect
}
