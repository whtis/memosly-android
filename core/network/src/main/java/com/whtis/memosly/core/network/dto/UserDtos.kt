package com.whtis.memosly.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserDto(
    val name: String,
    val role: String,
    val username: String,
    val email: String = "",
    val nickname: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val description: String = "",
    val createTime: String = "",
    val updateTime: String = "",
)

@JsonClass(generateAdapter = true)
data class UpdateUserRequest(
    val nickname: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val description: String? = null,
    val password: String? = null,
)

@JsonClass(generateAdapter = true)
data class UserStatsDto(
    val memoDisplayTimestamps: List<String>?,
    val memoTypeStats: Map<String, Int>?,
    val tagCount: Map<String, Int>?,
)

@JsonClass(generateAdapter = true)
data class UserSettingDto(
    val locale: String?,
    val appearance: String?,
    val memoVisibility: String?,
)

@JsonClass(generateAdapter = true)
data class UpdateUserSettingRequest(
    val locale: String? = null,
    val appearance: String? = null,
    val memoVisibility: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateAccessTokenRequest(
    val description: String,
    val expiresAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class ListAccessTokensResponse(
    val accessTokens: List<AccessTokenDto>,
)

@JsonClass(generateAdapter = true)
data class AccessTokenDto(
    val accessToken: String,
    val description: String,
    val issuedAt: String,
    val expiresAt: String?,
)
