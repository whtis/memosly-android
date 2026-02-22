package com.whtis.memosly.core.data.repository

import com.squareup.moshi.Moshi
import com.whtis.memosly.core.network.dto.VersionInfoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepositoryImpl @Inject constructor(
    private val moshi: Moshi,
) : UpdateRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun checkForUpdate(currentVersion: String): AppUpdateInfo =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(VERSION_JSON_URL)
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext noUpdate(currentVersion)
                    }
                    val json = resp.body?.string()
                        ?: return@withContext noUpdate(currentVersion)
                    val info = moshi.adapter(VersionInfoDto::class.java).fromJson(json)
                        ?: return@withContext noUpdate(currentVersion)

                    val latestVersion = info.version.removePrefix("v")
                    val hasUpdate = compareVersions(latestVersion, currentVersion) > 0

                    val isZh = Locale.getDefault().language == "zh"
                    val notes = if (isZh && info.releaseNotesZh.isNotBlank()) {
                        info.releaseNotesZh
                    } else {
                        info.releaseNotes
                    }

                    AppUpdateInfo(
                        latestVersion = latestVersion,
                        releaseNotes = notes,
                        hasUpdate = hasUpdate,
                    )
                }
            } catch (_: Exception) {
                noUpdate(currentVersion)
            }
        }

    companion object {
        private const val VERSION_JSON_URL =
            "https://raw.githubusercontent.com/whtis/memosly-android/main/version.json"

        private fun noUpdate(currentVersion: String) = AppUpdateInfo(
            latestVersion = currentVersion,
            releaseNotes = "",
            hasUpdate = false,
        )

        internal fun compareVersions(a: String, b: String): Int {
            val partsA = a.split(".").map { it.toIntOrNull() ?: 0 }
            val partsB = b.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(partsA.size, partsB.size)
            for (i in 0 until maxLen) {
                val pa = partsA.getOrElse(i) { 0 }
                val pb = partsB.getOrElse(i) { 0 }
                if (pa != pb) return pa - pb
            }
            return 0
        }
    }
}
