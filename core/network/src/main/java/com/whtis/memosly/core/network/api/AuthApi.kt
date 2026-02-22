package com.whtis.memosly.core.network.api

import com.whtis.memosly.core.network.dto.GetCurrentUserResponse
import com.whtis.memosly.core.network.dto.SessionResponse
import com.whtis.memosly.core.network.dto.SignInRequestBody
import com.whtis.memosly.core.network.dto.SignInResponseV026
import com.whtis.memosly.core.network.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {
    // --- v0.24 ---
    @POST("api/v1/auth/signin")
    suspend fun signInV024(
        @Query("passwordCredentials.username") username: String,
        @Query("passwordCredentials.password") password: String,
        @Query("neverExpire") neverExpire: Boolean = true,
    ): Response<UserDto>

    @POST("api/v1/auth/status")
    suspend fun getCurrentUserV024(): UserDto

    @POST("api/v1/auth/signout")
    suspend fun signOutV024()

    // --- v0.25 ---
    @POST("api/v1/auth/sessions")
    suspend fun signInV025(@Body body: SignInRequestBody): Response<SessionResponse>

    @GET("api/v1/auth/sessions/current")
    suspend fun getCurrentUserV025(): GetCurrentUserResponse

    @DELETE("api/v1/auth/sessions/current")
    suspend fun signOutV025()

    // --- v0.26 ---
    @POST("api/v1/auth/signin")
    suspend fun signInV026(@Body body: SignInRequestBody): Response<SignInResponseV026>

    @GET("api/v1/auth/me")
    suspend fun getCurrentUserV026(): GetCurrentUserResponse

    @POST("api/v1/auth/signout")
    suspend fun signOutV026()
}
