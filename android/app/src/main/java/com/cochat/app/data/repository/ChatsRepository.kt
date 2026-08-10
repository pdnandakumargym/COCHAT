package com.cochat.app.data.repository

import com.cochat.app.data.model.ChatSummary
import com.cochat.app.data.model.Message
import com.cochat.app.data.remote.ApiService
import com.cochat.app.data.remote.OpenPrivateChatRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ChatsRepository(private val api: ApiService) {

    suspend fun list(): List<ChatSummary> = api.listChats().chats

    suspend fun openPrivate(userId: String): ChatSummary = api.openPrivateChat(OpenPrivateChatRequest(userId)).chat

    suspend fun messages(chatId: String, before: String? = null): List<Message> =
        api.getMessages(chatId, before).messages

    suspend fun sendMessage(chatId: String, text: String?, file: File?, mimeType: String?): Message {
        val parts = mutableMapOf<String, okhttp3.RequestBody>()
        if (!text.isNullOrBlank()) {
            parts["text"] = text.toRequestBody("text/plain".toMediaType())
        }
        val filePart = file?.let {
            val body = it.asRequestBody((mimeType ?: "application/octet-stream").toMediaType())
            MultipartBody.Part.createFormData("file", it.name, body)
        }
        return api.sendMessage(chatId, parts, filePart).message
    }

    suspend fun markRead(chatId: String) {
        api.markChatRead(chatId)
    }
}
