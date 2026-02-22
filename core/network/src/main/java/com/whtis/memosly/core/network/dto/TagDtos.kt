package com.whtis.memosly.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TagDto(
    val name: String,
    val creator: String,
)

@JsonClass(generateAdapter = true)
data class UpsertTagRequest(
    val name: String,
)

@JsonClass(generateAdapter = true)
data class ListTagsResponse(
    val tags: List<TagDto>,
)
