package com.whtis.memosly.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResourceDto(
    val name: String = "",
    val uid: String = "",
    val createTime: String = "",
    val filename: String = "",
    val type: String = "",
    val size: String = "0",
    val memo: String? = null,
    val externalLink: String? = null,
)

@JsonClass(generateAdapter = true)
data class ListResourcesResponse(
    val resources: List<ResourceDto> = emptyList(),
    val attachments: List<ResourceDto> = emptyList(),
) {
    /** Unified accessor: v0.24 uses `resources`, v0.25+ uses `attachments` */
    fun all(): List<ResourceDto> = resources + attachments
}

@JsonClass(generateAdapter = true)
data class CreateResourceRequest(
    val filename: String,
    val type: String,
    val content: String, // base64-encoded bytes
)
