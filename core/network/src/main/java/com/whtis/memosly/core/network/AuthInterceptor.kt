package com.whtis.memosly.core.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenManager.accessToken.value
            ?: return chain.proceed(request)

        // Only attach credentials for requests bound to the memos server (or
        // the Retrofit placeholder, which ServerUrlInterceptor will rewrite).
        // External hosts — e.g. S3 buckets referenced via Resource.externalLink
        // when the server uses S3 storage — must not receive our Bearer token,
        // since the foreign Authorization header breaks presigned URL requests.
        if (!isMemosServerHost(request.url)) {
            return chain.proceed(request)
        }

        val builder = request.newBuilder()
        when (tokenManager.serverVersion.value) {
            // v0.25 uses session-based auth via Cookie header
            ServerVersion.V025 -> builder.header("Cookie", "user_session=$token")
            // v0.24/v0.26 use Bearer token auth
            else -> builder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }

    private fun isMemosServerHost(url: HttpUrl): Boolean {
        if (url.host == PLACEHOLDER_HOST) return true
        val configured = tokenManager.serverUrl.value ?: return false
        val configuredHost = configured.toHttpUrlOrNull()?.host
            ?: "https://$configured".toHttpUrlOrNull()?.host
            ?: return false
        return url.host.equals(configuredHost, ignoreCase = true)
    }
}
