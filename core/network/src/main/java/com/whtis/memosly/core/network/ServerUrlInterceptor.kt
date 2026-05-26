package com.whtis.memosly.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

<<<<<<< HEAD
internal const val PLACEHOLDER_HOST = "placeholder.example.com"
=======
/** Retrofit base URL host. ServerUrlInterceptor rewrites only requests to this host. */
const val PLACEHOLDER_HOST = "placeholder.example.com"
>>>>>>> origin/night-shift/20260504-s3-image-loading

@Singleton
class ServerUrlInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
<<<<<<< HEAD

<<<<<<< HEAD
        // Only rewrite the Retrofit placeholder host. Coil image requests
        // already carry a real URL (memos server, or an external host like S3
        // when the server uses S3 storage) and must pass through untouched.
=======
        // Only rewrite Retrofit-generated requests. External URLs (e.g. S3-stored
        // images loaded via Coil on the same OkHttp client) must pass through.
>>>>>>> origin/night-shift/20260504-s3-image-loading
=======
        // Only rewrite Retrofit's placeholder base URL to the configured server;
        // requests with concrete hosts (e.g. Coil loading external S3 image URLs)
        // must pass through untouched, otherwise external links get redirected
        // to the Memos host and return 404.
>>>>>>> origin/night-shift/20260510-auth-only-memos-host
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
