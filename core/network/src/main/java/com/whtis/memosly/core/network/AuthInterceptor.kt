package com.whtis.memosly.core.network

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
        val token = tokenManager.accessToken.value ?: return chain.proceed(request)

        // Don't leak Memos credentials to third-party hosts (e.g. S3-served
        // attachments loaded via Coil on the shared OkHttp client). When the
        // server URL is unknown, fall through and attach the header as before.
        val serverHost = tokenManager.serverUrl.value?.toHttpUrlOrNull()?.host
        val requestHost = request.url.host
        if (serverHost != null && requestHost != serverHost && requestHost != PLACEHOLDER_HOST) {
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
}
