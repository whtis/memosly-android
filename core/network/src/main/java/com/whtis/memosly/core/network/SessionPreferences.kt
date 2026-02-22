package com.whtis.memosly.core.network

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("memos_session", Context.MODE_PRIVATE)
    }

    var serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var userId: Int
        get() = prefs.getInt(KEY_USER_ID, 0)
        set(value) = prefs.edit().putInt(KEY_USER_ID, value).apply()

    private val _navModeFlow = MutableStateFlow(
        prefs.getString(KEY_NAV_MODE, NAV_MODE_DRAWER) ?: NAV_MODE_DRAWER
    )
    val navModeFlow: StateFlow<String> = _navModeFlow.asStateFlow()

    var navMode: String
        get() = _navModeFlow.value
        set(value) {
            prefs.edit().putString(KEY_NAV_MODE, value).apply()
            _navModeFlow.value = value
        }

    var serverVersion: String?
        get() = prefs.getString(KEY_SERVER_VERSION, null)
        set(value) = prefs.edit().putString(KEY_SERVER_VERSION, value).apply()

    fun clear() {
        val savedNavMode = navMode
        val savedServerVersion = serverVersion
        val savedServerUrl = serverUrl
        prefs.edit().clear().apply()
        // Restore UI preferences and server URL after clear (for login pre-fill)
        prefs.edit()
            .putString(KEY_NAV_MODE, savedNavMode)
            .putString(KEY_SERVER_VERSION, savedServerVersion)
            .putString(KEY_SERVER_URL, savedServerUrl)
            .apply()
        _navModeFlow.value = savedNavMode
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAV_MODE = "nav_mode"
        private const val KEY_SERVER_VERSION = "server_version"
        const val NAV_MODE_DRAWER = "drawer"
        const val NAV_MODE_TABS = "tabs"
    }
}
