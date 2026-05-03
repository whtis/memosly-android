package com.whtis.memosly.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/** Retrofit base URL host. ServerUrlInterceptor rewrites only requests to this host. */
const val PLACEHOLDER_HOST = "placeholder.example.com"

@Singleton
class ServerUrlInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Only rewrite Retrofit-generated requests. External URLs (e.g. S3-stored
        // images loaded via Coil on the same OkHttp client) must pass through.
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
