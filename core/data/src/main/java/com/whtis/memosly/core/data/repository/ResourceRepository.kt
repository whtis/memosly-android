package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.Resource

interface ResourceRepository {
    suspend fun listResources(): List<Resource>
    suspend fun uploadResource(filename: String, mimeType: String, bytes: ByteArray): Resource
    suspend fun deleteResource(name: String)
}
