package com.cochat.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cochat.app.data.model.User
import com.cochat.app.data.repository.RealtimeStore
import com.cochat.app.data.repository.UsersRepository
import com.cochat.app.ui.common.Avatar
import com.cochat.app.util.mimeTypeOf
import com.cochat.app.util.uriToCacheFile
import kotlinx.coroutines.launch

@Composable
fun ProfileViewScreen(user: User, realtimeStore: RealtimeStore, onEdit: () -> Unit) {
    val presence by realtimeStore.presence.collectAsState()
    val status = presence[user.id] ?: user.status

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        Avatar(user.profilePicture, user.fullName, status, size = 96)
        Spacer(Modifier.height(12.dp))
        Text(user.fullName, style = MaterialTheme.typography.headlineSmall)
        Text(user.designation.ifBlank { "Team member" }, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        AssistChip(onClick = {}, label = { Text(status.replaceFirstChar { it.uppercase() }) })
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                DetailRow("Email", user.email ?: "—")
                HorizontalDivider()
                DetailRow("Mobile", user.mobile ?: "—")
                HorizontalDivider()
                DetailRow("Designation", user.designation.ifBlank { "—" })
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit profile") }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
    }
}

@Composable
fun ProfileEditScreen(
    user: User,
    usersRepository: UsersRepository,
    onSaved: (User) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf(user.fullName) }
    var designation by remember { mutableStateOf(user.designation) }
    var saving by remember { mutableStateOf(false) }
    var uploadingAvatar by remember { mutableStateOf(false) }
    var currentAvatar by remember { mutableStateOf(user.profilePicture) }
    var success by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploadingAvatar = true
        scope.launch {
            val file = uriToCacheFile(context, uri)
            if (file != null) {
                val updated = usersRepository.uploadAvatar(file, mimeTypeOf(context, uri))
                currentAvatar = updated.profilePicture
                onSaved(updated)
            }
            uploadingAvatar = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Edit profile") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
        )
        Column(Modifier.padding(24.dp)) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }
            success?.let { Text(it, color = Color(0xFF166534), modifier = Modifier.padding(bottom = 8.dp)) }

            Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.padding(bottom = 20.dp)) {
                Avatar(currentAvatar, fullName, size = 84)
                IconButton(
                    onClick = { avatarPicker.launch("image/*") },
                    modifier = Modifier.size(28.dp).clickable(enabled = !uploadingAvatar) { avatarPicker.launch("image/*") },
                ) { Icon(Icons.Default.Edit, contentDescription = "Change avatar") }
            }

            OutlinedTextField(fullName, { fullName = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(designation, { designation = it }, label = { Text("Designation") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(user.email ?: "—", {}, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = false)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(user.mobile ?: "—", {}, label = { Text("Mobile") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = false)
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (fullName.isBlank()) { error = "Full name is required."; return@Button }
                    saving = true
                    error = null
                    success = null
                    scope.launch {
                        try {
                            val updated = usersRepository.updateProfile(fullName.trim(), designation.trim())
                            onSaved(updated)
                            success = "Profile updated."
                        } catch (e: Exception) {
                            error = "Could not update profile."
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (saving) "Saving…" else "Save changes") }
        }
    }
}
