package com.whtis.memosly.feature.memo

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

/**
 * Largest file we hand to the upload path.
 *
 * Matches the Memos server default (MEMOS_MAX_UPLOAD_SIZE_MIB = 32), so anything above this
 * would be rejected server-side anyway. It also keeps us clear of OOM: uploads are encoded
 * fully in memory (bytes -> base64 string -> JSON), so peak heap runs several times the file
 * size, and the app does not request a large heap.
 */
const val MAX_UPLOAD_BYTES: Int = 32 * 1024 * 1024

const val MAX_UPLOAD_MB: Int = MAX_UPLOAD_BYTES / (1024 * 1024)

sealed interface MediaReadResult {
    data class Success(
        val filename: String,
        val mimeType: String,
        val bytes: ByteArray,
    ) : MediaReadResult

    data class TooLarge(val displayName: String) : MediaReadResult

    data class Unreadable(val displayName: String) : MediaReadResult
}

/**
 * Turns a content URI into an upload payload, shared by the media picker and the share intent.
 */
class MediaUriReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun read(uri: Uri): MediaReadResult {
        val resolver = context.contentResolver
        val displayName = queryDisplayName(resolver, uri)
        val reportedSize = queryReportedSize(resolver, uri)

        // Reject on the provider's reported size first — reading a 300MB video into a
        // ByteArray to discover it is too big is the very crash this guards against.
        if (reportedSize != null && reportedSize > MAX_UPLOAD_BYTES) {
            return MediaReadResult.TooLarge(displayName)
        }

        val bytes = try {
            resolver.openInputStream(uri)?.use { stream ->
                if (reportedSize != null) {
                    stream.readBytes()
                } else {
                    // Provider didn't report a size; cap the read so an oversized file
                    // can't blow the heap before we notice.
                    stream.readAtMost(MAX_UPLOAD_BYTES) ?: return MediaReadResult.TooLarge(displayName)
                }
            } ?: return MediaReadResult.Unreadable(displayName)
        } catch (e: Exception) {
            return MediaReadResult.Unreadable(displayName)
        }

        if (bytes.size > MAX_UPLOAD_BYTES) return MediaReadResult.TooLarge(displayName)
        if (bytes.isEmpty()) return MediaReadResult.Unreadable(displayName)

        val rawMimeType = resolver.getType(uri)
        val mimeType = rawMimeType?.takeIf { it != "application/octet-stream" }
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(displayName.substringAfterLast('.', "").lowercase())
            ?: guessMimeTypeFromBytes(bytes)
            ?: rawMimeType
            ?: "application/octet-stream"

        // Ensure filename has an extension so renderers can detect media type from the URL
        val filename = if (!displayName.contains('.')) {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (ext != null) "$displayName.$ext" else displayName
        } else displayName

        return MediaReadResult.Success(filename, mimeType, bytes)
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String {
        val name = try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && index >= 0) it.getString(index) else null
            }
        } catch (e: Exception) {
            null
        }
        return name?.takeIf { it.isNotBlank() } ?: "file_${System.currentTimeMillis()}"
    }

    private fun queryReportedSize(resolver: ContentResolver, uri: Uri): Long? = try {
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use {
            val index = it.getColumnIndex(OpenableColumns.SIZE)
            if (it.moveToFirst() && index >= 0 && !it.isNull(index)) it.getLong(index) else null
        }
    } catch (e: Exception) {
        null
    }
}

/** Reads the stream, or returns null as soon as it exceeds [limit] bytes. */
private fun InputStream.readAtMost(limit: Int): ByteArray? {
    val buffer = ByteArrayOutputStream()
    val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = read(chunk)
        if (read == -1) break
        total += read
        if (total > limit) return null
        buffer.write(chunk, 0, read)
    }
    return buffer.toByteArray()
}

internal fun guessMimeTypeFromBytes(bytes: ByteArray): String? {
    if (bytes.size < 12) return null
    // JPEG: FF D8 FF
    if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) return "image/jpeg"
    // PNG: 89 50 4E 47
    if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) return "image/png"
    // GIF: GIF8
    if (bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x38.toByte()) return "image/gif"
    // WEBP: RIFF....WEBP
    if (bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte()
        && bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() && bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte()
    ) return "image/webp"
    // MP4/MOV/3GP: ftyp at offset 4
    if (bytes[4] == 0x66.toByte() && bytes[5] == 0x74.toByte() && bytes[6] == 0x79.toByte() && bytes[7] == 0x70.toByte()) return "video/mp4"
    return null
}
