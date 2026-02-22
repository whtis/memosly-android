package com.whtis.memosly.core.network

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
        return if (token != null) {
            val builder = request.newBuilder()
            when (tokenManager.serverVersion.value) {
                // v0.25 uses session-based auth via Cookie header
                ServerVersion.V025 -> builder.header("Cookie", "user_session=$token")
                // v0.24/v0.26 use Bearer token auth
                else -> builder.header("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        } else {
            chain.proceed(request)
        }
    }
}
