package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.IdentityProvider
import com.whtis.memosly.core.model.WorkspaceProfile

interface WorkspaceRepository {
    suspend fun getWorkspaceProfile(): WorkspaceProfile
    suspend fun listIdentityProviders(): List<IdentityProvider>
}
