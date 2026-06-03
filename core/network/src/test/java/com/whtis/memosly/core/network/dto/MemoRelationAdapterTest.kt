package com.whtis.memosly.core.network.dto

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the three on-the-wire shapes a Memos server can produce for
 * `MemoRelation`:
 *
 *  - v0.24 (sometimes): `memo` / `relatedMemo` are bare strings ("memos/123")
 *  - v0.24/v0.25: `type` is a numeric proto enum (1 = REFERENCE, 2 = COMMENT)
 *  - v0.26: `memo` / `relatedMemo` are objects with name/uid/snippet; `type` is a string
 *
 * A regression in this adapter silently breaks comment threads, reference
 * chips, and the memo detail page across every supported server version,
 * so the parsing branches are pinned with explicit fixtures.
 */
class MemoRelationAdapterTest {

    private val moshi = Moshi.Builder()
        .add(MemoRelationAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(MemoRelationDto::class.java)

    @Test
    fun `v026 object shape with string type parses fully`() {
        val json = """
            {
              "memo": {"name": "memos/abc", "uid": "abc", "snippet": "hello"},
              "relatedMemo": {"name": "memos/xyz", "uid": "xyz", "snippet": "world"},
              "type": "COMMENT"
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)!!

        assertEquals("memos/abc", dto.memo.name)
        assertEquals("abc", dto.memo.uid)
        assertEquals("hello", dto.memo.snippet)
        assertEquals("memos/xyz", dto.relatedMemo.name)
        assertEquals("xyz", dto.relatedMemo.uid)
        assertEquals("world", dto.relatedMemo.snippet)
        assertEquals("COMMENT", dto.type)
    }

    @Test
    fun `v024 string shape lifts bare names into RelatedMemoInfoDto`() {
        val json = """
            {
              "memo": "memos/abc",
              "relatedMemo": "memos/xyz",
              "type": "REFERENCE"
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)!!

        assertEquals("memos/abc", dto.memo.name)
        assertEquals("", dto.memo.uid)
        assertEquals("", dto.memo.snippet)
        assertEquals("memos/xyz", dto.relatedMemo.name)
        assertEquals("REFERENCE", dto.type)
    }

    @Test
    fun `numeric type 1 maps to REFERENCE`() {
        val json = """
            {
              "memo": {"name": "memos/abc"},
              "relatedMemo": {"name": "memos/xyz"},
              "type": 1
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)!!

        assertEquals("REFERENCE", dto.type)
    }

    @Test
    fun `numeric type 2 maps to COMMENT`() {
        val json = """
            {
              "memo": {"name": "memos/abc"},
              "relatedMemo": {"name": "memos/xyz"},
              "type": 2
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)!!

        assertEquals("COMMENT", dto.type)
    }

    @Test
    fun `unknown numeric type falls back to TYPE_UNSPECIFIED`() {
        val json = """
            {
              "memo": {"name": "memos/abc"},
              "relatedMemo": {"name": "memos/xyz"},
              "type": 99
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)!!

        assertEquals("TYPE_UNSPECIFIED", dto.type)
    }

    @Test
    fun `numeric memo id is rewritten into resource-name form`() {
        val json = """
            {
              "memo": 42,
              "relatedMemo": 7,
              "type": "REFERENCE"
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)!!

        assertEquals("memos/42", dto.memo.name)
        assertEquals("memos/7", dto.relatedMemo.name)
    }

    @Test
    fun `null memo fields yield empty RelatedMemoInfoDto without crashing`() {
        val json = """
            {
              "memo": null,
              "relatedMemo": null,
              "type": null
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)!!

        assertEquals("", dto.memo.name)
        assertEquals("", dto.relatedMemo.name)
        assertEquals("", dto.type)
    }

    @Test
    fun `snake_case related_memo alias is accepted`() {
        val json = """
            {
              "memo": {"name": "memos/abc"},
              "related_memo": {"name": "memos/xyz"},
              "type": "COMMENT"
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)!!

        assertEquals("memos/xyz", dto.relatedMemo.name)
    }

    @Test
    fun `unknown json fields are skipped, not failed on`() {
        val json = """
            {
              "memo": {"name": "memos/abc", "futureField": "ignore-me"},
              "relatedMemo": {"name": "memos/xyz"},
              "type": "REFERENCE",
              "audit": {"createdBy": "system"}
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)!!

        assertEquals("memos/abc", dto.memo.name)
        assertEquals("REFERENCE", dto.type)
    }

    @Test
    fun `toJson emits object shape and omits blank optional fields`() {
        val dto = MemoRelationDto(
            memo = RelatedMemoInfoDto(name = "memos/abc", uid = "abc", snippet = ""),
            relatedMemo = RelatedMemoInfoDto(name = "memos/xyz", uid = "", snippet = ""),
            type = "REFERENCE",
        )

        val json = adapter.toJson(dto)

        // memo has uid but no snippet; relatedMemo has neither
        assertEquals(
            """{"memo":{"name":"memos/abc","uid":"abc"},"relatedMemo":{"name":"memos/xyz"},"type":"REFERENCE"}""",
            json,
        )
    }
}
