package com.whtis.memosly.core.network.api

import com.whtis.memosly.core.network.dto.ListTagsResponse
import com.whtis.memosly.core.network.dto.TagDto
import com.whtis.memosly.core.network.dto.UpsertTagRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TagApi {
    @GET("api/v1/tags")
    suspend fun listTags(): ListTagsResponse

    @POST("api/v1/tags")
    suspend fun upsertTag(@Body request: UpsertTagRequest): TagDto

    @DELETE("api/v1/tags/{tag}")
    suspend fun deleteTag(@Path("tag") tag: String)
}
