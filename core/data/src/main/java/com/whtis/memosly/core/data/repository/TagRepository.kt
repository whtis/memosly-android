package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.Tag

interface TagRepository {
    suspend fun listTags(): List<Tag>
    suspend fun upsertTag(name: String): Tag
    suspend fun deleteTag(tag: String)
}
