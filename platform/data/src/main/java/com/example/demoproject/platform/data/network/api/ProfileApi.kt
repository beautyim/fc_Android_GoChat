package com.example.demoproject.platform.data.network.api

import com.example.demoproject.platform.data.network.dto.AlbumDeleteRequestDto
import com.example.demoproject.platform.data.network.dto.AlbumAddRequestDto
import com.example.demoproject.platform.data.network.dto.AlbumListRequestDto
import com.example.demoproject.platform.data.network.dto.AlbumListResponseDto
import com.example.demoproject.platform.data.network.dto.BlackListRequestDto
import com.example.demoproject.platform.data.network.dto.EditProfileInfoResponseDto
import com.example.demoproject.platform.data.network.dto.EditProfileUpdateRequestDto
import com.example.demoproject.platform.data.network.dto.EditProfileUpdateResponseDto
import com.example.demoproject.platform.data.network.dto.FocusFollowRequestDto
import com.example.demoproject.platform.data.network.dto.FocusListRequestDto
import com.example.demoproject.platform.data.network.dto.GiftHistoryRequestDto
import com.example.demoproject.platform.data.network.dto.GiftReceivedListResponseDto
import com.example.demoproject.platform.data.network.dto.GiftSentListResponseDto
import com.example.demoproject.platform.data.network.dto.HomeInfoRequestDto
import com.example.demoproject.platform.data.network.dto.HomeInfoResponseDto
import com.example.demoproject.platform.data.network.dto.HomeListRequestDto
import com.example.demoproject.platform.data.network.dto.HomeMyResponseDto
import com.example.demoproject.platform.data.network.dto.NewVisitorRequestDto
import com.example.demoproject.platform.data.network.dto.NewVisitorSummaryDto
import com.example.demoproject.platform.data.network.dto.PrivacyUpdateRequestDto
import com.example.demoproject.platform.data.network.dto.ProfileChangePasswordRequestDto
import com.example.demoproject.platform.data.network.dto.ProfileEmailCodeRequestDto
import com.example.demoproject.platform.data.network.dto.ProfileEmailCodeResponseDto
import com.example.demoproject.platform.data.network.dto.ProfileUpdateEmailRequestDto
import com.example.demoproject.platform.data.network.dto.UserBlackRequestDto
import com.example.demoproject.platform.data.network.dto.UserListResponseDto
import com.example.demoproject.platform.data.network.dto.UserSearchRequestDto
import com.example.demoproject.platform.data.network.dto.UserSearchResponseDto
import com.example.demoproject.platform.data.network.dto.UserSyncPermissionRequestDto
import com.example.demoproject.platform.data.network.dto.VisitorListRequestDto
import com.example.demoproject.platform.network.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit bindings for profile, home, focus and album endpoints per the Spicy API doc.
 */
interface ProfileApi {

    /** `home/my` — the current user's profile home. */
    @POST("home/my")
    suspend fun getMyHome(): ApiResponse<HomeMyResponseDto>

    /** `home/info` — another user's public profile home (by external `user_id`). */
    @POST("home/info")
    suspend fun getHomeInfo(@Body body: HomeInfoRequestDto): ApiResponse<HomeInfoResponseDto>

    /** `user/info` — edit-profile initialization data. */
    @POST("user/info")
    suspend fun getEditProfileInfo(): ApiResponse<EditProfileInfoResponseDto>

    /** `me/new-visitor` — new visitor/follower summary for the Me page. */
    @POST("me/new-visitor")
    suspend fun getNewVisitorSummary(@Body body: NewVisitorRequestDto): ApiResponse<NewVisitorSummaryDto>

    /** `album/list` — photo/video album list by target uid. */
    @POST("album/list")
    suspend fun getAlbumList(@Body body: AlbumListRequestDto): ApiResponse<AlbumListResponseDto>

    /** `album/del` — delete own album media by ids. */
    @POST("album/del")
    suspend fun deleteAlbum(@Body body: AlbumDeleteRequestDto): ApiResponse<Unit?>

    /** `album/add` — add photos/videos into album. */
    @POST("album/add")
    suspend fun addAlbum(@Body body: AlbumAddRequestDto): ApiResponse<Unit?>

