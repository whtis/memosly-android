package com.whtis.memosly.core.data.repository

import android.util.Base64
import com.whtis.memosly.core.model.Resource
import com.whtis.memosly.core.network.ServerVersion
import com.whtis.memosly.core.network.TokenManager
import com.whtis.memosly.core.network.api.ResourceApi
import com.whtis.memosly.core.network.dto.CreateResourceRequest
import com.whtis.memosly.core.network.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceRepositoryImpl @Inject constructor(
    private val resourceApi: ResourceApi,
    private val tokenManager: TokenManager,
) : ResourceRepository {

    private val version: ServerVersion
        get() = tokenManager.serverVersion.value

    override suspend fun listResources(): List<Resource> =
        when (version) {
            ServerVersion.V024 -> resourceApi.listResources()
            // v0.25 + v0.26 both use attachments
            else -> resourceApi.listResourcesV026()
        }.all().map { it.toDomain() }

    override suspend fun uploadResource(filename: String, mimeType: String, bytes: ByteArray): Resource {
        val base64Content = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val request = CreateResourceRequest(
            filename = filename,
            type = mimeType,
            content = base64Content,
        )
        return when (version) {
            ServerVersion.V024 -> resourceApi.createResource(request)
            // v0.25 + v0.26 both use attachments
            else -> resourceApi.createResourceV026(request)
        }.toDomain()
    }

    override suspend fun deleteResource(name: String) =
        when (version) {
            ServerVersion.V024 -> resourceApi.deleteResource(name)
            // v0.25 + v0.26 both use attachments
            else -> resourceApi.deleteResourceV026(name)
        }
}
