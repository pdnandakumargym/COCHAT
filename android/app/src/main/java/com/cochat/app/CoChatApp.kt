package com.cochat.app

import android.app.Application
import com.cochat.app.data.remote.SocketManager
import com.cochat.app.data.remote.TokenStore
import com.cochat.app.data.remote.createApiService
import com.cochat.app.data.repository.AuthRepository
import com.cochat.app.data.repository.ChatsRepository
import com.cochat.app.data.repository.GroupsRepository
import com.cochat.app.data.repository.NotificationsRepository
import com.cochat.app.data.repository.RealtimeStore
import com.cochat.app.data.repository.UsersRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Simple hand-rolled service locator (no DI framework) so every screen can
 * reach the same singletons via `(application as CoChatApp)`.
 */
class CoChatApp : Application() {
    val appScope = CoroutineScope(SupervisorJob())

    lateinit var tokenStore: TokenStore
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var usersRepository: UsersRepository
        private set
    lateinit var chatsRepository: ChatsRepository
        private set
    lateinit var groupsRepository: GroupsRepository
        private set
    lateinit var notificationsRepository: NotificationsRepository
        private set
    lateinit var realtimeStore: RealtimeStore
        private set

    override fun onCreate() {
        super.onCreate()

        tokenStore = TokenStore(this)
        val api = createApiService(tokenStore)
        val socketManager = SocketManager()

        authRepository = AuthRepository(api, tokenStore)
        usersRepository = UsersRepository(api, tokenStore)
        chatsRepository = ChatsRepository(api)
        groupsRepository = GroupsRepository(api)
        notificationsRepository = NotificationsRepository(api)
        realtimeStore = RealtimeStore(socketManager, chatsRepository, notificationsRepository, tokenStore, appScope)
    }
}
