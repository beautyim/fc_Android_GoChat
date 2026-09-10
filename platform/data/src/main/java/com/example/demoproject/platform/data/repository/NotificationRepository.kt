package com.example.demoproject.platform.data.repository

import com.example.demoproject.platform.data.model.Notification
import com.example.demoproject.platform.network.result.AppResult

interface NotificationRepository {
    suspend fun getNotifications(page: Int): AppResult<List<Notification>>
    suspend fun markAllAsRead(): AppResult<Unit>
}
