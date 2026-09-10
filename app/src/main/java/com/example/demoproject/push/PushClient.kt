package com.example.demoproject.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.local.pref.AppPrefs
import com.example.demoproject.platform.data.network.NetworkRuntime
import com.example.demoproject.platform.data.network.dto.UserSyncPermissionRequestDto
import com.example.demoproject.platform.network.result.AppResult
import com.example.demoproject.platform.network.safeApiCallUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

interface PushGateway {
    val newMessagePushEnabled: Flow<Boolean>
    val likesPushEnabled: Flow<Boolean>
    suspend fun setPushEventEnabled(event: PushEventPreference, enabled: Boolean): AppResult<Unit>
    suspend fun uploadCurrentFirebaseToken(): AppResult<Unit>
    suspend fun uploadFirebaseToken(token: String): AppResult<Unit>
}

class PushClient(
    context: Context,
    private val appPrefs: AppPrefs = AppPrefs.create(context),
) : PushGateway {
    private val appContext = context.applicationContext

    override val newMessagePushEnabled: Flow<Boolean> = appPrefs.newMessagePushEnabled
    override val likesPushEnabled: Flow<Boolean> = appPrefs.likesPushEnabled

    override suspend fun setPushEventEnabled(event: PushEventPreference, enabled: Boolean): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            val nextNewMessageEnabled = if (event == PushEventPreference.NewMessage) {
                enabled
            } else {
                appPrefs.newMessagePushEnabled.first()
            }
            val nextLikesEnabled = if (event == PushEventPreference.Likes) {
                enabled
            } else {
                appPrefs.likesPushEnabled.first()
            }
            val runtime = NetworkRuntime.get(appContext)
            val syncResult = safeApiCallUnit {
                runtime.profileApi.syncPermission(
                    UserSyncPermissionRequestDto(
                        notice = if (nextNewMessageEnabled || nextLikesEnabled) 1 else 0,
                    ),
                )
            }
            if (syncResult is AppResult.Failure) {
                return@withContext syncResult
            }
            runCatching {
                when (event) {
                    PushEventPreference.NewMessage -> appPrefs.setNewMessagePushEnabled(enabled)
                    PushEventPreference.Likes -> appPrefs.setLikesPushEnabled(enabled)
                }
            }.fold(
                onSuccess = { AppResult.Success(Unit) },
                onFailure = { AppResult.UnknownError(cause = it) },
            )
        }

    override suspend fun uploadCurrentFirebaseToken(): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            val token = fetchFirebaseToken()
            if (token.isNullOrBlank()) {
                AppLogger.w(TAG, "FCM token unavailable")
                return@withContext AppResult.BizError(AppResult.CODE_EMPTY_PAYLOAD, "FCM token unavailable")
            }
            uploadFirebaseToken(token)
        }

    override suspend fun uploadFirebaseToken(token: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            NetworkRuntime.get(appContext).pushTokenRepository.uploadFirebaseToken(token)
        }

    private suspend fun fetchFirebaseToken(): String? =
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (cont.isActive) cont.resume(token)
                }
                .addOnFailureListener {
                    AppLogger.w(TAG, "fetch FCM token failed: ${it.message}")
                    if (cont.isActive) cont.resume(null)
                }
        }

    private companion object {
        const val TAG = "PushClient"
    }
}
