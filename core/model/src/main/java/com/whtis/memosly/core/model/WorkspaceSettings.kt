package com.whtis.memosly.core.model

data class WorkspaceGeneralSetting(
    val additionalScript: String,
    val additionalStyle: String,
    val customProfile: CustomProfile,
)

data class CustomProfile(
    val title: String,
    val description: String,
    val logoUrl: String,
    val locale: String,
    val appearance: String,
)
