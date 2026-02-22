package com.whtis.memosly.core.model

data class MemoRelation(
    val memo: String,
    val relatedMemo: String,
    val type: MemoRelationType,
)

enum class MemoRelationType {
    REFERENCE,
    COMMENT,
    UNKNOWN,
}
