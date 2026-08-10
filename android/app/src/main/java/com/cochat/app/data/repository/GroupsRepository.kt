package com.cochat.app.data.repository

import com.cochat.app.data.model.GroupInfo
import com.cochat.app.data.remote.AddMembersRequest
import com.cochat.app.data.remote.ApiService
import com.cochat.app.data.remote.CreateGroupRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class GroupsRepository(private val api: ApiService) {

    suspend fun create(name: String, memberIds: List<String>): GroupInfo =
        api.createGroup(CreateGroupRequest(name, memberIds)).group

    suspend fun get(id: String): GroupInfo = api.getGroup(id).group

    suspend fun updateName(id: String, name: String): GroupInfo =
        api.updateGroupName(id, name.toRequestBody("text/plain".toMediaType())).group

    suspend fun updateAvatar(id: String, file: File, mimeType: String): GroupInfo {
        val body = file.asRequestBody(mimeType.toMediaType())
        val part = MultipartBody.Part.createFormData("avatar", file.name, body)
        return api.updateGroupAvatar(id, part).group
    }

    suspend fun addMembers(id: String, memberIds: List<String>): GroupInfo =
        api.addMembers(id, AddMembersRequest(memberIds)).group

    suspend fun removeMember(id: String, userId: String) {
        api.removeMember(id, userId)
    }

    suspend fun leave(id: String) {
        api.leaveGroup(id)
    }
}
