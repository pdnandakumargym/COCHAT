package com.cochat.app.data.repository

import com.cochat.app.data.model.User
import com.cochat.app.data.remote.ApiService
import com.cochat.app.data.remote.TokenStore
import com.cochat.app.data.remote.UpdateProfileRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class UsersRepository(private val api: ApiService, private val tokenStore: TokenStore) {

    suspend fun me(): User = api.me().user

    suspend fun updateProfile(fullName: String?, designation: String?): User {
        val user = api.updateMe(UpdateProfileRequest(fullName, designation)).user
        tokenStore.updateUser(user)
        return user
    }

    suspend fun uploadAvatar(file: File, mimeType: String): User {
        val body = file.asRequestBody(mimeType.toMediaType())
        val part = MultipartBody.Part.createFormData("avatar", file.name, body)
        val user = api.uploadAvatar(part).user
        tokenStore.updateUser(user)
        return user
    }

    suspend fun list(query: String = ""): List<User> = api.listUsers(query).users

    suspend fun get(id: String): User = api.getUser(id).user
}
