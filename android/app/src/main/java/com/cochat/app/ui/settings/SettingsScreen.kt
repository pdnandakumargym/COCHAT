package com.cochat.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cochat.app.data.model.User
import com.cochat.app.data.repository.AuthRepository
import com.cochat.app.ui.theme.DangerBg
import com.cochat.app.ui.theme.DangerRed
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(user: User?, authRepository: AuthRepository, onLoggedOut: () -> Unit) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })
        Column(Modifier.padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Notifications", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Push notifications", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Get notified of new messages while CoChat is in the background",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                        Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Account", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Signed in as", color = Color.Gray)
                        Text(user?.email ?: user?.mobile ?: "")
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { scope.launch { authRepository.logout(); onLoggedOut() } },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerBg, contentColor = DangerRed),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Log out") }
                }
            }
        }
    }
}
