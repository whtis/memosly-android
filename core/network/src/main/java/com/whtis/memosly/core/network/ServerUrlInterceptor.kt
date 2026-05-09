package com.whtis.memosly.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerUrlInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        // Only rewrite Retrofit's placeholder base URL to the configured server;
        // requests with concrete hosts (e.g. Coil loading external S3 image URLs)
        // must pass through untouched, otherwise external links get redirected
        // to the Memos host and return 404.
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

    private companion object {
        const val PLACEHOLDER_HOST = "placeholder.example.com"
    }
}
