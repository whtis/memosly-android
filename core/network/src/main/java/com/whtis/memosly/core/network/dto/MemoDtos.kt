package com.whtis.memosly.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ListMemosResponse(
    val memos: List<MemoDto>,
    val nextPageToken: String?,
)

@JsonClass(generateAdapter = true)
data class MemoDto(
    val name: String,
    val uid: String = "",
    val creator: String = "",
    val createTime: String = "",
    val updateTime: String = "",
    val displayTime: String = "",
    val content: String = "",
    val visibility: String = "PRIVATE",
    val state: String = "NORMAL",
    val pinned: Boolean = false,
    val resources: List<ResourceDto>? = null,
    val attachments: List<ResourceDto>? = null,
    val relations: List<MemoRelationDto>? = null,
    val reactions: List<ReactionDto>? = null,
    val tags: List<String>? = null,
    val snippet: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateMemoRequest(
    val content: String,
    val visibility: String? = null,
)

@JsonClass(generateAdapter = true)
data class MemoUpdateFields(
    val content: String? = null,
    val visibility: String? = null,
    val state: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateCommentRequest(
    val comment: CreateMemoRequest,
)

@JsonClass(generateAdapter = true)
data class RelatedMemoInfoDto(
    val name: String = "",
    val uid: String = "",
    val snippet: String = "",
)

// Custom adapter: MemoRelationAdapter handles both v0.24 (string) and v0.26 (object) formats
data class MemoRelationDto(
    val memo: RelatedMemoInfoDto = RelatedMemoInfoDto(),
    val relatedMemo: RelatedMemoInfoDto = RelatedMemoInfoDto(),
    val type: String = "",
)

@JsonClass(generateAdapter = true)
data class ReactionDto(
    val id: Int = 0,
    val name: String = "",
    val creator: String = "",
    val contentId: String = "",
    val reactionType: String = "",
)

@JsonClass(generateAdapter = true)
data class UpsertReactionRequest(
    val reaction: ReactionFields,
)

@JsonClass(generateAdapter = true)
data class ReactionFields(
    val contentId: String,
    val reactionType: String,
)

@JsonClass(generateAdapter = true)
data class SetMemoRelationsRequest(
    val relations: List<MemoRelationDto>,
)

/** Minimal reference with only `name` — avoids sending empty fields that break gRPC-gateway parsing */
@JsonClass(generateAdapter = true)
data class ResourceRef(val name: String)

@JsonClass(generateAdapter = true)
data class SetMemoResourcesRequest(
    val resources: List<ResourceRef>,
)

@JsonClass(generateAdapter = true)
data class SetMemoAttachmentsRequest(
    val attachments: List<ResourceRef>,
)

@JsonClass(generateAdapter = true)
data class ListMemoCommentsResponse(
    val memos: List<MemoDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ListMemoReactionsResponse(
    val reactions: List<ReactionDto> = emptyList(),
)
