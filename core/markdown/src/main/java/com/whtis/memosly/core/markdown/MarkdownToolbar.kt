package com.whtis.memosly.core.markdown

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

data class ToolbarAction(
    val icon: ImageVector,
    val label: String,
    val prefix: String,
    val suffix: String = "",
)

private val toolbarActions = listOf(
    ToolbarAction(Icons.Outlined.FormatBold, "Bold", "**", "**"),
    ToolbarAction(Icons.Outlined.FormatItalic, "Italic", "*", "*"),
    ToolbarAction(Icons.Outlined.FormatStrikethrough, "Strikethrough", "~~", "~~"),
    ToolbarAction(Icons.Outlined.Code, "Code", "`", "`"),
    ToolbarAction(Icons.Outlined.Title, "Heading", "## "),
    ToolbarAction(Icons.Outlined.FormatQuote, "Quote", "> "),
    ToolbarAction(Icons.Outlined.FormatListBulleted, "Bullet list", "- "),
    ToolbarAction(Icons.Outlined.FormatListNumbered, "Numbered list", "1. "),
    ToolbarAction(Icons.Outlined.Link, "Link", "[", "](url)"),
    ToolbarAction(Icons.Outlined.Tag, "Tag", "#"),
)

@Composable
fun MarkdownToolbar(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    onAttachClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Attach media button (first, separated)
        if (onAttachClick != null) {
            IconButton(onClick = onAttachClick) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = "Attach",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }

        // Markdown formatting buttons
        toolbarActions.forEach { action ->
            IconButton(
                onClick = {
                    val newValue = insertMarkdown(textFieldValue, action.prefix, action.suffix)
                    onValueChange(newValue)
                },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun insertMarkdown(
    value: TextFieldValue,
    prefix: String,
    suffix: String,
): TextFieldValue {
    val text = value.text
    val selection = value.selection
    val selectedText = text.substring(selection.min, selection.max)
    val newText = buildString {
        append(text.substring(0, selection.min))
        append(prefix)
        append(selectedText)
        append(suffix)
        append(text.substring(selection.max))
    }
    val cursorPosition = selection.min + prefix.length + selectedText.length
    return TextFieldValue(
        text = newText,
        selection = TextRange(cursorPosition),
    )
}
