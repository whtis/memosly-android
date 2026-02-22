package com.whtis.memosly.core.network.api

import com.whtis.memosly.core.network.dto.IdentityProviderDto
import com.whtis.memosly.core.network.dto.ListIdentityProvidersResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface IdentityProviderApi {
    @GET("api/v1/identityProviders")
    suspend fun listIdentityProviders(): ListIdentityProvidersResponse

    @POST("api/v1/identityProviders")
    suspend fun createIdentityProvider(@Body provider: IdentityProviderDto): IdentityProviderDto

    @PATCH("api/v1/identityProviders/{id}")
    suspend fun updateIdentityProvider(
        @Path("id") id: Int,
        @Body provider: IdentityProviderDto,
    ): IdentityProviderDto

    @DELETE("api/v1/identityProviders/{id}")
    suspend fun deleteIdentityProvider(@Path("id") id: Int)
}
