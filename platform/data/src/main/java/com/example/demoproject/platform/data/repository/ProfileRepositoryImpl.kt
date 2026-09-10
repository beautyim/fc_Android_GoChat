package com.example.demoproject.platform.data.repository

import android.util.Log
import com.example.demoproject.platform.data.local.cache.UserCache
import com.example.demoproject.platform.data.model.AlbumPhoto
import com.example.demoproject.platform.data.model.User
import com.example.demoproject.platform.data.network.dto.AlbumDeleteRequestDto
import com.example.demoproject.platform.data.network.dto.AlbumAddMediaDto
import com.example.demoproject.platform.data.network.dto.AlbumAddRequestDto
import com.example.demoproject.platform.data.network.dto.AlbumListRequestDto
import com.example.demoproject.platform.data.network.dto.AlbumMediaDto
import com.example.demoproject.platform.data.network.dto.EditProfileEmailDto
import com.example.demoproject.platform.data.network.dto.EditProfileTagDto
import com.example.demoproject.platform.data.network.dto.EditProfileUpdateRequestDto
import com.example.demoproject.platform.data.network.dto.EditProfileUpdateUserInfoDto
import com.example.demoproject.platform.data.network.dto.FocusFollowRequestDto
import com.example.demoproject.platform.data.network.dto.FocusListRequestDto
import com.example.demoproject.platform.data.network.dto.HomeInfoRequestDto
import com.example.demoproject.platform.data.network.dto.HomeListRequestDto
import com.example.demoproject.platform.data.network.toPicUrlOrNull
import com.example.demoproject.platform.data.network.toRelativeMediaPathOrNull
import com.example.demoproject.platform.data.network.api.ProfileApi
import com.example.demoproject.platform.data.network.dto.UserDto
import com.example.demoproject.platform.data.network.dto.UserSearchRequestDto
import com.example.demoproject.platform.data.network.mapper.toDomain
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.result.map
import com.example.demoproject.platform.network.result.onSuccessSuspend
import com.example.demoproject.platform.network.safeApiCall
import com.example.demoproject.platform.network.safeApiCallNullable
import com.example.demoproject.platform.network.safeApiCallUnit
import com.example.demoproject.platform.s3.MediaUploadService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi,
    private val userDao: UserCache,
    private val mediaUploadService: MediaUploadService,
) : ProfileRepository {

    override fun observeCachedUser(id: String): Flow<User?> =
        userDao.observeById(id)

    override suspend fun getCachedUser(id: String): User? =
        userDao.getById(id)

    override suspend fun getMyProfile(): AppResult<User> =
        safeApiCall { profileApi.getMyHome() }
            .map { result -> result.userInfo.toDomain() }
            .onSuccessSuspend { userDao.upsert(it) }

    override suspend fun getMyHomeDetail(): AppResult<ProfileHomeDetail> = coroutineScope {
        val homeDeferred = async {
            safeApiCall { profileApi.getMyHome() }
        }
        val albumDeferred = async {
            getEditProfileAlbumPage(lastTime = 0, pageCount = 20, mediaType = 0)
        }
        when (val home = homeDeferred.await()) {
            is AppResult.Failure -> home
            is AppResult.Success -> {
                val userInfo = home.data.userInfo
                val user = userInfo.toDomain()
                userDao.upsert(user)
                val media = when (val album = albumDeferred.await()) {
                    is AppResult.Success -> album.data.items
                    is AppResult.Failure -> emptyList()
                }
                AppResult.Success(
                    ProfileHomeDetail(
                        user = user,
                        backgroundUrl = userInfo.background.toPicUrlOrNull()
                            ?: user.avatar,
                        followingCount = userInfo.focusNum,
                        followerCount = userInfo.fansNum,
                        media = media,
                        isSelf = true,
                        coinBalance = home.data.accountInfo.money,
                    ),
                )
            }
        }
    }

    override suspend fun getUserHomeDetail(externalUserId: String): AppResult<ProfileHomeDetail> {
        val userId = externalUserId.trim()
        if (userId.isEmpty()) {
            return AppResult.BizError(
                code = AppResult.CODE_EMPTY_PAYLOAD,
                message = "Invalid user ID",
            )
        }
        return safeApiCall {
            profileApi.getHomeInfo(HomeInfoRequestDto(userId = userId))
        }.map { response ->
            val user = response.userInfo.toDomain()
            ProfileHomeDetail(
                user = user,
                backgroundUrl = response.userInfo.background.toPicUrlOrNull()
                    ?: user.avatar,
                followingCount = response.userInfo.focusNum,
                followerCount = response.userInfo.fansNum,
                media = response.album.list.mapNotNull(AlbumMediaDto::toAlbumPhoto),
                isSelf = false,
            )
        }.onSuccessSuspend { userDao.upsert(it.user) }
    }

    override suspend fun getMyPhotoUnlockCount(): AppResult<Int> =
        safeApiCall { profileApi.getMyHome() }
            .map { result -> result.userInfo.albumNum }

    override suspend fun getMyPrivateUnlockCounts(): AppResult<MyPrivateUnlockCounts> =
        AppResult.Success(MyPrivateUnlockCounts(photoCount = 0, videoCount = 0))

    override suspend fun getEditProfileData(): AppResult<EditProfileData> =
        safeApiCall { profileApi.getEditProfileInfo() }
            .map { response ->
                val user = response.userInfo.toDomain()
                EditProfileData(
                    user = user,
                    level = response.userInfo.level,
                    sex = response.userInfo.sex,
                    sign = response.userInfo.sign.orEmpty(),
                    background = response.userInfo.background.toPicUrlOrNull(),
                    speakLanguages = response.userInfo.speakLanguage
                        ?.split(',')
                        ?.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
                        .orEmpty(),
                    tags = response.tags.map(EditProfileTagDto::toDomain),
                )
            }
            .onSuccessSuspend { userDao.upsert(it.user) }

    override suspend fun updateEditProfile(update: EditProfileUpdate): AppResult<EditProfileUpdateResult> {
        val avatar = when {
            update.avatar == null -> null
            update.avatar.isBlank() -> null
            else -> when (val uploaded = mediaUploadService.uploadImageIfNeeded(update.avatar, flag = "avatar")) {
                is AppResult.Success -> uploaded.data.remotePath.toRelativeMediaPathOrNull()
                is AppResult.Failure -> return uploaded
            }
        }
        val background = when {
            update.background == null -> null
            update.background.isBlank() -> null
            else -> when (val uploaded = mediaUploadService.uploadImageIfNeeded(update.background, flag = "background")) {
                is AppResult.Success -> uploaded.data.remotePath.toRelativeMediaPathOrNull()
                is AppResult.Failure -> return uploaded
            }
        }
        val result = safeApiCallNullable {
            profileApi.updateEditProfile(
                EditProfileUpdateRequestDto(
                    userInfo = EditProfileUpdateUserInfoDto(
                        nickname = update.nickname,
                        sign = update.sign,
                        birthday = update.birthday,
                        sex = update.sex,
                        avatar = avatar,
                        background = background,
                        countryCode = update.countryCode,
                        email = update.email?.let { email ->
                            EditProfileEmailDto(email = email, emailCode = update.emailCode.orEmpty())
                        },
                        speakLanguage = update.speakLanguages?.joinToString(","),
                        userId = update.userId,
                        tags = update.tags,
                    ),
                ),
            )
        }
        return when (result) {
            is AppResult.Success -> AppResult.Success(
                EditProfileUpdateResult(
                    callbackType = result.data?.callback?.type,
                    callbackTitle = result.data?.callback?.title,
                    callbackMessage = result.data?.callback?.displayMessage,
                ),
            )
            is AppResult.Failure -> result
        }
    }

    override suspend fun updateProfile(
        nickname: String?,
        avatar: String?,
        gender: Int?,
        birthday: String?,
        bio: String?,
    ): AppResult<User> {
        val resolvedAvatar = when {
            avatar.isNullOrBlank() -> when (val currentAvatar = fetchCurrentAvatarForUpdate()) {
                is AppResult.Success -> currentAvatar.data
                is AppResult.Failure -> return currentAvatar
            }
            else -> when (val uploaded = mediaUploadService.uploadImageIfNeeded(avatar, flag = "avatar")) {
                is AppResult.Success -> uploaded.data.remotePath.normalizeAvatarForUpdate()
                is AppResult.Failure -> return uploaded
            }
        }

        val update = safeApiCallNullable {
            profileApi.updateEditProfile(
                EditProfileUpdateRequestDto(
                    userInfo = EditProfileUpdateUserInfoDto(
                        avatar = resolvedAvatar,
                        nickname = nickname,
                        birthday = birthday,
                        sex = gender,
                        sign = bio,
                    ),
                ),
            )
        }

        return when (update) {
            is AppResult.Success -> getMyProfile()
            is AppResult.BizError ->
                if (update.code == AppResult.CODE_EMPTY_PAYLOAD) getMyProfile()
                else update
            is AppResult.NetworkError,
            is AppResult.UnknownError -> update
        }
    }

    private suspend fun fetchCurrentAvatarForUpdate(): AppResult<String?> =
        safeApiCall { profileApi.getMyHome() }
            .map { result ->
                result.userInfo.avatar
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: result.userInfo.smallAvatar
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
            }
            .map { it.normalizeAvatarForUpdate() }

    private fun String?.normalizeAvatarForUpdate(): String? {
        return toRelativeMediaPathOrNull()
    }

    override suspend fun getUserProfile(id: String): AppResult<User> {
        val uid = id.toLongOrNull() ?: return AppResult.BizError(
            code = AppResult.CODE_EMPTY_PAYLOAD,
            message = "Invalid user ID",
        )
        return safeApiCall { profileApi.searchUser(UserSearchRequestDto(searchUid = uid)) }
            .map { result -> result.userInfo.toDomain() }
            .onSuccessSuspend { userDao.upsert(it) }
    }

    override suspend fun getAlbumPhotos(
        targetUserId: String,
        page: Int,
        pageCount: Int,
    ): AppResult<List<AlbumPhoto>> {
        val uid = targetUserId.toLongOrNull() ?: return AppResult.BizError(
            code = AppResult.CODE_EMPTY_PAYLOAD,
            message = "Invalid user ID",
        )
        Log.d(
            TAG,
            "getAlbumPhotos request targetUserId=$targetUserId page=$page pageCount=$pageCount",
        )
        return safeApiCall {
            profileApi.getAlbumList(
                AlbumListRequestDto(
                    pageCount = pageCount.coerceIn(1, 100),
                    targetUid = uid,
                    lastTime = page.coerceAtLeast(1),
                ),
            )
        }.map { response ->
            val rawList = response.list
            var nonImageCount = 0
            var emptyUrlCount = 0
            val photos = rawList.asSequence()
                .mapNotNull { media ->
                    if (media.mediaType != AlbumMediaDto.MEDIA_IMAGE) {
                        nonImageCount += 1
                        return@mapNotNull null
                    }
                    val thumbnailUrl = (media.smallUrl ?: media.url).toPicUrlOrNull()
                    val fullImageUrl = media.url.toPicUrlOrNull() ?: thumbnailUrl
                    if (thumbnailUrl.isNullOrBlank() && fullImageUrl.isNullOrBlank()) {
                        emptyUrlCount += 1
                        return@mapNotNull null
                    }
                    AlbumPhoto(
                        mediaId = media.mediaId,
                        mediaType = media.mediaType,
                        imageUrl = thumbnailUrl ?: fullImageUrl,
                        thumbnailUrl = thumbnailUrl ?: fullImageUrl,
                        fullImageUrl = fullImageUrl ?: thumbnailUrl,
                    )
                }
                .toList()
            Log.i(
                TAG,
                "getAlbumPhotos result targetUserId=$targetUserId raw=${rawList.size} mapped=${photos.size} nonImage=$nonImageCount emptyUrl=$emptyUrlCount",
            )
            photos
        }
    }

    override suspend fun getEditProfileAlbumPage(
        lastTime: Int,
        pageCount: Int,
        mediaType: Int,
    ): AppResult<AlbumMediaPage> =
        safeApiCall {
            profileApi.getAlbumList(
                AlbumListRequestDto(
                    pageCount = pageCount.coerceIn(1, 20),
                    lastTime = lastTime.coerceAtLeast(0),
                    mediaType = mediaType,
                ),
            )
        }.map { response ->
            AlbumMediaPage(
                items = response.list.mapNotNull(AlbumMediaDto::toAlbumPhoto),
                hasMore = response.hasMore == 1,
                lastTime = response.lastTime,
                totalCount = response.count,
            )
        }

    override suspend fun addAlbumPhotos(localUris: List<String>): AppResult<Unit> {
        if (localUris.isEmpty()) return AppResult.Success(Unit)
        val medias = mutableListOf<AlbumAddMediaDto>()
        for (uri in localUris) {
            when (val upload = mediaUploadService.uploadImageIfNeeded(uri, flag = "album")) {
                is AppResult.Success -> {
                    medias += AlbumAddMediaDto(
                        mediaType = AlbumMediaDto.MEDIA_IMAGE,
                        url = upload.data.remotePath,
                        width = upload.data.width,
                        height = upload.data.height,
                    )
                }
                is AppResult.Failure -> return upload
            }
        }
        return safeApiCallUnit {
            profileApi.addAlbum(AlbumAddRequestDto(albumList = medias))
        }
    }

    override suspend fun deleteAlbumPhotos(ids: List<Long>): AppResult<Unit> {
        if (ids.isEmpty()) return AppResult.Success(Unit)
        return safeApiCallUnit {
            profileApi.deleteAlbum(
                AlbumDeleteRequestDto(mediaIds = ids.distinct().filter { it > 0L }),
            )
        }
    }

    override suspend fun followUser(id: String): AppResult<Unit> {
        val uid = id.toLongOrNull() ?: return AppResult.BizError(
            code = AppResult.CODE_EMPTY_PAYLOAD,
            message = "Invalid user ID",
        )
        return safeApiCallUnit {
            profileApi.focusFollow(
                FocusFollowRequestDto(
                    targetUid = uid,
                    flag = FocusFollowRequestDto.FLAG_ADD,
                ),
            )
        }
    }

    override suspend fun unfollowUser(id: String): AppResult<Unit> {
        val uid = id.toLongOrNull() ?: return AppResult.BizError(
            code = AppResult.CODE_EMPTY_PAYLOAD,
            message = "Invalid user ID",
        )
        return safeApiCallUnit {
            profileApi.focusFollow(
                FocusFollowRequestDto(
                    targetUid = uid,
                    flag = FocusFollowRequestDto.FLAG_DELETE,
                ),
            )
        }
    }

    override suspend fun getFollowingUsers(
        targetUserId: String,
        page: Int,
        pageSize: Int,
    ): AppResult<List<User>> {
        val uid = targetUserId.toLongOrNull() ?: 0L
        return safeApiCall {
            profileApi.getFocusList(
                FocusListRequestDto(
                    page = page,
                    targetUid = uid.takeIf { it > 0L },
                ),
            )
        }.map { dto -> dto.list.map(UserDto::toDomain) }
            .onSuccessSuspend { users -> userDao.upsertAll(users.map { it }) }
    }

    override suspend fun getFollowerUsers(
        targetUserId: String,
        page: Int,
        pageSize: Int,
    ): AppResult<List<User>> {
        val uid = targetUserId.toLongOrNull() ?: 0L
        return safeApiCall {
            profileApi.getFocusFansList(
                FocusListRequestDto(
                    page = page,
                    targetUid = uid.takeIf { it > 0L },
                ),
            )
        }.map { dto -> dto.list.map(UserDto::toDomain) }
            .onSuccessSuspend { users -> userDao.upsertAll(users.map { it }) }
    }

    override suspend fun getFollowerCount(targetUserId: String): AppResult<Int> {
        val uid = targetUserId.toLongOrNull() ?: 0L
        return safeApiCall {
            profileApi.getFocusFansList(
                FocusListRequestDto(
                    page = 1,
                    targetUid = uid.takeIf { it > 0L },
                ),
            )
        }.map { dto -> dto.resolvedTotal }
    }

    override suspend fun discoverUsers(
        page: Int,
        pageSize: Int,
        flag: Int,
        language: String?,
    ): AppResult<DiscoverUsersPage> =
        safeApiCall {
            profileApi.discoverUsers(
                HomeListRequestDto(
                    flag = flag,
                    page = page.coerceAtLeast(1),
                    filterList = languageFilter(language),
                ),
            )
        }
            .map { dto ->
                DiscoverUsersPage(
                    users = dto.list.map(UserDto::toDomain),
                    hasMore = dto.hasMore,
                )
            }
            .onSuccessSuspend { pageData -> userDao.upsertAll(pageData.users.map { it }) }
}

