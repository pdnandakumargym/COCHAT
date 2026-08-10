package com.cochat.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CHATS = "chats"
    const val CHAT_THREAD = "chats/{chatId}"
    const val TEAM = "team"
    const val CREATE_GROUP = "groups/new"
    const val GROUP_INFO = "groups/{groupId}/info"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile/edit"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"

    fun chatThread(chatId: String) = "chats/$chatId"
    fun groupInfo(groupId: String) = "groups/$groupId/info"
}
