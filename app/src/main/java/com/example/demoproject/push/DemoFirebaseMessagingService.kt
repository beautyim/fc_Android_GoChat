package com.example.demoproject.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.demoproject.DemoApplication
import com.example.demoproject.platform.common.log.AppLogger
import com.example.demoproject.platform.data.local.pref.AppPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DemoFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            pushGateway().uploadFirebaseToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val payload = PushNotificationPayload.fromData(message.data)
        if (payload == null) {
            AppLogger.d(TAG, "push ignored reason=unknown_payload keys=${message.data.keys}")
            return
        }
        scope.launch {
            if (!payload.isEnabled()) {
                AppLogger.i(TAG, "push suppressed event=${payload.eventType}")
            } else if (PushForegroundState.isForeground) {
                AppLogger.i(TAG, "push suppressed reason=foreground event=${payload.eventType}")
            } else {
                PushNotificationPresenter(applicationContext).show(payload)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun pushGateway(): PushGateway =
        (application as? DemoApplication)?.pushGateway ?: PushClient(applicationContext)

    private suspend fun PushNotificationPayload.isEnabled(): Boolean {
        val prefs = AppPrefs.create(applicationContext)
        return when (this) {
            is PushNotificationPayload.NewMessage -> prefs.newMessagePushEnabled.first()
            is PushNotificationPayload.Like -> prefs.likesPushEnabled.first()
        }
    }

    private companion object {
        const val TAG = "FirebaseMessaging"
    }
}
