package com.whtis.memosly.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ServerVersion {
    V024, V025, V026;

    companion object {
        fun fromString(value: String?): ServerVersion = when (value) {
            "V024" -> V024
            "V025" -> V025
            "V026" -> V026
            else -> V026
        }
    }
}

@Singleton
class TokenManager @Inject constructor(
    private val sessionPreferences: SessionPreferences,
) {
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

    private val _serverVersion = MutableStateFlow(ServerVersion.V026)
    val serverVersion: StateFlow<ServerVersion> = _serverVersion.asStateFlow()

    init {
        // Eagerly restore from persistent storage so API calls work
        // even after process death (when nav backstack skips AuthScreen)
        setServerUrl(sessionPreferences.serverUrl)
        _accessToken.value = sessionPreferences.accessToken
        _serverVersion.value = ServerVersion.fromString(sessionPreferences.serverVersion)
    }

    fun setAccessToken(token: String?) {
        _accessToken.value = token
    }

    fun setServerUrl(url: String?) {
        _serverUrl.value = url?.trimEnd('/')?.let { u ->
            if (u.isNotBlank() && !u.startsWith("http://") && !u.startsWith("https://")) {
                "https://$u"
            } else {
                u
            }
        }
    }

    fun setServerVersion(version: ServerVersion) {
        _serverVersion.value = version
    }

    fun isAuthenticated(): Boolean = _accessToken.value != null

    fun clear() {
        _accessToken.value = null
    }
}
