package com.cochat.app.data.repository

import com.cochat.app.data.model.AppNotification
import com.cochat.app.data.remote.ApiService

class NotificationsRepository(private val api: ApiService) {
    suspend fun list(): List<AppNotification> = api.listNotifications().notifications
    suspend fun markRead(id: String) { api.markNotificationRead(id) }
    suspend fun markAllRead() { api.markAllNotificationsRead() }
}
