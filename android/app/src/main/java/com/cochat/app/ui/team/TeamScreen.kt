package com.cochat.app.ui.team

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cochat.app.data.model.User
import com.cochat.app.data.repository.ChatsRepository
import com.cochat.app.data.repository.RealtimeStore
import com.cochat.app.data.repository.UsersRepository
import com.cochat.app.ui.common.Avatar
import com.cochat.app.ui.common.EmptyState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TeamScreen(
    usersRepository: UsersRepository,
    chatsRepository: ChatsRepository,
    realtimeStore: RealtimeStore,
    onOpenChat: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var members by remember { mutableStateOf<List<User>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val presence by realtimeStore.presence.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        delay(250)
        loading = true
        members = usersRepository.list(query.trim())
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Team Members") })
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search by name, email, mobile, or designation…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            loading -> {}
            members.isEmpty() -> EmptyState("No team members match your search.")
            else -> LazyColumn {
                items(members, key = { it.id }) { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(member.profilePicture, member.fullName, presence[member.id] ?: member.status, size = 48)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(member.fullName, style = MaterialTheme.typography.bodyLarge)
                            Text(member.designation.ifBlank { "Team member" }, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            Text(member.email ?: member.mobile ?: "", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = {
                            scope.launch {
                                val chat = chatsRepository.openPrivate(member.id)
                                val current = realtimeStore.chats.value
                                if (current.none { it.id == chat.id }) realtimeStore.chats.value = listOf(chat) + current
                                onOpenChat(chat.id)
                            }
                        }) { Text("Message") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
