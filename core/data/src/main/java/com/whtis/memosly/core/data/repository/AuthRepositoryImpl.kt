package com.whtis.memosly.core.data.repository

import android.util.Log
import com.whtis.memosly.core.model.User
import com.whtis.memosly.core.network.ServerVersion
import com.whtis.memosly.core.network.SessionPreferences
import com.whtis.memosly.core.network.TokenManager
import com.whtis.memosly.core.network.api.AuthApi
import com.whtis.memosly.core.network.dto.PasswordCredentials
import com.whtis.memosly.core.network.dto.SignInRequestBody
import com.whtis.memosly.core.network.toDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val sessionPreferences: SessionPreferences,
) : AuthRepository {

    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override val serverUrl: String?
        get() = tokenManager.serverUrl.value

    override suspend fun signIn(
        serverUrl: String,
        username: String,
        password: String,
        version: ServerVersion,
    ): User {
        tokenManager.setServerUrl(serverUrl)
        tokenManager.setServerVersion(version)
        sessionPreferences.serverVersion = version.name

        val user = when (version) {
            ServerVersion.V024 -> signInV024(username, password)
            ServerVersion.V025 -> signInV025(username, password)
            ServerVersion.V026 -> signInV026(username, password)
        }

        sessionPreferences.serverUrl = serverUrl
        _currentUser.value = user
        _isAuthenticated.value = true
        sessionPreferences.userId = user.id
        return user
    }

    private suspend fun signInV024(username: String, password: String): User {
        val response = authApi.signInV024(username = username, password = password)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "no body"
            throw Exception("Sign in failed: ${response.code()} ${response.message()} - $errorBody")
        }

        // v0.24: extract memos.access-token from grpc-metadata-set-cookie header
        val accessToken = extractCookieValue(response.headers(), "memos.access-token")
            ?: throw Exception("No access token in response")

        tokenManager.setAccessToken(accessToken)
        sessionPreferences.accessToken = accessToken

        val userDto = response.body() ?: throw Exception("Empty response body")
        return userDto.toDomain()
    }

    private suspend fun signInV025(username: String, password: String): User {
        val body = SignInRequestBody(
            passwordCredentials = PasswordCredentials(username, password),
        )
        val response = authApi.signInV025(body)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "no body"
            throw Exception("Sign in failed: ${response.code()} ${response.message()} - $errorBody")
        }

        // v0.25: extract user_session from grpc-metadata-set-cookie or set-cookie header
        val sessionToken = extractCookieValue(response.headers(), "user_session")
            ?: throw Exception("No user_session cookie in response")

        tokenManager.setAccessToken(sessionToken)
        sessionPreferences.accessToken = sessionToken

        val sessionResponse = response.body() ?: throw Exception("Empty response body")
        val userDto = sessionResponse.user ?: throw Exception("No user in response")
        return userDto.toDomain()
    }

    private suspend fun signInV026(username: String, password: String): User {
        val body = SignInRequestBody(
            passwordCredentials = PasswordCredentials(username, password),
        )
        val response = authApi.signInV026(body)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "no body"
            throw Exception("Sign in failed: ${response.code()} ${response.message()} - $errorBody")
        }

        val signInResponse = response.body() ?: throw Exception("Empty response body")

        // v0.26: token is in the response body
        val accessToken = signInResponse.accessToken
            ?: throw Exception("No access token in response")

        tokenManager.setAccessToken(accessToken)
        sessionPreferences.accessToken = accessToken

        val userDto = signInResponse.user ?: throw Exception("No user in response")
        return userDto.toDomain()
    }

    private fun extractCookieValue(headers: okhttp3.Headers, cookieName: String): String? {
        // Try grpc-metadata-set-cookie first, then set-cookie
        for (headerName in listOf("grpc-metadata-set-cookie", "set-cookie")) {
            val cookieHeader = headers[headerName] ?: continue
            val token = cookieHeader
                .split(";")
                .firstOrNull { it.trim().startsWith("$cookieName=") }
                ?.substringAfter("$cookieName=")
            if (token != null) return token
        }
        return null
    }

    override suspend fun signInWithAccessToken(
        serverUrl: String,
        accessToken: String,
        version: ServerVersion,
    ): User {
        tokenManager.setServerUrl(serverUrl)
        tokenManager.setServerVersion(version)
        sessionPreferences.serverVersion = version.name

        // Strip "Bearer " prefix if user pasted it
        val cleanToken = accessToken.trim().removePrefix("Bearer ").trim()
        tokenManager.setAccessToken(cleanToken)

        try {
            val user = getCurrentUser()
            // Token is valid — persist everything
            sessionPreferences.serverUrl = serverUrl
            sessionPreferences.accessToken = cleanToken
            sessionPreferences.userId = user.id
            _currentUser.value = user
            _isAuthenticated.value = true
            return user
        } catch (e: Exception) {
            // Token validation failed — clean up
            tokenManager.clear()
            throw Exception("Access token validation failed: ${e.message}")
        }
    }

    override suspend fun signOut() {
        try {
            when (tokenManager.serverVersion.value) {
                ServerVersion.V024 -> authApi.signOutV024()
                ServerVersion.V025 -> authApi.signOutV025()
                ServerVersion.V026 -> authApi.signOutV026()
            }
        } catch (_: Exception) {
            // Ignore signout API errors
        }
        tokenManager.clear()
        sessionPreferences.clear()
        _isAuthenticated.value = false
        _currentUser.value = null
    }

    override suspend fun restoreSession(): Boolean {
        val serverUrl = sessionPreferences.serverUrl ?: return false
        val accessToken = sessionPreferences.accessToken ?: return false
        val version = ServerVersion.fromString(sessionPreferences.serverVersion)

        tokenManager.setServerUrl(serverUrl)
        tokenManager.setAccessToken(accessToken)
        tokenManager.setServerVersion(version)

        return try {
            val user = getCurrentUser()
            _isAuthenticated.value = true
            _currentUser.value = user
            true
        } catch (e: HttpException) {
            if (e.code() in listOf(401, 403)) {
                // Token is genuinely invalid — clear everything
                Log.w(TAG, "restoreSession: auth failed (${e.code()}), clearing session")
                tokenManager.clear()
                sessionPreferences.clear()
                false
            } else {
                // Server error (500 etc.) — token might still be valid, let user proceed
                Log.w(TAG, "restoreSession: server error (${e.code()}), keeping token")
                _isAuthenticated.value = true
                true
            }
        } catch (e: Exception) {
            // Network error, timeout, etc. — token might still be valid, let user proceed
            Log.w(TAG, "restoreSession: network error, keeping token", e)
            _isAuthenticated.value = true
            true
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
    }

    override suspend fun getCurrentUser(): User {
        val userDto = when (tokenManager.serverVersion.value) {
            ServerVersion.V024 -> authApi.getCurrentUserV024()
            ServerVersion.V025 -> {
                val resp = authApi.getCurrentUserV025()
                resp.user ?: throw Exception("No user in response")
            }
            ServerVersion.V026 -> {
                val resp = authApi.getCurrentUserV026()
                resp.user ?: throw Exception("No user in response")
            }
        }
        val user = userDto.toDomain()
        _currentUser.value = user
        _isAuthenticated.value = true
        return user
    }
}
