package com.whtis.memosly.core.network.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.whtis.memosly.core.network.dto.ResourceRef
import com.whtis.memosly.core.network.dto.SetMemoAttachmentsRequest
import com.whtis.memosly.core.network.dto.SetMemoResourcesRequest
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Pins the HTTP method and path used to link uploaded files to a memo.
 *
 * Both calls are bound to `patch:` in the Memos proto. Sending POST to the v0.24
 * endpoint answers 501 and the upload stays orphaned — server-side it shows up as an
 * unused resource, and the memo renders without its images on every client. That
 * shipped, because a wrong verb fails quietly and looks exactly like a save that worked.
 */
class MemoApiAttachmentEndpointTest {

    private lateinit var server: MockWebServer
    private lateinit var api: MemoApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                )
            )
            .build()
            .create(MemoApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `v024 setMemoResources patches the resources endpoint`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        runBlocking {
            api.setMemoResources(
                "4fLRfWCwya4YA2CvnBmWZY",
                SetMemoResourcesRequest(listOf(ResourceRef("resources/R6dURNySEeeTUbaCz43Ft2"))),
            )
        }

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/memos/4fLRfWCwya4YA2CvnBmWZY/resources", request.path)
        assertEquals(
            """{"resources":[{"name":"resources/R6dURNySEeeTUbaCz43Ft2"}]}""",
            request.body.readUtf8(),
        )
    }

    @Test
    fun `v025 and up setMemoAttachments patches the attachments endpoint`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        runBlocking {
            api.setMemoAttachments(
                "bsveqyE5j3H6YXvWBXuzBP",
                SetMemoAttachmentsRequest(listOf(ResourceRef("attachments/Bc8eUUhk7jCGLgSkKrArYW"))),
            )
        }

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/memos/bsveqyE5j3H6YXvWBXuzBP/attachments", request.path)
        assertEquals(
            """{"attachments":[{"name":"attachments/Bc8eUUhk7jCGLgSkKrArYW"}]}""",
            request.body.readUtf8(),
        )
    }
}
