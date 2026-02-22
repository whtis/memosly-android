package com.whtis.memosly.core.network.dto

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Custom Moshi adapter for [MemoRelationDto] that handles both:
 * - v0.24: memo/relatedMemo may be strings (resource names) or objects; type may be numeric
 * - v0.26: memo/relatedMemo are objects {name, snippet}; type is string
 */
class MemoRelationAdapter {

    @FromJson
    fun fromJson(reader: JsonReader): MemoRelationDto {
        var memo = RelatedMemoInfoDto()
        var relatedMemo = RelatedMemoInfoDto()
        var type = ""

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "memo" -> memo = readMemoField(reader)
                "relatedMemo", "related_memo" -> relatedMemo = readMemoField(reader)
                "type" -> type = readTypeField(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return MemoRelationDto(memo = memo, relatedMemo = relatedMemo, type = type)
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: MemoRelationDto) {
        writer.beginObject()
        writer.name("memo")
        writeMemoField(writer, value.memo)
        writer.name("relatedMemo")
        writeMemoField(writer, value.relatedMemo)
        writer.name("type").value(value.type)
        writer.endObject()
    }

    private fun readMemoField(reader: JsonReader): RelatedMemoInfoDto {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> {
                // v0.24 format: just a resource name string like "memos/123"
                val name = reader.nextString()
                RelatedMemoInfoDto(name = name)
            }
            JsonReader.Token.BEGIN_OBJECT -> {
                // v0.26 format: object with name, uid, snippet
                var name = ""
                var uid = ""
                var snippet = ""
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "name" -> name = reader.nextString()
                        "uid" -> uid = reader.nextString()
                        "snippet" -> snippet = reader.nextString()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                RelatedMemoInfoDto(name = name, uid = uid, snippet = snippet)
            }
            JsonReader.Token.NUMBER -> {
                // Unlikely but handle numeric ID
                val id = reader.nextInt()
                RelatedMemoInfoDto(name = "memos/$id")
            }
            JsonReader.Token.NULL -> {
                reader.nextNull<Any>()
                RelatedMemoInfoDto()
            }
            else -> {
                reader.skipValue()
                RelatedMemoInfoDto()
            }
        }
    }

    private fun readTypeField(reader: JsonReader): String {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> {
                // Proto enum numeric values
                when (reader.nextInt()) {
                    1 -> "REFERENCE"
                    2 -> "COMMENT"
                    else -> "TYPE_UNSPECIFIED"
                }
            }
            JsonReader.Token.NULL -> {
                reader.nextNull<Any>()
                ""
            }
            else -> {
                reader.skipValue()
                ""
            }
        }
    }

    private fun writeMemoField(writer: JsonWriter, info: RelatedMemoInfoDto) {
        writer.beginObject()
        writer.name("name").value(info.name)
        if (info.uid.isNotBlank()) {
            writer.name("uid").value(info.uid)
        }
        if (info.snippet.isNotBlank()) {
            writer.name("snippet").value(info.snippet)
        }
        writer.endObject()
    }
}
