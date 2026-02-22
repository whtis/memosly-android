package com.whtis.memosly.core.model

data class Webhook(
    val id: Int,
    val creatorId: Int,
    val name: String,
    val url: String,
    val createTime: String,
    val updateTime: String,
)