private fun languageFilter(language: String?): kotlinx.serialization.json.JsonElement {
    val normalized = language?.trim()?.takeIf { it.isNotEmpty() } ?: return kotlinx.serialization.json.JsonObject(emptyMap())
    return kotlinx.serialization.json.buildJsonObject {
        put(
            "language",
            kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive(normalized))),
        )
    }
}

private const val TAG = "BerryCam/ProfileRepo"

private fun EditProfileTagDto.toDomain(): EditProfileTag =
    EditProfileTag(
        id = id,
        name = name,
    )

private fun AlbumMediaDto.toAlbumPhoto(): AlbumPhoto? {
    val thumbnail = smallPhotoUrl.toPicUrlOrNull()
        ?: smallUrl.toPicUrlOrNull()
        ?: coverUrl.toPicUrlOrNull()
        ?: url.toPicUrlOrNull()
    val full = url.toPicUrlOrNull() ?: thumbnail
    if (thumbnail.isNullOrBlank() && full.isNullOrBlank()) return null
    return AlbumPhoto(
        mediaId = mediaId,
        mediaType = mediaType,
        imageUrl = thumbnail ?: full,
        thumbnailUrl = thumbnail ?: full,
        fullImageUrl = full ?: thumbnail,
        coverUrl = coverUrl.toPicUrlOrNull() ?: thumbnail,
        width = width,
        height = height,
        duration = duration,
        status = status,
        addedAtSeconds = addTime,
        content = content.orEmpty(),
    )
}
