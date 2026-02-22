package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.IdentityProvider
import com.whtis.memosly.core.model.WorkspaceProfile
import com.whtis.memosly.core.network.api.IdentityProviderApi
import com.whtis.memosly.core.network.api.WorkspaceApi
import com.whtis.memosly.core.network.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepositoryImpl @Inject constructor(
    private val workspaceApi: WorkspaceApi,
    private val identityProviderApi: IdentityProviderApi,
) : WorkspaceRepository {

    override suspend fun getWorkspaceProfile(): WorkspaceProfile =
        workspaceApi.getWorkspaceProfile().toDomain()

    override suspend fun listIdentityProviders(): List<IdentityProvider> =
        identityProviderApi.listIdentityProviders().identityProviders.map { it.toDomain() }
}
