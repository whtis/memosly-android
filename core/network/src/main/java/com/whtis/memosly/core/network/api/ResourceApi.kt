package com.whtis.memosly.core.network.api

import com.whtis.memosly.core.network.dto.CreateResourceRequest
import com.whtis.memosly.core.network.dto.ListResourcesResponse
import com.whtis.memosly.core.network.dto.ResourceDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ResourceApi {
    // v0.24: resources
    @GET("api/v1/resources")
    suspend fun listResources(): ListResourcesResponse

    @POST("api/v1/resources")
    suspend fun createResource(@Body request: CreateResourceRequest): ResourceDto

    @DELETE("api/v1/resources/{name}")
    suspend fun deleteResource(@Path("name") name: String)

    // v0.25 / v0.26: attachments
    @GET("api/v1/attachments")
    suspend fun listResourcesV026(): ListResourcesResponse

    @POST("api/v1/attachments")
    suspend fun createResourceV026(@Body request: CreateResourceRequest): ResourceDto

    @DELETE("api/v1/attachments/{name}")
    suspend fun deleteResourceV026(@Path("name") name: String)
}
