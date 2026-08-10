package com.cochat.app.data.remote

import com.cochat.app.data.model.AppNotification
import com.cochat.app.data.model.ChatSummary
import com.cochat.app.data.model.GroupInfo
import com.cochat.app.data.model.Message
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import org.json.JSONObject

sealed class SocketEvent {
    data class PresenceUpdate(val userId: String, val status: String) : SocketEvent()
    data class MessageNew(val message: Message, val chatId: String) : SocketEvent()
    data class ChatUpdated(val chat: ChatSummary) : SocketEvent()
    data class ChatRead(val chatId: String, val userId: String) : SocketEvent()
    data class GroupUpdated(val chat: GroupInfo, val action: String) : SocketEvent()
    data class TypingUpdate(val chatId: String, val userId: String, val isTyping: Boolean) : SocketEvent()
    data class NotificationNew(val notification: AppNotification) : SocketEvent()
}

class SocketManager {
    private var socket: Socket? = null
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val _events = MutableSharedFlow<SocketEvent>(0, 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    fun connect(token: String) {
        if (socket?.connected() == true) return
        val options = IO.Options.builder().setAuth(mapOf("token" to token)).build()
        val s = IO.socket(java.net.URI.create(NetworkConfig.SOCKET_URL), options)

        s.on("presence:update") { args ->
            val o = args[0] as JSONObject
            emit(SocketEvent.PresenceUpdate(o.getString("userId"), o.getString("status")))
        }
        s.on("message:new") { args ->
            val o = args[0] as JSONObject
            val message = json.decodeFromString<Message>(o.getJSONObject("message").toString())
            emit(SocketEvent.MessageNew(message, o.getString("chatId")))
        }
        s.on("chat:updated") { args ->
            val o = args[0] as JSONObject
            val chat = json.decodeFromString<ChatSummary>(o.getJSONObject("chat").toString())
            emit(SocketEvent.ChatUpdated(chat))
        }
        s.on("chat:read") { args ->
            val o = args[0] as JSONObject
            emit(SocketEvent.ChatRead(o.getString("chatId"), o.getString("userId")))
        }
        s.on("group:updated") { args ->
            val o = args[0] as JSONObject
            val chat = json.decodeFromString<GroupInfo>(o.getJSONObject("chat").toString())
            emit(SocketEvent.GroupUpdated(chat, o.getString("action")))
        }
        s.on("typing:update") { args ->
            val o = args[0] as JSONObject
            emit(SocketEvent.TypingUpdate(o.getString("chatId"), o.getString("userId"), o.getBoolean("isTyping")))
        }
        s.on("notification:new") { args ->
            val o = args[0] as JSONObject
            val notification = json.decodeFromString<AppNotification>(o.getJSONObject("notification").toString())
            emit(SocketEvent.NotificationNew(notification))
        }

        s.connect()
        socket = s
    }

    fun emitTypingStart(chatId: String) = socket?.emit("typing:start", JSONObject().put("chatId", chatId))
    fun emitTypingStop(chatId: String) = socket?.emit("typing:stop", JSONObject().put("chatId", chatId))
    fun emitActivity() = socket?.emit("presence:activity")

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    private fun emit(event: SocketEvent) {
        _events.tryEmit(event)
    }
}
