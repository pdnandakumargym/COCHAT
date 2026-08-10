package com.cochat.app.data.remote

import com.cochat.app.data.model.AuthResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ---- Auth ----
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest): Response<Unit>

    // ---- Users ----
    @GET("users/me")
    suspend fun me(): UserEnvelope

    @PATCH("users/me")
    suspend fun updateMe(@Body body: UpdateProfileRequest): UserEnvelope

    @Multipart
    @POST("users/me/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): UserEnvelope

    @GET("users")
    suspend fun listUsers(@Query("q") query: String = ""): UsersEnvelope

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserEnvelope

    // ---- Chats ----
    @GET("chats")
    suspend fun listChats(): ChatsEnvelope

    @POST("chats/private")
    suspend fun openPrivateChat(@Body body: OpenPrivateChatRequest): ChatEnvelope

    @GET("chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int = 30,
    ): MessagesEnvelope

    @Multipart
    @POST("chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?,
    ): MessageEnvelope

    @POST("chats/{chatId}/read")
    suspend fun markChatRead(@Path("chatId") chatId: String): Response<Unit>

    // ---- Groups ----
    @POST("groups")
    suspend fun createGroup(@Body body: CreateGroupRequest): GroupEnvelope

    @GET("groups/{id}")
    suspend fun getGroup(@Path("id") id: String): GroupEnvelope

    @Multipart
    @PATCH("groups/{id}")
    suspend fun updateGroupName(@Path("id") id: String, @Part("name") name: RequestBody): GroupEnvelope

    @Multipart
    @PATCH("groups/{id}")
    suspend fun updateGroupAvatar(@Path("id") id: String, @Part avatar: MultipartBody.Part): GroupEnvelope

    @POST("groups/{id}/members")
    suspend fun addMembers(@Path("id") id: String, @Body body: AddMembersRequest): GroupEnvelope

    @DELETE("groups/{id}/members/{userId}")
    suspend fun removeMember(@Path("id") id: String, @Path("userId") userId: String): Response<Unit>

    @POST("groups/{id}/leave")
    suspend fun leaveGroup(@Path("id") id: String): Response<Unit>

    // ---- Notifications ----
    @GET("notifications")
    suspend fun listNotifications(): NotificationsEnvelope

    @POST("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<Unit>

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Unit>
}
