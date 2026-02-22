package com.whtis.memosly.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WebhookDto(
    val id: Int,
    val creatorId: Int,
    val name: String,
    val url: String,
    val createTime: String,
    val updateTime: String,
)

@JsonClass(generateAdapter = true)
data class CreateWebhookRequest(
    val name: String,
    val url: String,
)

@JsonClass(generateAdapter = true)
data class ListWebhooksResponse(
    val webhooks: List<WebhookDto>,
)
