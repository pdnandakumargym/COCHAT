package com.cochat.app.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cochat.app.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "cochat_session")

/**
 * Persists the JWT pair + last-known user profile. Reads/writes go through
 * DataStore (async, Flow-based) but a couple of call sites — notably the
 * OkHttp Authenticator, which runs on a background thread outside coroutine
 * scope — need a synchronous snapshot, hence the `runBlocking` helpers below.
 */
class TokenStore(private val context: Context) {
    private object Keys {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val USER = stringPreferencesKey("user_json")
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.ACCESS] }
    val userFlow: Flow<User?> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER]?.let { runCatching { Json.decodeFromString<User>(it) }.getOrNull() }
    }

    fun accessTokenBlocking(): String? = runBlocking { accessTokenFlow.first() }
    fun refreshTokenBlocking(): String? = runBlocking { context.dataStore.data.first()[Keys.REFRESH] }

    suspend fun save(accessToken: String, refreshToken: String, user: User) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS] = accessToken
            prefs[Keys.REFRESH] = refreshToken
            prefs[Keys.USER] = Json.encodeToString(User.serializer(), user)
        }
    }

    fun saveBlocking(accessToken: String, refreshToken: String, user: User) = runBlocking {
        save(accessToken, refreshToken, user)
    }

    suspend fun updateUser(user: User) {
        context.dataStore.edit { prefs -> prefs[Keys.USER] = Json.encodeToString(User.serializer(), user) }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    fun isLoggedInBlocking(): Boolean = accessTokenBlocking() != null
}
