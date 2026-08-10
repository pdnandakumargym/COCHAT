package com.cochat.app.data.remote

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import java.util.concurrent.TimeUnit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

object NetworkConfig {
    // 10.0.2.2 is the Android emulator's alias for the host machine's localhost.
    // For a physical device on the same LAN, replace with the dev machine's IP
    // (and add it to network_security_config.xml, since it's plain HTTP).
    const val BASE_URL = "http://10.0.2.2:4000/api/"
    const val SOCKET_URL = "http://10.0.2.2:4000"
}

private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

/** Adds the current access token to every request except auth endpoints. */
private class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath.contains("/auth/")) return chain.proceed(request)
        val token = tokenStore.accessTokenBlocking() ?: return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
    }
}

/**
 * On a 401 from any non-auth endpoint, synchronously calls /auth/refresh
 * (via a bare OkHttpClient with no interceptor, to avoid recursion), persists
 * the new token pair, and retries the original request once.
 */
private class TokenAuthenticator(private val tokenStore: TokenStore) : Authenticator {
    private val plainClient = OkHttpClient()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains("/auth/")) return null
        if (responseCount(response) >= 2) return null // already retried once

        val refreshToken = tokenStore.refreshTokenBlocking() ?: return null
        return runBlocking {
            try {
                val bodyJson = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
                val req = Request.Builder()
                    .url(NetworkConfig.BASE_URL + "auth/refresh")
                    .post(bodyJson.toRequestBody("application/json".toMediaType()))
                    .build()
                val res = plainClient.newCall(req).execute()
                if (!res.isSuccessful) return@runBlocking null
                val text = res.body?.string() ?: return@runBlocking null
                val auth = json.decodeFromString<com.cochat.app.data.model.AuthResponse>(text)
                tokenStore.save(auth.accessToken, auth.refreshToken, auth.user)
                response.request.newBuilder().header("Authorization", "Bearer ${auth.accessToken}").build()
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}

fun createApiService(tokenStore: TokenStore): ApiService {
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
    val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .authenticator(TokenAuthenticator(tokenStore))
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val contentType = "application/json".toMediaType()
    val retrofit = Retrofit.Builder()
        .baseUrl(NetworkConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    return retrofit.create()
}
