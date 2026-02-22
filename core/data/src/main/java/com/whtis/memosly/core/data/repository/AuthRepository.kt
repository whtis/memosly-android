package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.User
import com.whtis.memosly.core.network.ServerVersion
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val isAuthenticated: StateFlow<Boolean>
    val currentUser: StateFlow<User?>
    val serverUrl: String?
    suspend fun signIn(serverUrl: String, username: String, password: String, version: ServerVersion): User
    suspend fun signOut()
    suspend fun restoreSession(): Boolean
    suspend fun getCurrentUser(): User
}
