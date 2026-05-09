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
        val token = tokenManager.accessToken.value
        if (token == null || !isMemosServerRequest(request.url.host)) {
            // Skip auth for external URLs (e.g. S3 externalLink images loaded by Coil) —
            // sending Bearer/Cookie to S3 makes it reject the request as malformed auth.
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

    private fun isMemosServerRequest(requestHost: String): Boolean {
        val configured = tokenManager.serverUrl.value?.toHttpUrlOrNull()?.host ?: return false
        return requestHost.equals(configured, ignoreCase = true)
    }
}
