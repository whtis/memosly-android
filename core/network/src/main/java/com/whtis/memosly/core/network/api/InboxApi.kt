package com.whtis.memosly.core.network.api

import com.whtis.memosly.core.network.dto.InboxMessageDto
import com.whtis.memosly.core.network.dto.ListInboxResponse
import com.whtis.memosly.core.network.dto.UpdateInboxRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface InboxApi {
    @GET("api/v1/inboxes")
    suspend fun listInbox(): ListInboxResponse

    @PATCH("api/v1/inboxes/{name}")
    suspend fun updateInbox(
        @Path("name") name: String,
        @Body request: UpdateInboxRequest,
    ): InboxMessageDto
}
