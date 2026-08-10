package com.cochat.app.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cochat.app.data.model.ChatSummary
import com.cochat.app.data.repository.RealtimeStore
import com.cochat.app.ui.common.Avatar
import com.cochat.app.ui.common.EmptyState

@Composable
fun ChatListScreen(
    realtimeStore: RealtimeStore,
    onOpenChat: (String) -> Unit,
    onNewChat: () -> Unit,
    onNewGroup: () -> Unit,
) {
    val chats by realtimeStore.chats.collectAsState()
    val presence by realtimeStore.presence.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Chats") },
            actions = {
                IconButton(onClick = onNewChat) { Icon(Icons.Default.PersonAdd, contentDescription = "New chat") }
                IconButton(onClick = onNewGroup) { Icon(Icons.Default.Group, contentDescription = "New group") }
            },
        )
        if (chats.isEmpty()) {
            EmptyState("No conversations yet. Tap + to start chatting.")
        } else {
            LazyColumn {
                items(chats, key = { it.id }) { chat ->
                    ChatRow(chat, presence[chat.peer?.id]) { onOpenChat(chat.id) }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: ChatSummary, livePeerStatus: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val status = if (chat.type == "private") livePeerStatus ?: chat.peer?.status else null
        Avatar(chat.displayAvatar, chat.displayName, status, size = 48)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(chat.displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    previewText(chat),
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (chat.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(chat.unreadCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun previewText(chat: ChatSummary): String {
    val lm = chat.lastMessage ?: return "No messages yet"
    return if (lm.type == "text") lm.text else "Sent a ${lm.type}"
}
