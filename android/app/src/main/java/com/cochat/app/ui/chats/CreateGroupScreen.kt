package com.cochat.app.ui.chats

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
import androidx.compose.ui.unit.dp
import com.cochat.app.data.model.User
import com.cochat.app.data.repository.GroupsRepository
import com.cochat.app.data.repository.UsersRepository
import com.cochat.app.ui.common.Avatar
import kotlinx.coroutines.launch

@Composable
fun CreateGroupScreen(
    usersRepository: UsersRepository,
    groupsRepository: GroupsRepository,
    onCreated: (String) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var members by remember { mutableStateOf<List<User>>(emptyList()) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { members = usersRepository.list() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("New Group") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
        )
        Column(Modifier.padding(16.dp)) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Text("Add members (${selected.size} selected)", style = MaterialTheme.typography.labelMedium)
        }

        LazyColumn(Modifier.weight(1f)) {
            items(members, key = { it.id }) { member ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            selected = if (selected.contains(member.id)) selected - member.id else selected + member.id
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = selected.contains(member.id), onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Avatar(member.profilePicture, member.fullName, size = 38)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(member.fullName, style = MaterialTheme.typography.bodyMedium)
                        Text(member.designation.ifBlank { "Team member" }, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
            }
        }

        Button(
            onClick = {
                if (name.isBlank()) { error = "Give your group a name."; return@Button }
                if (selected.isEmpty()) { error = "Add at least one team member."; return@Button }
                saving = true
                error = null
                scope.launch {
                    try {
                        val group = groupsRepository.create(name.trim(), selected.toList())
                        onCreated(group.id)
                    } catch (e: Exception) {
                        error = "Could not create group."
                    } finally {
                        saving = false
                    }
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text(if (saving) "Creating…" else "Create group")
        }
    }
}
