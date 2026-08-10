package com.cochat.app.data.repository

import com.cochat.app.data.model.AppNotification
import com.cochat.app.data.model.ChatSummary
import com.cochat.app.data.model.GroupInfo
import com.cochat.app.data.model.Message
import com.cochat.app.data.remote.SocketEvent
import com.cochat.app.data.remote.SocketManager
import com.cochat.app.data.remote.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * App-wide realtime state: one socket connection, fanned out into StateFlows
 * that every screen reads from. Central instance lives in CoChatApp so it
 * survives navigation/config changes for as long as the process does.
 */
class RealtimeStore(
    private val socketManager: SocketManager,
    private val chatsRepo: ChatsRepository,
    private val notificationsRepo: NotificationsRepository,
    private val tokenStore: TokenStore,
    private val scope: CoroutineScope,
) {
    val chats = MutableStateFlow<List<ChatSummary>>(emptyList())
    val presence = MutableStateFlow<Map<String, String>>(emptyMap())
    val notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val typing = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val lastIncomingMessage = MutableStateFlow<Pair<String, Message>?>(null)
    val lastGroupUpdate = MutableStateFlow<Pair<GroupInfo, String>?>(null)

    private var started = false

    suspend fun start() {
        if (started) return
        started = true

        val token = tokenStore.accessTokenBlocking() ?: return

        // UNDISPATCHED runs synchronously up to the first suspension point, so
        // `collect` has already subscribed before `connect()` runs below —
        // otherwise the server's own presence:update (sent the instant this
        // socket connects) can arrive before anything is listening for it.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            socketManager.events.collect { handle(it) }
        }
        socketManager.connect(token)

        val fetchedChats = chatsRepo.list()
        chats.value = fetchedChats
        notifications.value = notificationsRepo.list()

        val snapshot = mutableMapOf<String, String>()
        for (chat in fetchedChats) chat.peer?.let { snapshot[it.id] = it.status }
        // merge under whatever presence:update events already arrived live
        presence.value = snapshot + presence.value
    }

    private fun handle(event: SocketEvent) {
        when (event) {
            is SocketEvent.PresenceUpdate -> {
                presence.value = presence.value + (event.userId to event.status)
            }
            is SocketEvent.MessageNew -> {
                lastIncomingMessage.value = event.chatId to event.message
            }
            is SocketEvent.ChatUpdated -> {
                val list = chats.value.toMutableList()
                val idx = list.indexOfFirst { it.id == event.chat.id }
                if (idx >= 0) list[idx] = event.chat else list.add(0, event.chat)
                chats.value = list.sortedByDescending { it.updatedAt }
            }
            is SocketEvent.GroupUpdated -> {
                lastGroupUpdate.value = event.chat to event.action
                if (event.action == "created") {
                    scope.launch { chats.value = chatsRepo.list() }
                }
            }
            is SocketEvent.NotificationNew -> {
                notifications.value = listOf(event.notification) + notifications.value
            }
            is SocketEvent.TypingUpdate -> {
                val map = typing.value.toMutableMap()
                val set = (map[event.chatId] ?: emptySet()).toMutableSet()
                if (event.isTyping) set.add(event.userId) else set.remove(event.userId)
                map[event.chatId] = set
                typing.value = map
            }
            is SocketEvent.ChatRead -> Unit
        }
    }

    fun emitTypingStart(chatId: String) = socketManager.emitTypingStart(chatId)
    fun emitTypingStop(chatId: String) = socketManager.emitTypingStop(chatId)

    fun markChatReadLocally(chatId: String) {
        chats.value = chats.value.map { if (it.id == chatId) it.copy(unreadCount = 0) else it }
    }

    fun stop() {
        socketManager.disconnect()
        started = false
        chats.value = emptyList()
        presence.value = emptyMap()
        notifications.value = emptyList()
    }
}