    /** `gift/send-list` — paged gifts sent by the current or target user. */
    @POST("gift/send-list")
    suspend fun getSentGifts(@Body body: GiftHistoryRequestDto): ApiResponse<GiftSentListResponseDto>

    /** `gift/receive-list` — paged gifts received by the current or target user. */
    @POST("gift/receive-list")
    suspend fun getReceivedGifts(@Body body: GiftHistoryRequestDto): ApiResponse<GiftReceivedListResponseDto>

    /** `user/update-info` — edits the current user's profile with partial `user_info`. */
    @POST("user/update-info")
    suspend fun updateEditProfile(@Body body: EditProfileUpdateRequestDto): ApiResponse<EditProfileUpdateResponseDto?>

    /** `home/list` — paged discover home users. */
    @POST("home/list")
    suspend fun discoverUsers(@Body body: HomeListRequestDto): ApiResponse<UserListResponseDto>

    /** `focus/follow` — follow (`flag=add`) or unfollow (`flag=del`) a target user. */
    @POST("focus/follow")
    suspend fun focusFollow(@Body body: FocusFollowRequestDto): ApiResponse<Unit?>

    /** `focus/list` — users the current or target account follows. */
    @POST("focus/list")
    suspend fun getFocusList(@Body body: FocusListRequestDto): ApiResponse<UserListResponseDto>

    /** `focus/fans-list` — users who follow the current or target account. */
    @POST("focus/fans-list")
    suspend fun getFocusFansList(@Body body: FocusListRequestDto): ApiResponse<UserListResponseDto>

    /** `user/search` — lookup a user by numeric UID. */
    @POST("user/search")
    suspend fun searchUser(@Body body: UserSearchRequestDto): ApiResponse<UserSearchResponseDto>

    /** `user/visitor` — paged visitors for the current account. */
    @POST("user/visitor")
    suspend fun getVisitorUsers(@Body body: VisitorListRequestDto): ApiResponse<UserListResponseDto>

    /** `user/black-list` — paged users blocked by the current account. */
    @POST("user/black-list")
    suspend fun getBlackList(@Body body: BlackListRequestDto): ApiResponse<UserListResponseDto>

    /** `user/black` — block or unblock a target user with `flag=add|del`. */
    @POST("user/black")
    suspend fun updateBlackUser(@Body body: UserBlackRequestDto): ApiResponse<Unit?>

    /** `privacy/update` — update a single privacy setting item. */
    @POST("privacy/update")
    suspend fun updatePrivacy(@Body body: PrivacyUpdateRequestDto): ApiResponse<Unit?>

    /** `profile/update-email-code` — send a verification code to a new email. */
    @POST("profile/update-email-code")
    suspend fun sendUpdateEmailCode(@Body body: ProfileEmailCodeRequestDto): ApiResponse<ProfileEmailCodeResponseDto?>

    /** `profile/update-email` — bind the current account email. */
    @POST("profile/update-email")
    suspend fun updateEmail(@Body body: ProfileUpdateEmailRequestDto): ApiResponse<Unit?>

    /** `profile/modify-email` — change the currently bound account email. */
    @POST("profile/modify-email")
    suspend fun modifyEmail(@Body body: ProfileUpdateEmailRequestDto): ApiResponse<ProfileEmailCodeResponseDto?>

    /** `profile/change-pwd-code` — send the authenticated password-change email code. */
    @POST("profile/change-pwd-code")
    suspend fun sendChangePasswordCode(@Body body: ProfileEmailCodeRequestDto): ApiResponse<Unit?>

    /** `profile/change-pwd` — change the email login password. */
    @POST("profile/change-pwd")
    suspend fun changePassword(@Body body: ProfileChangePasswordRequestDto): ApiResponse<Unit?>

    /** `user/sync-permission` — patch client permission switch state. */
    @POST("user/sync-permission")
    suspend fun syncPermission(@Body body: UserSyncPermissionRequestDto): ApiResponse<Unit?>

    /** `user/disband` — deregister and disband the current logged-in account. */
    @POST("user/disband")
    suspend fun disbandAccount(): ApiResponse<Unit?>
}
