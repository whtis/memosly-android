package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.User
import com.whtis.memosly.core.model.UserAccessToken
import com.whtis.memosly.core.model.UserStats
import com.whtis.memosly.core.model.Webhook

interface UserRepository {
    suspend fun getUser(id: Int): User
    suspend fun updateUser(id: Int, nickname: String?, email: String?, avatarUrl: String?, description: String?): User
    suspend fun getUserStats(id: Int): UserStats
    suspend fun listAccessTokens(id: Int): List<UserAccessToken>
    suspend fun createAccessToken(id: Int, description: String, expiresAt: String?): Unit
    suspend fun deleteAccessToken(id: Int, token: String)
    suspend fun listWebhooks(creatorId: Int): List<Webhook>
    suspend fun createWebhook(name: String, url: String): Webhook
    suspend fun deleteWebhook(id: Int)
}
