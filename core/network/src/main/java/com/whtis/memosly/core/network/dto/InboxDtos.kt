package com.whtis.memosly.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InboxMessageDto(
    val name: String,
    val sender: String,
    val receiver: String,
    val status: String,
    val createTime: String,
    val type: String?,
    val activityId: Int?,
)

@JsonClass(generateAdapter = true)
data class ListInboxResponse(
    val inboxes: List<InboxMessageDto>,
)

@JsonClass(generateAdapter = true)
data class UpdateInboxRequest(
    val status: String,
)
