package com.cochat.app.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cochat.app.data.model.GroupInfo
import com.cochat.app.data.model.User
import com.cochat.app.data.repository.GroupsRepository
import com.cochat.app.data.repository.RealtimeStore
import com.cochat.app.data.repository.UsersRepository
import com.cochat.app.ui.common.Avatar
import com.cochat.app.ui.common.LoadingState
import com.cochat.app.ui.theme.DangerBg
import com.cochat.app.ui.theme.DangerRed
import kotlinx.coroutines.launch

@Composable
fun GroupInfoScreen(
    groupId: String,
    currentUserId: String,
    groupsRepository: GroupsRepository,
    usersRepository: UsersRepository,
    realtimeStore: RealtimeStore,
    onBack: () -> Unit,
    onLeft: () -> Unit,
) {
    var group by remember { mutableStateOf<GroupInfo?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showAddMembers by remember { mutableStateOf(false) }
    var candidates by remember { mutableStateOf<List<User>>(emptyList()) }
    val presence by realtimeStore.presence.collectAsState()
    val lastGroupUpdate by realtimeStore.lastGroupUpdate.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(groupId) {
        group = groupsRepository.get(groupId)
        loading = false
    }

    LaunchedEffect(lastGroupUpdate) {
        val (updatedChat, _) = lastGroupUpdate ?: return@LaunchedEffect
        if (updatedChat.id == groupId) group = updatedChat
    }

    val isAdmin = group?.members?.any { it.id == currentUserId && it.role == "admin" } == true

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Group Info") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
        )

        if (loading || group == null) {
            LoadingState()
            return@Column
        }
        val g = group!!

        LazyColumn(Modifier.weight(1f)) {
            item {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(g.avatar, g.name, size = 84)
                    Spacer(Modifier.height(10.dp))
                    Text(g.name, style = MaterialTheme.typography.titleLarge)
                    Text("${g.members.size} members", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Members", style = MaterialTheme.typography.titleMedium)
                    if (isAdmin) {
                        TextButton(onClick = {
                            showAddMembers = true
                            scope.launch {
                                val all = usersRepository.list()
                                val existing = g.members.map { it.id }.toSet()
                                candidates = all.filter { it.id !in existing }
                            }
                        }) { Text("+ Add members") }
                    }
                }
            }

            items(g.members, key = { it.id }) { member ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(member.profilePicture, member.fullName, presence[member.id] ?: member.status, size = 40)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(member.fullName + if (member.id == currentUserId) " (You)" else "", style = MaterialTheme.typography.bodyMedium)
                        Text(member.designation.ifBlank { "Team member" }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    if (member.role == "admin") {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                            Text("Admin", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    } else if (isAdmin) {
                        TextButton(onClick = {
                            scope.launch {
                                groupsRepository.removeMember(groupId, member.id)
                                group = group?.copy(members = group!!.members.filter { it.id != member.id })
                            }
                        }) { Text("Remove", color = DangerRed) }
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { scope.launch { groupsRepository.leave(groupId); onLeft() } },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerBg, contentColor = DangerRed),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) { Text("Leave group") }
                Spacer(Modifier.height(24.dp))
            }
        }

        if (showAddMembers) {
            AlertDialog(
                onDismissRequest = { showAddMembers = false },
                title = { Text("Add members") },
                text = {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(candidates, key = { it.id }) { candidate ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            group = groupsRepository.addMembers(groupId, listOf(candidate.id))
                                            candidates = candidates.filter { it.id != candidate.id }
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Avatar(candidate.profilePicture, candidate.fullName, size = 32)
                                Spacer(Modifier.width(10.dp))
                                Text(candidate.fullName, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showAddMembers = false }) { Text("Done") } },
            )
        }
    }
}
