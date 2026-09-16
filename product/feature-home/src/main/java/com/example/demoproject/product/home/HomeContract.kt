package com.example.demoproject.product.home

import com.example.demoproject.platform.data.model.OnlinePresence
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.data.model.VideoShow
import com.example.demoproject.platform.data.network.dto.HomeListRequestDto
import com.example.demoproject.product.profile.ProfileGiftUi

data class HomeUiState(
    val coinBalance: Int = 0,
    val callFreeMin: Int = 0,
    val selectedFilter: OnlineFilter = OnlineFilter.All,
    val pages: Map<OnlineFilter, OnlineTabPage> = OnlineFilter.entries.associateWith {
        OnlineTabPage()
    },
    /** When non-null, the Online video-show overlay is open for this user id. */
    val videoShowUserId: String? = null,
    val isGiftSheetVisible: Boolean = false,
    val gifts: List<ProfileGiftUi> = emptyList(),
    val selectedGiftId: Long? = null,
    val isGiftCatalogLoading: Boolean = false,
    val isGiftSending: Boolean = false,
    /** Absolute SVGA URL after a successful send; null when idle. */
    val giftAnimationUrl: String? = null,
) {
    val currentPage: OnlineTabPage
        get() = pages[selectedFilter] ?: OnlineTabPage()

    val videoShowUser: OnlineUserUi?
        get() = videoShowUserId?.let { id ->
            currentPage.users.firstOrNull { it.id == id && it.videoShow != null }
        }

    val videoShowUsers: List<OnlineUserUi>
        get() = currentPage.users.filter { it.videoShow != null }
}

/** Cached first-page + paging state for one Online filter tab. */
data class OnlineTabPage(
    val users: List<OnlineUserUi> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    /** First-load skeleton for this tab. */
    val isLoading: Boolean = false,
    /** Pull-to-refresh indicator; keeps existing list visible. */
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    /** True after at least one successful first-page load (enables cache hit). */
    val hasLoaded: Boolean = false,
)

data class OnlineUserUi(
    val id: String,
    /** Backend public `user_id` for `/home/info`; falls back to [id] when blank. */
    val externalUserId: String,
    val nickname: String,
    val age: Int,
    val avatarUrl: String?,
    val presence: OnlinePresence,
    val showFreeBadge: Boolean,
    val videoShow: VideoShowUi? = null,
) {
    val prefersMessageAction: Boolean
        get() = presence.prefersMessageAction

    val profileRouteUserId: String
        get() = externalUserId.ifBlank { id }

    val displayNameAge: String
        get() = if (age > 0) "$nickname, $age" else nickname
}

data class VideoShowUi(
    val mediaId: String,
    val videoUrl: String,
    val coverUrl: String?,
)

enum class OnlineFilter(
    val flag: Int,
    val language: String? = null,
) {
    All(HomeListRequestDto.FLAG_RECOMMENDED),
    New(HomeListRequestDto.FLAG_OFFLINE),
    Following(HomeListRequestDto.FLAG_FOLLOWING),
    Popular(HomeListRequestDto.FLAG_ONLINE_HOSTS),
    English(HomeListRequestDto.FLAG_RECOMMENDED, language = "en"),
    Spanish(HomeListRequestDto.FLAG_RECOMMENDED, language = "es"),
    Portuguese(HomeListRequestDto.FLAG_RECOMMENDED, language = "pt"),
}

sealed interface HomeIntent {
    /** Force reload the current tab (pull-to-refresh / retry). */
    data object Refresh : HomeIntent
    data object LoadMore : HomeIntent
    data class SelectFilter(val filter: OnlineFilter) : HomeIntent
    data class OpenUserProfile(val user: OnlineUserUi) : HomeIntent
    data class OpenUserAction(val user: OnlineUserUi) : HomeIntent
    data object OpenCoins : HomeIntent
    data class ReportUser(val userId: String) : HomeIntent
    data object CloseVideoShow : HomeIntent
    data object NextVideoShow : HomeIntent
    data object OpenVideoShowProfile : HomeIntent
    data object OpenVideoShowGift : HomeIntent
    data object DismissVideoShowGiftSheet : HomeIntent
    data class SelectVideoShowGift(val giftId: Long) : HomeIntent
    data object SendVideoShowGift : HomeIntent
    data object DismissVideoShowGiftAnimation : HomeIntent
    data object StartVideoShowCall : HomeIntent
}

sealed interface HomeEffect {
    data class OpenProfile(val externalUserId: String) : HomeEffect
    data class OpenChatDetail(val conversationId: String, val nickname: String) : HomeEffect
    data class StartVideoCall(
        val userId: String,
        val nickname: String,
        val avatarUrl: String = "",
        val age: Int = 0,
        val videoUrl: String = "",
        val coverUrl: String = "",
    ) : HomeEffect
    data object OpenStore : HomeEffect
    data class ShowMessage(val message: String) : HomeEffect
}

fun User.toOnlineUserUi(showFreeBadge: Boolean): OnlineUserUi {
    val presence = onlinePresence()
    return OnlineUserUi(
        id = id,
        externalUserId = externalUserId.ifBlank { id },
        nickname = nickname,
        age = age,
        avatarUrl = avatar,
        presence = presence,
        showFreeBadge = showFreeBadge && presence == OnlinePresence.Online,
        videoShow = videoShow?.toUi(),
    )
}

fun VideoShow.toUi(): VideoShowUi = VideoShowUi(
    mediaId = mediaId,
    videoUrl = videoUrl,
    coverUrl = coverUrl,
)
