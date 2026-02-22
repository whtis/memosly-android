package com.whtis.memosly.core.network.api

import com.whtis.memosly.core.network.dto.CreateMemoRequest
import com.whtis.memosly.core.network.dto.ListMemoCommentsResponse
import com.whtis.memosly.core.network.dto.ListMemoReactionsResponse
import com.whtis.memosly.core.network.dto.ListMemosResponse
import com.whtis.memosly.core.network.dto.MemoDto
import com.whtis.memosly.core.network.dto.ReactionDto
import com.whtis.memosly.core.network.dto.UpsertReactionRequest
import com.whtis.memosly.core.network.dto.SetMemoAttachmentsRequest
import com.whtis.memosly.core.network.dto.SetMemoRelationsRequest
import com.whtis.memosly.core.network.dto.SetMemoResourcesRequest
import com.whtis.memosly.core.network.dto.MemoUpdateFields
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MemoApi {
    @GET("api/v1/memos")
    suspend fun listMemos(
        @Query("pageSize") pageSize: Int,
        @Query("pageToken") pageToken: String? = null,
        @Query("filter") filter: String? = null,
        @Query("state") state: String? = null,
    ): ListMemosResponse

    @POST("api/v1/memos")
    suspend fun createMemo(@Body request: CreateMemoRequest): MemoDto

    @GET("api/v1/memos/{id}")
    suspend fun getMemo(@Path("id") id: String): MemoDto

    @PATCH("api/v1/memos/{id}")
    suspend fun updateMemo(
        @Path("id") id: String,
        @Body memo: MemoUpdateFields,
        @Query("updateMask") updateMask: String,
    ): MemoDto

    @DELETE("api/v1/memos/{id}")
    suspend fun deleteMemo(@Path("id") id: String)

    @GET("api/v1/memos/{id}/comments")
    suspend fun listMemoComments(@Path("id") id: String): ListMemoCommentsResponse

    @POST("api/v1/memos/{id}/comments")
    suspend fun createMemoComment(
        @Path("id") id: String,
        @Body request: CreateMemoRequest,
    ): MemoDto

    @GET("api/v1/memos/{id}/reactions")
    suspend fun listMemoReactions(@Path("id") id: String): ListMemoReactionsResponse

    @POST("api/v1/memos/{id}/reactions")
    suspend fun upsertMemoReaction(
        @Path("id") id: String,
        @Body request: UpsertReactionRequest,
    ): ReactionDto

    // v0.24/v0.25: delete reaction by global id
    @DELETE("api/v1/reactions/{id}")
    suspend fun deleteReaction(@Path("id") id: Int)

    // v0.26: delete reaction scoped under memo
    @DELETE("api/v1/memos/{memoId}/reactions/{reactionId}")
    suspend fun deleteMemoReaction(
        @Path("memoId") memoId: String,
        @Path("reactionId") reactionId: Int,
    )

    @POST("api/v1/memos/{id}/relations")
    suspend fun setMemoRelations(
        @Path("id") id: String,
        @Body request: SetMemoRelationsRequest,
    )

    // v0.24: link resources to memo
    @POST("api/v1/memos/{id}/resources")
    suspend fun setMemoResources(
        @Path("id") id: String,
        @Body request: SetMemoResourcesRequest,
    )

    // v0.25+: link attachments to memo
    @PATCH("api/v1/memos/{uid}/attachments")
    suspend fun setMemoAttachments(
        @Path("uid") uid: String,
        @Body request: SetMemoAttachmentsRequest,
    )
}
