package com.whtis.memosly.core.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import coil3.compose.AsyncImage
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.node.*

private val videoExtensions = setOf("mp4", "webm", "mov", "avi", "mkv", "3gp", "m4v")

private fun isVideoUrl(url: String): Boolean {
    val path = url.substringBefore("?").substringBefore("#")
    val decoded = try { java.net.URLDecoder.decode(path, "UTF-8") } catch (_: Exception) { path }
    val ext = decoded.substringAfterLast('.', "").lowercase()
    return ext in videoExtensions
}

@Composable
fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier,
    serverUrl: String = "",
    onTagClick: ((String) -> Unit)? = null,
    onMediaClick: ((url: String, isVideo: Boolean) -> Unit)? = null,
) {
    val document = remember(content) { MarkdownParser.parse(content) }

    Column(modifier = modifier) {
        var node = document.firstChild
        while (node != null) {
            RenderBlock(node = node, serverUrl = serverUrl, onTagClick = onTagClick, onMediaClick = onMediaClick)
            Spacer(modifier = Modifier.height(4.dp))
            node = node.next
        }
    }
}

@Composable
private fun RenderBlock(
    node: Node,
    serverUrl: String = "",
    onTagClick: ((String) -> Unit)? = null,
    onMediaClick: ((url: String, isVideo: Boolean) -> Unit)? = null,
) {
    when (node) {
        is Heading -> {
            val style = when (node.level) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                4 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            }
            Text(
                text = renderInlines(node, serverUrl = serverUrl),
                style = style,
            )
        }
        is Paragraph -> {
            // Check if paragraph contains only a single Image node
            val firstChild = node.firstChild
            if (firstChild is Image && firstChild.next == null) {
                RenderImage(image = firstChild, serverUrl = serverUrl, onMediaClick = onMediaClick)
            } else {
                val uriHandler = LocalUriHandler.current
                val annotated = renderInlines(node, onTagClick = onTagClick, serverUrl = serverUrl)
                ClickableText(
                    text = annotated,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    onClick = { offset ->
                        annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                        annotated.getStringAnnotations("TAG", offset, offset).firstOrNull()?.let {
                            onTagClick?.invoke(it.item)
                        }
                    },
                )
            }
        }
        is BulletList -> {
            var item = node.firstChild
            while (item != null) {
                if (item is ListItem) {
                    RenderListItem(item = item, ordered = false, index = 0, serverUrl = serverUrl, onTagClick = onTagClick, onMediaClick = onMediaClick)
                }
                item = item.next
            }
        }
        is OrderedList -> {
            var item = node.firstChild
            var index = node.startNumber
            while (item != null) {
                if (item is ListItem) {
                    RenderListItem(item = item, ordered = true, index = index, serverUrl = serverUrl, onTagClick = onTagClick, onMediaClick = onMediaClick)
                    index++
                }
                item = item.next
            }
        }
        is BlockQuote -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            ) {
                Row {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outline),
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        var child = node.firstChild
                        while (child != null) {
                            RenderBlock(node = child, serverUrl = serverUrl, onTagClick = onTagClick, onMediaClick = onMediaClick)
                            child = child.next
                        }
                    }
                }
            }
        }
        is FencedCodeBlock -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            ) {
                Text(
                    text = node.literal.trimEnd(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }
        }
        is IndentedCodeBlock -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            ) {
                Text(
                    text = node.literal.trimEnd(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }
        }
        is ThematicBreak -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun RenderImage(
    image: Image,
    serverUrl: String,
    onMediaClick: ((url: String, isVideo: Boolean) -> Unit)? = null,
) {
    val url = resolveUrl(image.destination, serverUrl)
    val isVideo = isVideoUrl(image.destination)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onMediaClick != null) Modifier.clickable { onMediaClick(url, isVideo) } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = image.title ?: "",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
        if (isVideo) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private fun resolveUrl(url: String, serverUrl: String): String {
    if (url.startsWith("http://") || url.startsWith("https://")) return url
    if (url.startsWith("/") && serverUrl.isNotBlank()) return "$serverUrl$url"
    return url
}

@Composable
private fun RenderListItem(
    item: ListItem,
    ordered: Boolean,
    index: Int,
    serverUrl: String = "",
    onTagClick: ((String) -> Unit)? = null,
    onMediaClick: ((url: String, isVideo: Boolean) -> Unit)? = null,
) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val marker = item.firstChild
        if (marker is TaskListItemMarker) {
            Checkbox(
                checked = marker.isChecked,
                onCheckedChange = null,
                modifier = Modifier.padding(end = 4.dp),
            )
        } else if (ordered) {
            Text(
                text = "$index. ",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 4.dp),
            )
        } else {
            Text(
                text = "\u2022 ",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 4.dp),
            )
        }

        Column {
            var child = item.firstChild
            while (child != null) {
                if (child !is TaskListItemMarker) {
                    RenderBlock(node = child, serverUrl = serverUrl, onTagClick = onTagClick, onMediaClick = onMediaClick)
                }
                child = child.next
            }
        }
    }
}

@Composable
private fun renderInlines(
    node: Node,
    onTagClick: ((String) -> Unit)? = null,
    serverUrl: String = "",
): AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        var child = node.firstChild
        while (child != null) {
            appendInlineNode(child, primaryColor, serverUrl)
            child = child.next
        }
    }
}

private fun AnnotatedString.Builder.appendInlineNode(
    node: Node,
    linkColor: androidx.compose.ui.graphics.Color,
    serverUrl: String = "",
) {
    when (node) {
        is org.commonmark.node.Text -> {
            val text = node.literal
            val tagRegex = Regex("""#([^\s#]+)""")
            var lastIndex = 0
            tagRegex.findAll(text).forEach { match ->
                append(text.substring(lastIndex, match.range.first))
                pushStringAnnotation("TAG", match.groupValues[1])
                withStyle(SpanStyle(color = linkColor)) {
                    append(match.value)
                }
                pop()
                lastIndex = match.range.last + 1
            }
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
        is Code -> {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = linkColor.copy(alpha = 0.1f))) {
                append(node.literal)
            }
        }
        is Emphasis -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                var child = node.firstChild
                while (child != null) {
                    appendInlineNode(child, linkColor, serverUrl)
                    child = child.next
                }
            }
        }
        is StrongEmphasis -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                var child = node.firstChild
                while (child != null) {
                    appendInlineNode(child, linkColor, serverUrl)
                    child = child.next
                }
            }
        }
        is Link -> {
            val resolvedUrl = resolveUrl(node.destination, serverUrl)
            pushStringAnnotation("URL", resolvedUrl)
            withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                var child = node.firstChild
                while (child != null) {
                    appendInlineNode(child, linkColor, serverUrl)
                    child = child.next
                }
            }
            pop()
        }
        is Image -> {
            // Inline image in mixed content - show alt text as placeholder
            append("[")
            var child = node.firstChild
            while (child != null) {
                appendInlineNode(child, linkColor, serverUrl)
                child = child.next
            }
            append("]")
        }
        is Strikethrough -> {
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                var child = node.firstChild
                while (child != null) {
                    appendInlineNode(child, linkColor, serverUrl)
                    child = child.next
                }
            }
        }
        is SoftLineBreak -> append("\n")
        is HardLineBreak -> append("\n")
    }
}
