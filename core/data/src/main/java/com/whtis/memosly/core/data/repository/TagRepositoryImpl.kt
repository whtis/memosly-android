package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.Tag
import com.whtis.memosly.core.network.ServerVersion
import com.whtis.memosly.core.network.TokenManager
import com.whtis.memosly.core.network.api.UserApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : TagRepository {

    private val version: ServerVersion
        get() = tokenManager.serverVersion.value

    override suspend fun listTags(): List<Tag> {
        val user = authRepository.currentUser.value
            ?: throw IllegalStateException("Not authenticated")
        val userId = user.id.toString()
        val stats = when (version) {
            ServerVersion.V024 -> userApi.getUserStats(userId)
            // v0.25 + v0.26 both use :getStats
            else -> userApi.getUserStatsV026(userId)
        }
        return stats.tagCount?.entries?.map { (name, _) ->
            Tag(name = name, creator = user.name)
        } ?: emptyList()
    }

    override suspend fun upsertTag(name: String): Tag {
        // Tags are managed through memo content in Memos v0.24+
        val user = authRepository.currentUser.value
            ?: throw IllegalStateException("Not authenticated")
        return Tag(name = name, creator = user.name)
    }

    override suspend fun deleteTag(tag: String) {
        // Global tag deletion not supported in Memos v0.24+
        // Tags are removed by editing memo content
    }
}
