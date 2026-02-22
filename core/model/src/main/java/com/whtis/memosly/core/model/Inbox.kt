package com.whtis.memosly.core.model

data class InboxMessage(
    val name: String,
    val sender: String,
    val receiver: String,
    val status: InboxStatus,
    val createTime: String,
    val type: InboxType,
    val activityId: Int?,
)

enum class InboxStatus {
    UNREAD,
    READ,
    ARCHIVED,
    UNKNOWN,
}

enum class InboxType {
    MEMO_COMMENT,
    VERSION_UPDATE,
    UNKNOWN,
}
