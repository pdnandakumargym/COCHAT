package com.cochat.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val fullName: String,
    val email: String? = null,
    val mobile: String? = null,
    val designation: String = "",
    val profilePicture: String = "",
    val status: String = "offline",
    val lastSeen: String? = null,
)

@Serializable
data class AuthResponse(
    val user: User,
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class Attachment(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
)

@Serializable
data class MessageSender(
    @SerialName("_id") val id: String,
    val fullName: String,
    val profilePicture: String = "",
)

@Serializable
data class Message(
    @SerialName("_id") val id: String,
    val chat: String,
    val sender: MessageSender,
    val type: String,
    val text: String = "",
    val attachment: Attachment? = null,
    val systemEvent: String? = null,
    val createdAt: String,
)

@Serializable
data class LastMessage(
    val text: String,
    val senderId: String,
    val type: String,
    val createdAt: String,
)

@Serializable
data class ChatPeer(
    val id: String,
    val fullName: String,
    val profilePicture: String = "",
    val designation: String = "",
    val status: String = "offline",
)

@Serializable
data class ChatSummary(
    val id: String,
    val type: String,
    val lastMessage: LastMessage? = null,
    val unreadCount: Int = 0,
    val updatedAt: String,
    val name: String? = null,
    val avatar: String? = null,
    val memberCount: Int? = null,
    val peer: ChatPeer? = null,
) {
    val displayName: String get() = if (type == "group") name ?: "Group" else peer?.fullName ?: "Unknown"
    val displayAvatar: String get() = if (type == "group") avatar ?: "" else peer?.profilePicture ?: ""
}

@Serializable
data class GroupMember(
    val id: String,
    val role: String,
    val fullName: String,
    val profilePicture: String = "",
    val designation: String = "",
    val status: String = "offline",
)

@Serializable
data class GroupInfo(
    val id: String,
    val type: String = "group",
    val name: String,
    val avatar: String = "",
    val createdBy: String? = null,
    val createdAt: String? = null,
    val members: List<GroupMember> = emptyList(),
)

@Serializable
data class NotificationActor(
    @SerialName("_id") val id: String,
    val fullName: String,
    val profilePicture: String = "",
)

@Serializable
data class AppNotification(
    @SerialName("_id") val id: String,
    val user: String,
    val type: String,
    val title: String,
    val body: String = "",
    val chat: String? = null,
    val actor: NotificationActor? = null,
    val read: Boolean = false,
    val createdAt: String,
)
