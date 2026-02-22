package com.whtis.memosly.core.network.api

import com.whtis.memosly.core.network.dto.WorkspaceProfileDto
import com.whtis.memosly.core.network.dto.WorkspaceSettingDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface WorkspaceApi {
    @GET("api/v1/workspace/profile")
    suspend fun getWorkspaceProfile(): WorkspaceProfileDto

    @GET("api/v1/workspace/setting")
    suspend fun getWorkspaceSetting(): WorkspaceSettingDto

    @PATCH("api/v1/workspace/setting")
    suspend fun updateWorkspaceSetting(@Body setting: WorkspaceSettingDto): WorkspaceSettingDto
}
