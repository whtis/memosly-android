package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.User
import com.whtis.memosly.core.model.UserAccessToken
import com.whtis.memosly.core.model.UserStats
import com.whtis.memosly.core.model.Webhook
import com.whtis.memosly.core.network.ServerVersion
import com.whtis.memosly.core.network.TokenManager
import com.whtis.memosly.core.network.api.UserApi
import com.whtis.memosly.core.network.api.WebhookApi
import com.whtis.memosly.core.network.dto.CreateAccessTokenRequest
import com.whtis.memosly.core.network.dto.CreateWebhookRequest
import com.whtis.memosly.core.network.dto.UpdateUserRequest
import com.whtis.memosly.core.network.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val webhookApi: WebhookApi,
    private val tokenManager: TokenManager,
) : UserRepository {

    private val version: ServerVersion
        get() = tokenManager.serverVersion.value

    override suspend fun getUser(id: Int): User =
        userApi.getUser(id.toString()).toDomain()

    override suspend fun updateUser(
        id: Int,
        nickname: String?,
        email: String?,
        avatarUrl: String?,
        description: String?,
    ): User = userApi.updateUser(
        id.toString(),
        UpdateUserRequest(
            nickname = nickname,
            email = email,
            avatarUrl = avatarUrl,
            description = description,
        ),
    ).toDomain()

    override suspend fun getUserStats(id: Int): UserStats =
        when (version) {
            ServerVersion.V024 -> userApi.getUserStats(id.toString())
            // v0.25 + v0.26 both use :getStats
            else -> userApi.getUserStatsV026(id.toString())
        }.toDomain()

    override suspend fun listAccessTokens(id: Int): List<UserAccessToken> =
        when (version) {
            ServerVersion.V024 -> userApi.listAccessTokens(id.toString())
            ServerVersion.V025 -> userApi.listAccessTokensV025(id.toString())
            ServerVersion.V026 -> userApi.listAccessTokensV026(id.toString())
        }.accessTokens.map { it.toDomain() }

    override suspend fun createAccessToken(id: Int, description: String, expiresAt: String?) {
        val req = CreateAccessTokenRequest(description, expiresAt)
        when (version) {
            ServerVersion.V024 -> userApi.createAccessToken(id.toString(), req)
            ServerVersion.V025 -> userApi.createAccessTokenV025(id.toString(), req)
            ServerVersion.V026 -> userApi.createAccessTokenV026(id.toString(), req)
        }
    }

    override suspend fun deleteAccessToken(id: Int, token: String) =
        when (version) {
            ServerVersion.V024 -> userApi.deleteAccessToken(id.toString(), token)
            ServerVersion.V025 -> userApi.deleteAccessTokenV025(id.toString(), token)
            ServerVersion.V026 -> userApi.deleteAccessTokenV026(id.toString(), token)
        }

    override suspend fun listWebhooks(creatorId: Int): List<Webhook> =
        when (version) {
            ServerVersion.V024 -> webhookApi.listWebhooks("users/$creatorId")
            // v0.25 + v0.26: nested under user resource
            else -> webhookApi.listWebhooksV025(creatorId.toString())
        }.webhooks.map { it.toDomain() }

    override suspend fun createWebhook(name: String, url: String): Webhook =
        webhookApi.createWebhook(CreateWebhookRequest(name, url)).toDomain()

    override suspend fun deleteWebhook(id: Int) =
        webhookApi.deleteWebhook(id)
}
