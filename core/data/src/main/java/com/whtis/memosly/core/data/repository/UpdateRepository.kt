package com.whtis.memosly.core.data.repository

data class AppUpdateInfo(
    val latestVersion: String,
    val releaseNotes: String,
    val hasUpdate: Boolean,
)

interface UpdateRepository {
    suspend fun checkForUpdate(currentVersion: String): AppUpdateInfo
}
