package com.whtis.memosly.core.markdown

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.node.*

/**
 * A non-clickable markdown preview renderer that flattens markdown into a single
 * styled Text composable. Safe for use inside clickable containers (e.g. MemoCard).
 */
@Composable
fun MarkdownPreview(
    content: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 6,
) {
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(content, codeBackground, linkColor) {
        renderMarkdownPreview(content, codeBackground, linkColor)
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private fun renderMarkdownPreview(
    content: String,
    codeBackground: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color,
): AnnotatedString {
    val document = MarkdownParser.parse(content)
    return buildAnnotatedString {
        var isFirstBlock = true
        var node = document.firstChild
        while (node != null) {
            if (!isFirstBlock && node !is ThematicBreak) {
                append("\n")
            }
            appendBlock(node, codeBackground, linkColor)
            isFirstBlock = false
            node = node.next
        }
    }
}

private fun AnnotatedString.Builder.appendBlock(
    node: Node,
    codeBackground: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color,
) {
    when (node) {
        is Heading -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInlines(node, codeBackground, linkColor)
            }
        }
        is Paragraph -> {
            appendInlines(node, codeBackground, linkColor)
        }
        is BulletList -> {
            var item = node.firstChild
            var first = true
            while (item != null) {
                if (item is ListItem) {
                    if (!first) append("\n")
                    append("\u2022 ")
                    appendListItemContent(item, codeBackground, linkColor)
                    first = false
                }
                item = item.next
            }
        }
        is OrderedList -> {
            var item = node.firstChild
            var index = node.startNumber
            var first = true
            while (item != null) {
                if (item is ListItem) {
                    if (!first) append("\n")
                    append("$index. ")
                    appendListItemContent(item, codeBackground, linkColor)
                    index++
                    first = false
                }
                item = item.next
            }
        }
        is BlockQuote -> {
            append("> ")
            var child = node.firstChild
            while (child != null) {
                appendBlock(child, codeBackground, linkColor)
                child = child.next
            }
        }
        is FencedCodeBlock -> {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) {
                append(node.literal.trimEnd())
            }
        }
        is IndentedCodeBlock -> {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) {
                append(node.literal.trimEnd())
            }
        }
        is ThematicBreak -> {
            append("\n---\n")
        }
    }
}

private fun AnnotatedString.Builder.appendListItemContent(
    item: ListItem,
    codeBackground: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color,
) {
    var child = item.firstChild
    while (child != null) {
        if (child is Paragraph) {
            appendInlines(child, codeBackground, linkColor)
        } else {
            appendBlock(child, codeBackground, linkColor)
        }
        child = child.next
    }
}

private fun AnnotatedString.Builder.appendInlines(
    node: Node,
    codeBackground: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color,
) {
    var child = node.firstChild
    while (child != null) {
        appendInlineNode(child, codeBackground, linkColor)
        child = child.next
    }
}

private fun AnnotatedString.Builder.appendInlineNode(
    node: Node,
    codeBackground: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color,
) {
    when (node) {
        is org.commonmark.node.Text -> append(node.literal)
        is Code -> {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) {
                append(node.literal)
            }
        }
        is Emphasis -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendInlines(node, codeBackground, linkColor)
            }
        }
        is StrongEmphasis -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInlines(node, codeBackground, linkColor)
            }
        }
        is Image -> {
            withStyle(SpanStyle(color = linkColor)) {
                append("[")
                // Show alt text from child nodes, or filename from URL
                val hasChildren = node.firstChild != null
                if (hasChildren) {
                    appendInlines(node, codeBackground, linkColor)
                } else {
                    append(node.destination.substringAfterLast("/"))
                }
                append("]")
            }
        }
        is Link -> {
            withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                appendInlines(node, codeBackground, linkColor)
            }
        }
        is Strikethrough -> {
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                appendInlines(node, codeBackground, linkColor)
            }
        }
        is SoftLineBreak -> append("\n")
        is HardLineBreak -> append("\n")
    }
}
