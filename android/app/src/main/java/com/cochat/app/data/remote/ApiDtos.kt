package com.cochat.app.data.remote

import com.cochat.app.data.model.*
import kotlinx.serialization.Serializable

@Serializable data class UserEnvelope(val user: User)
@Serializable data class UsersEnvelope(val users: List<User>)
@Serializable data class ChatsEnvelope(val chats: List<ChatSummary>)
@Serializable data class ChatEnvelope(val chat: ChatSummary)
@Serializable data class MessagesEnvelope(val messages: List<Message>)
@Serializable data class MessageEnvelope(val message: Message)
@Serializable data class GroupEnvelope(val group: GroupInfo)
@Serializable data class NotificationsEnvelope(val notifications: List<AppNotification>)

@Serializable
data class RegisterRequest(
    val fullName: String,
    val email: String? = null,
    val mobile: String? = null,
    val password: String,
    val designation: String? = null,
)

@Serializable data class LoginRequest(val identifier: String, val password: String)
@Serializable data class RefreshRequest(val refreshToken: String)
@Serializable data class UpdateProfileRequest(val fullName: String? = null, val designation: String? = null)
@Serializable data class OpenPrivateChatRequest(val userId: String)
@Serializable data class CreateGroupRequest(val name: String, val memberIds: List<String>)
@Serializable data class UpdateGroupNameRequest(val name: String)
@Serializable data class AddMembersRequest(val memberIds: List<String>)
