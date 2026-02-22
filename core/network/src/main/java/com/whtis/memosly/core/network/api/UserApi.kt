package com.whtis.memosly.core.network.api

import com.whtis.memosly.core.network.dto.CreateAccessTokenRequest
import com.whtis.memosly.core.network.dto.ListAccessTokensResponse
import com.whtis.memosly.core.network.dto.UpdateUserRequest
import com.whtis.memosly.core.network.dto.UserDto
import com.whtis.memosly.core.network.dto.UserStatsDto
import com.whtis.memosly.core.network.dto.UserSettingDto
import com.whtis.memosly.core.network.dto.UpdateUserSettingRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface UserApi {
    @GET("api/v1/users/{id}")
    suspend fun getUser(@Path("id") id: String): UserDto

    @PATCH("api/v1/users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body request: UpdateUserRequest,
    ): UserDto

    // v0.24: users/{id}/stats
    @GET("api/v1/users/{id}/stats")
    suspend fun getUserStats(@Path("id") id: String): UserStatsDto

    // v0.25 / v0.26: users/{id}:getStats
    @GET("api/v1/users/{id}:getStats")
    suspend fun getUserStatsV026(@Path("id") id: String): UserStatsDto

    @GET("api/v1/users/{id}/setting")
    suspend fun getUserSettings(@Path("id") id: String): UserSettingDto

    @PATCH("api/v1/users/{id}/setting")
    suspend fun updateUserSettings(
        @Path("id") id: String,
        @Body request: UpdateUserSettingRequest,
    ): UserSettingDto

    // v0.24: access_tokens (snake_case)
    @GET("api/v1/users/{id}/access_tokens")
    suspend fun listAccessTokens(@Path("id") id: String): ListAccessTokensResponse

    @POST("api/v1/users/{id}/access_tokens")
    suspend fun createAccessToken(
        @Path("id") id: String,
        @Body request: CreateAccessTokenRequest,
    )

    @DELETE("api/v1/users/{id}/access_tokens/{token}")
    suspend fun deleteAccessToken(
        @Path("id") id: String,
        @Path("token") token: String,
    )

    // v0.25: accessTokens (camelCase)
    @GET("api/v1/users/{id}/accessTokens")
    suspend fun listAccessTokensV025(@Path("id") id: String): ListAccessTokensResponse

    @POST("api/v1/users/{id}/accessTokens")
    suspend fun createAccessTokenV025(
        @Path("id") id: String,
        @Body request: CreateAccessTokenRequest,
    )

    @DELETE("api/v1/users/{id}/accessTokens/{token}")
    suspend fun deleteAccessTokenV025(
        @Path("id") id: String,
        @Path("token") token: String,
    )

    // v0.26: personalAccessTokens
    @GET("api/v1/users/{id}/personalAccessTokens")
    suspend fun listAccessTokensV026(@Path("id") id: String): ListAccessTokensResponse

    @POST("api/v1/users/{id}/personalAccessTokens")
    suspend fun createAccessTokenV026(
        @Path("id") id: String,
        @Body request: CreateAccessTokenRequest,
    )

    @DELETE("api/v1/users/{id}/personalAccessTokens/{token}")
    suspend fun deleteAccessTokenV026(
        @Path("id") id: String,
        @Path("token") token: String,
    )
}
