package com.cochat.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cochat.app.ui.auth.LoginScreen
import com.cochat.app.ui.auth.RegisterScreen
import com.cochat.app.ui.chats.ChatListScreen
import com.cochat.app.ui.chats.ChatThreadScreen
import com.cochat.app.ui.chats.CreateGroupScreen
import com.cochat.app.ui.chats.GroupInfoScreen
import com.cochat.app.ui.navigation.Routes
import com.cochat.app.ui.notifications.NotificationsScreen
import com.cochat.app.ui.profile.ProfileEditScreen
import com.cochat.app.ui.profile.ProfileViewScreen
import com.cochat.app.ui.settings.SettingsScreen
import com.cochat.app.ui.team.TeamScreen
import com.cochat.app.ui.theme.CoChatTheme

private val TOP_LEVEL_ROUTES = setOf(Routes.CHATS, Routes.TEAM, Routes.NOTIFICATIONS, Routes.SETTINGS)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CoChatApp
        setContent {
            CoChatTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CoChatApp(app)
                }
            }
        }
    }
}

@Composable
fun CoChatApp(app: CoChatApp) {
    val navController = rememberNavController()
    val user by app.authRepository.currentUser.collectAsState(initial = null)
    val context = LocalContext.current

    LaunchedEffect(user) {
        if (user != null) app.realtimeStore.start()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in TOP_LEVEL_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomBar) BottomBar(navController, currentRoute, app.realtimeStore)
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (app.authRepository.isLoggedIn) Routes.CHATS else Routes.LOGIN,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    authRepository = app.authRepository,
                    onLoggedIn = { navController.navigate(Routes.CHATS) { popUpTo(0) } },
                    onGoToRegister = { navController.navigate(Routes.REGISTER) },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    authRepository = app.authRepository,
                    onRegistered = { navController.navigate(Routes.CHATS) { popUpTo(0) } },
                    onGoToLogin = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable(Routes.CHATS) {
                ChatListScreen(
                    realtimeStore = app.realtimeStore,
                    onOpenChat = { chatId -> navController.navigate(Routes.chatThread(chatId)) },
                    onNewChat = { navController.navigate(Routes.TEAM) },
                    onNewGroup = { navController.navigate(Routes.CREATE_GROUP) },
                )
            }
            composable(Routes.CHAT_THREAD) { backStack ->
                val chatId = backStack.arguments?.getString("chatId") ?: return@composable
                ChatThreadScreen(
                    chatId = chatId,
                    chatsRepository = app.chatsRepository,
                    realtimeStore = app.realtimeStore,
                    currentUserId = user?.id ?: "",
                    onBack = { navController.popBackStack() },
                    onOpenGroupInfo = { navController.navigate(Routes.groupInfo(chatId)) },
                    onOpenAttachment = { url ->
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    },
                )
            }
            composable(Routes.CREATE_GROUP) {
                CreateGroupScreen(
                    usersRepository = app.usersRepository,
                    groupsRepository = app.groupsRepository,
                    onCreated = { groupId ->
                        navController.navigate(Routes.chatThread(groupId)) { popUpTo(Routes.CHATS) }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.GROUP_INFO) { backStack ->
                val groupId = backStack.arguments?.getString("groupId") ?: return@composable
                GroupInfoScreen(
                    groupId = groupId,
                    currentUserId = user?.id ?: "",
                    groupsRepository = app.groupsRepository,
                    usersRepository = app.usersRepository,
                    realtimeStore = app.realtimeStore,
                    onBack = { navController.popBackStack() },
                    onLeft = { navController.navigate(Routes.CHATS) { popUpTo(Routes.CHATS) { inclusive = true } } },
                )
            }
            composable(Routes.TEAM) {
                TeamScreen(
                    usersRepository = app.usersRepository,
                    chatsRepository = app.chatsRepository,
                    realtimeStore = app.realtimeStore,
                    onOpenChat = { chatId -> navController.navigate(Routes.chatThread(chatId)) },
                )
            }
            composable(Routes.PROFILE) {
                user?.let { u ->
                    ProfileViewScreen(user = u, realtimeStore = app.realtimeStore, onEdit = { navController.navigate(Routes.PROFILE_EDIT) })
                }
            }
            composable(Routes.PROFILE_EDIT) {
                user?.let { u ->
                    ProfileEditScreen(
                        user = u,
                        usersRepository = app.usersRepository,
                        onSaved = {},
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    notificationsRepository = app.notificationsRepository,
                    realtimeStore = app.realtimeStore,
                    onOpenChat = { chatId -> navController.navigate(Routes.chatThread(chatId)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    user = user,
                    authRepository = app.authRepository,
                    onLoggedOut = {
                        app.realtimeStore.stop()
                        navController.navigate(Routes.LOGIN) { popUpTo(0) }
                    },
                )
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?, realtimeStore: com.cochat.app.data.repository.RealtimeStore) {
    val chats by realtimeStore.chats.collectAsState()
    val notifications by realtimeStore.notifications.collectAsState()
    val unreadChats = chats.sumOf { it.unreadCount }
    val unreadNotifications = notifications.count { !it.read }

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.CHATS,
            onClick = { navController.navigate(Routes.CHATS) { popUpTo(Routes.CHATS) { inclusive = true } } },
            icon = { BadgedBox(badge = { if (unreadChats > 0) Badge { Text(unreadChats.toString()) } }) { Icon(Icons.Default.Chat, contentDescription = "Chats") } },
            label = { Text("Chats") },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.TEAM,
            onClick = { navController.navigate(Routes.TEAM) { popUpTo(Routes.CHATS) } },
            icon = { Icon(Icons.Default.Groups, contentDescription = "Team") },
            label = { Text("Team") },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.NOTIFICATIONS,
            onClick = { navController.navigate(Routes.NOTIFICATIONS) { popUpTo(Routes.CHATS) } },
            icon = { BadgedBox(badge = { if (unreadNotifications > 0) Badge { Text(unreadNotifications.toString()) } }) { Icon(Icons.Default.Notifications, contentDescription = "Notifications") } },
            label = { Text("Alerts") },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.SETTINGS,
            onClick = { navController.navigate(Routes.SETTINGS) { popUpTo(Routes.CHATS) } },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
        )
    }
}
