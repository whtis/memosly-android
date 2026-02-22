package com.whtis.memosly.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorkspaceProfileDto(
    val owner: String,
    val version: String,
    val mode: String,
)

@JsonClass(generateAdapter = true)
data class WorkspaceSettingDto(
    val generalSetting: WorkspaceGeneralSettingDto?,
)

@JsonClass(generateAdapter = true)
data class WorkspaceGeneralSettingDto(
    val additionalScript: String?,
    val additionalStyle: String?,
    val customProfile: CustomProfileDto?,
)

@JsonClass(generateAdapter = true)
data class CustomProfileDto(
    val title: String?,
    val description: String?,
    val logoUrl: String?,
    val locale: String?,
    val appearance: String?,
)
