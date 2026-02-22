package com.whtis.memosly.core.model

import java.net.URLEncoder

data class Resource(
    val name: String,
    val uid: String,
    val createTime: String,
    val filename: String,
    val type: String,
    val size: Long,
    val memo: String,
    val externalLink: String,
)

fun Resource.isImage(): Boolean =
    type.startsWith("image/")

fun Resource.isVideo(): Boolean =
    type.startsWith("video/")

fun Resource.isAudio(): Boolean =
    type.startsWith("audio/")

fun Resource.formattedSize(): String {
    if (size <= 0) return ""
    val kb = size / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.0f KB".format(kb)
        else -> "$size B"
    }
}

private fun encodePathSegment(segment: String): String =
    URLEncoder.encode(segment, "UTF-8").replace("+", "%20")

fun Resource.displayUrl(serverUrl: String): String =
    if (externalLink.isNotBlank()) externalLink
    else "$serverUrl/file/$name/${encodePathSegment(filename)}"

fun Resource.thumbnailUrl(serverUrl: String): String =
    if (externalLink.isNotBlank()) externalLink
    else "$serverUrl/file/$name/${encodePathSegment(filename)}?thumbnail=true"
