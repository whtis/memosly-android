package com.whtis.memosly.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

internal const val PLACEHOLDER_HOST = "placeholder.example.com"

@Singleton
class ServerUrlInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Only rewrite the Retrofit placeholder host. Coil image requests
        // already carry a real URL (memos server, or an external host like S3
        // when the server uses S3 storage) and must pass through untouched.
        if (originalRequest.url.host != PLACEHOLDER_HOST) {
            return chain.proceed(originalRequest)
        }

        val serverUrl = tokenManager.serverUrl.value
            ?: return chain.proceed(originalRequest)

        val newUrl = serverUrl.toHttpUrlOrNull()
            ?: "https://$serverUrl".toHttpUrlOrNull()
            ?: return chain.proceed(originalRequest)

        val modifiedUrl = originalRequest.url.newBuilder()
            .scheme(newUrl.scheme)
            .host(newUrl.host)
            .port(newUrl.port)
            .build()

        val modifiedRequest = originalRequest.newBuilder()
            .url(modifiedUrl)
            .build()

        return chain.proceed(modifiedRequest)
    }
}
