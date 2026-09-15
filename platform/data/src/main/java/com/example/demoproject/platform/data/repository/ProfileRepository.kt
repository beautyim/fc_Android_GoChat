package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.AlbumPhoto
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.network.paging.OffsetPageRequest
import com.example.demoproject.platform.network.result.AppResult
import kotlinx.coroutines.flow.Flow

data class MyPrivateUnlockCounts(
    val photoCount: Int,
    val videoCount: Int,
)

data class EditProfileData(
    val user: User,
    val level: Int,
    val sex: Int,
    val sign: String,
    val background: String?,
    val speakLanguages: List<String>,
    val tags: List<EditProfileTag>,
)

data class EditProfileTag(
    val id: Int,
    val name: String,
)

data class EditProfileUpdate(
    val nickname: String? = null,
    val sign: String? = null,
    val birthday: String? = null,
    val sex: Int? = null,
    val avatar: String? = null,
    val background: String? = null,
    val countryCode: String? = null,
    val email: String? = null,
    val emailCode: String? = null,
    val speakLanguages: List<String>? = null,
    val userId: String? = null,
    val tags: List<Int>? = null,
)

data class EditProfileUpdateResult(
    val callbackType: String? = null,
    val callbackTitle: String? = null,
    val callbackMessage: String? = null,
)

data class AlbumMediaPage(
    val items: List<AlbumPhoto>,
    val hasMore: Boolean,
    val lastTime: Int,
    val totalCount: Int,
)

/** Public profile home used by the profile screen (self or other). */
data class ProfileHomeDetail(
    val user: User,
    val backgroundUrl: String?,
    val followingCount: Int,
    val followerCount: Int,
    val media: List<AlbumPhoto>,
    val isSelf: Boolean,
    /** From `account_info.money` on `/home/my`; null for other-user homes. */
    val coinBalance: Int? = null,
)

/**
 * User profile operations, backed by `/home/my`, `/home/info`, `/user/search`,
 * `/user/update-info`, `/user/disband`, `/focus/list`, `/focus/follow`, `/home/list`
 * and the Room `UserDao` cache.
 *
 * Caching strategy: network-first, local cache for offline viewing. The
 * cache key is the stringified `uid`.
 */
interface ProfileRepository {

    /** Observes the cached [User] for [id] (stringified `uid`). */
    fun observeCachedUser(id: String): Flow<User?>

    /**
     * One-shot read from the Room cache (no network). Used when another
     * endpoint omits VIP flags that the UI still needs (e.g. `/visitor/list`).
     */
    suspend fun getCachedUser(id: String): User?

    /** `home/my` — the signed-in user's profile. */
    suspend fun getMyProfile(): AppResult<User>

    /**
     * `home/my` + `/album/list` — signed-in user's profile home for the profile screen
     * (header, counts, banner, album images & videos).
     */
    suspend fun getMyHomeDetail(): AppResult<ProfileHomeDetail>

    /**
     * `home/info` — another user's public profile home by external `user_id`.
     * Prefer [User.externalUserId] from list/search payloads.
     */
    suspend fun getUserHomeDetail(externalUserId: String): AppResult<ProfileHomeDetail>

    /** `home/my` — album count used as a lightweight unlock hint. */
    suspend fun getMyPhotoUnlockCount(): AppResult<Int>

    /** Not exposed by the Spicy doc; returns zeroed counts until a dedicated endpoint exists. */
    suspend fun getMyPrivateUnlockCounts(): AppResult<MyPrivateUnlockCounts>

    /** `/user/info` — edit-profile initialization data. */
    suspend fun getEditProfileData(): AppResult<EditProfileData>

    /** `/user/update-info` — partial edit-profile update. */
    suspend fun updateEditProfile(update: EditProfileUpdate): AppResult<EditProfileUpdateResult>

    /** `user/disband` — deregister and disband the current logged-in account. */
    suspend fun disbandAccount(): AppResult<Unit>

    /**
     * `user/update-info` — edits the current profile. Every argument is
     * optional; passing `null` means "leave this field untouched".
     *
     * @param gender 1 = Male, 2 = Female, 3 = Non-binary.
     * @param birthday `yyyy-MM-dd` formatted.
     */
    suspend fun updateProfile(
        nickname: String? = null,
        avatar: String? = null,
        gender: Int? = null,
        birthday: String? = null,
        bio: String? = null,
    ): AppResult<User>

    /** `user/search` — another user's profile summary by numeric UID. */
    suspend fun getUserProfile(id: String): AppResult<User>

    /** `/album/list` — public/my album photos by target uid. */
    suspend fun getAlbumPhotos(
        targetUserId: String,
        page: Int = 1,
        pageCount: Int = 20,
    ): AppResult<List<AlbumPhoto>>

    /** `/album/list` — paged album media for the edit-profile screen. */
    suspend fun getEditProfileAlbumPage(
        lastTime: Int = 0,
        pageCount: Int = 20,
        mediaType: Int = 0,
    ): AppResult<AlbumMediaPage>

    /** `/album/add` — add local photos to my album. */
    suspend fun addAlbumPhotos(localUris: List<String>): AppResult<Unit>

    /** `/album/delete` — delete own album photos by media ids. */
    suspend fun deleteAlbumPhotos(ids: List<Long>): AppResult<Unit>

    /** `focus/follow` with `flag=add`. */
    suspend fun followUser(id: String): AppResult<Unit>

    /** `focus/follow` with `flag=del`. */
    suspend fun unfollowUser(id: String): AppResult<Unit>

    /** `focus/list` — paged accounts the target user follows. */
    suspend fun getFollowingUsers(
        targetUserId: String,
        page: Int,
        pageSize: Int = OffsetPageRequest.DEFAULT_PAGE_SIZE,
    ): AppResult<FollowUsersPage>

    /** `focus/fans-list` — paged followers of the target user. */
    suspend fun getFollowerUsers(
        targetUserId: String,
        page: Int,
        pageSize: Int = OffsetPageRequest.DEFAULT_PAGE_SIZE,
    ): AppResult<FollowUsersPage>

    /** `focus/fans-list` total follower count (`total_num` from the API). */
    suspend fun getFollowerCount(targetUserId: String): AppResult<Int>

    /**
     * `/home/list` — paged discover users.
     *
     * @param flag see [com.example.demoproject.platform.data.network.dto.HomeListRequestDto]
     * @param language optional `filter_list.language` value (e.g. `en`)
     */
    suspend fun discoverUsers(
        page: Int,
        pageSize: Int = OffsetPageRequest.DEFAULT_PAGE_SIZE,
        flag: Int = com.example.demoproject.platform.data.network.dto.HomeListRequestDto.FLAG_RECOMMENDED,
        language: String? = null,
    ): AppResult<DiscoverUsersPage>
}

/** Page from `POST /home/list`. */
data class DiscoverUsersPage(
    val users: List<User>,
    val hasMore: Boolean,
)

/**
 * Page from `POST focus/list` / `POST focus/fans-list`.
 *
 * [FocusListRequestDto][com.example.demoproject.platform.data.network.dto.FocusListRequestDto]
 * carries no page size, so callers must page off [hasMore] rather than the
 * returned list length.
 */
data class FollowUsersPage(
    val users: List<User>,
    val hasMore: Boolean,
    val total: Int,
)
