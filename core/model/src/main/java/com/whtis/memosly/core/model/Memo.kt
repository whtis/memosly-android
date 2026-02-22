package com.whtis.memosly.core.model

data class Memo(
    val name: String,
    val uid: String,
    val creator: String,
    val createTime: String,
    val updateTime: String,
    val displayTime: String,
    val content: String,
    val visibility: Visibility,
    val state: MemoState,
    val pinned: Boolean,
    val resources: List<Resource>,
    val relations: List<MemoRelation>,
    val reactions: List<Reaction>,
    val tags: List<String>,
    val snippet: String,
) {
    val isArchived: Boolean get() = state == MemoState.ARCHIVED
}

enum class Visibility {
    PRIVATE,
    PROTECTED,
    PUBLIC,
    UNKNOWN,
}

enum class MemoState {
    NORMAL,
    ARCHIVED,
    UNKNOWN,
}
