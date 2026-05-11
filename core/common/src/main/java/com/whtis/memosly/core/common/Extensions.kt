package com.whtis.memosly.core.common

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun String.toLocalDateTime(): LocalDateTime {
    val instant = Instant.parse(this)
    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
}

fun LocalDateTime.toRelativeTimeString(): String {
    val now = LocalDateTime.now()
    val minutes = java.time.Duration.between(this, now).toMinutes()
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1440 -> "${minutes / 60}h ago"
        minutes < 10080 -> "${minutes / 1440}d ago"
        else -> format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}

fun String.extractTags(): List<String> {
    val regex = Regex("""(?:^|\s)#([a-zA-Z0-9_/\-]+)""")
    return regex.findAll(this).map { it.groupValues[1] }.distinct().toList()
}

/**
 * Escapes a string for safe embedding inside a CEL double-quoted string literal.
 *
 * Memos' ListMemos filter parameter is CEL — interpolating raw user input lets a
 * stray `"` or `\` produce malformed expressions, so the server rejects the request
 * and search silently returns nothing. Only `\` and `"` need escaping for the
 * double-quoted form we use.
 */
fun String.escapeCelStringLiteral(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")
