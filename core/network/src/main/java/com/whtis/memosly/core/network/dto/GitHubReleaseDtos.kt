package com.whtis.memosly.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VersionInfoDto(
    val version: String = "",
    @Json(name = "release_notes") val releaseNotes: String = "",
    @Json(name = "release_notes_zh") val releaseNotesZh: String = "",
)
