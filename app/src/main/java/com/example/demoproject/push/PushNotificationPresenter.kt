package com.example.demoproject.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.demoproject.MainActivity
import com.example.demoproject.R
import kotlin.math.absoluteValue

class PushNotificationPresenter(
    private val context: Context,
) {

    fun show(payload: PushNotificationPayload) {
        if (!canNotify()) return
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(payload.title())
            .setContentText(payload.body())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(payload.pendingIntent())
            .build()
        NotificationManagerCompat.from(context).notify(payload.notificationId(), notification)
    }

    private fun canNotify(): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_MESSAGES,
            context.getString(R.string.push_channel_messages),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.push_channel_messages_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun PushNotificationPayload.title(): String = when (this) {
        is PushNotificationPayload.NewMessage -> context.getString(R.string.push_new_message_title)
        is PushNotificationPayload.Like -> context.getString(R.string.push_like_title)
    }

    private fun PushNotificationPayload.body(): String = when (this) {
        is PushNotificationPayload.NewMessage -> context.getString(R.string.push_new_message_body)
        is PushNotificationPayload.Like -> context.getString(R.string.push_like_body)
    }

    private fun PushNotificationPayload.pendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(PushNotificationPayload.EXTRA_PUSH_TYPE, eventType)
        when (this) {
            is PushNotificationPayload.NewMessage -> {
                intent.putExtra(PushNotificationPayload.EXTRA_CONVERSATION_ID, conversationId)
                intent.putExtra(PushNotificationPayload.EXTRA_EXTERNAL_USER_ID, externalUserId)
            }
            is PushNotificationPayload.Like -> {
                senderId?.let { intent.putExtra(PushNotificationPayload.EXTRA_SENDER_ID, it) }
            }
        }
        return PendingIntent.getActivity(
            context,
            notificationId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun PushNotificationPayload.notificationId(): Int =
        when (this) {
            is PushNotificationPayload.NewMessage -> "message:$conversationId:$messageId".hashCode().absoluteValue
            is PushNotificationPayload.Like -> "like:$senderId:$eventId".hashCode().absoluteValue
        }

    private companion object {
        const val CHANNEL_MESSAGES = "berrycam_messages"
    }
}
