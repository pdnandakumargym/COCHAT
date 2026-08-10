package com.cochat.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cochat.app.data.model.AppNotification
import com.cochat.app.data.repository.NotificationsRepository
import com.cochat.app.data.repository.RealtimeStore
import com.cochat.app.ui.common.Avatar
import com.cochat.app.ui.common.EmptyState
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(
    notificationsRepository: NotificationsRepository,
    realtimeStore: RealtimeStore,
    onOpenChat: (String) -> Unit,
) {
    val notifications by realtimeStore.notifications.collectAsState()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Notifications") },
            actions = {
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = {
                        scope.launch {
                            notificationsRepository.markAllRead()
                            realtimeStore.notifications.value = realtimeStore.notifications.value.map { it.copy(read = true) }
                        }
                    }) { Text("Mark all read") }
                }
            },
        )
        if (notifications.isEmpty()) {
            EmptyState("You're all caught up.")
        } else {
            LazyColumn {
                items(notifications, key = { it.id }) { notification ->
                    NotificationRow(notification) {
                        scope.launch {
                            if (!notification.read) {
                                notificationsRepository.markRead(notification.id)
                                realtimeStore.notifications.value = realtimeStore.notifications.value.map {
                                    if (it.id == notification.id) it.copy(read = true) else it
                                }
                            }
                            notification.chat?.let(onOpenChat)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (!notification.read) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val actor = notification.actor
        if (actor != null) {
            Avatar(actor.profilePicture, actor.fullName, size = 40)
        } else {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Text(iconFor(notification.type)) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(notification.title, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text(notification.body, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Text(notification.createdAt.take(16).replace('T', ' '), color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
        }
        if (!notification.read) {
            Box(
                modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape),
            )
        }
    }
}

private fun iconFor(type: String) = when (type) {
    "private_message" -> "💬"
    "group_message" -> "👥"
    "group_created" -> "🎉"
    "member_added" -> "➕"
    "member_removed" -> "➖"
    else -> "🔔"
}
