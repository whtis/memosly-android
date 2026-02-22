package com.whtis.memosly.core.network.api

import com.whtis.memosly.core.network.dto.CreateWebhookRequest
import com.whtis.memosly.core.network.dto.ListWebhooksResponse
import com.whtis.memosly.core.network.dto.WebhookDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WebhookApi {
    // v0.24: top-level /webhooks with ?creator= query
    @GET("api/v1/webhooks")
    suspend fun listWebhooks(@Query("creator") creator: String): ListWebhooksResponse

    // v0.25 / v0.26: nested under user resource
    @GET("api/v1/users/{id}/webhooks")
    suspend fun listWebhooksV025(@Path("id") id: String): ListWebhooksResponse

    @POST("api/v1/webhooks")
    suspend fun createWebhook(@Body request: CreateWebhookRequest): WebhookDto

    @PATCH("api/v1/webhooks/{id}")
    suspend fun updateWebhook(
        @Path("id") id: Int,
        @Body request: CreateWebhookRequest,
    ): WebhookDto

    @DELETE("api/v1/webhooks/{id}")
    suspend fun deleteWebhook(@Path("id") id: Int)
}
