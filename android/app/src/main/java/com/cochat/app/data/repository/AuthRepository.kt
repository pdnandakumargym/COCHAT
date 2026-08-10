package com.cochat.app.data.repository

import com.cochat.app.data.model.User
import com.cochat.app.data.remote.ApiService
import com.cochat.app.data.remote.LoginRequest
import com.cochat.app.data.remote.RefreshRequest
import com.cochat.app.data.remote.RegisterRequest
import com.cochat.app.data.remote.TokenStore
import kotlinx.coroutines.flow.Flow

class AuthRepository(private val api: ApiService, private val tokenStore: TokenStore) {

    val currentUser: Flow<User?> = tokenStore.userFlow
    val isLoggedIn: Boolean get() = tokenStore.isLoggedInBlocking()

    suspend fun register(fullName: String, email: String?, mobile: String?, password: String, designation: String?) {
        val res = api.register(RegisterRequest(fullName, email, mobile, password, designation))
        tokenStore.save(res.accessToken, res.refreshToken, res.user)
    }

    suspend fun login(identifier: String, password: String) {
        val res = api.login(LoginRequest(identifier, password))
        tokenStore.save(res.accessToken, res.refreshToken, res.user)
    }

    suspend fun logout() {
        val refreshToken = tokenStore.refreshTokenBlocking()
        if (refreshToken != null) {
            runCatching { api.logout(RefreshRequest(refreshToken)) }
        }
        tokenStore.clear()
    }
}
