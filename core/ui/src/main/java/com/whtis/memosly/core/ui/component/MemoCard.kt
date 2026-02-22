package com.whtis.memosly.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.whtis.memosly.core.ui.R
import com.whtis.memosly.core.common.toLocalDateTime
import com.whtis.memosly.core.common.toRelativeTimeString
import com.whtis.memosly.core.model.Memo
import com.whtis.memosly.core.model.MemoRelationType
import com.whtis.memosly.core.model.MemoState
import com.whtis.memosly.core.model.Reaction
import com.whtis.memosly.core.model.Resource
import com.whtis.memosly.core.model.formattedSize
import com.whtis.memosly.core.model.isAudio
import com.whtis.memosly.core.model.isImage
import com.whtis.memosly.core.model.isVideo
import com.whtis.memosly.core.model.displayUrl
import com.whtis.memosly.core.model.thumbnailUrl

@Composable
fun MemoCard(
    memo: Memo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    serverUrl: String = "",
    onTagClick: ((String) -> Unit)? = null,
    creatorLabel: String? = null,
    onReactionClick: ((String) -> Unit)? = null,
    onAddReaction: (() -> Unit)? = null,
    onCommentClick: (() -> Unit)? = null,
    showEmojiPicker: Boolean = false,
    reactionOverrides: List<Reaction>? = null,
    commentPreviews: List<Memo> = emptyList(),
    creatorNames: Map<String, String> = emptyMap(),
    onMediaClick: ((ViewableMedia) -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    contentRenderer: @Composable (String) -> Unit = { text ->
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
    },
) {
    val commentCount = memo.relations.count { it.type == MemoRelationType.COMMENT }
    val displayContent = remember(memo.content, memo.tags) {
        stripInlineTags(stripImageMarkdown(memo.content), memo.tags)
    }
    val contentImageUrls = remember(memo.content) { extractImageUrls(memo.content) }
    val visualResources = remember(memo.resources) { memo.resources.filter { it.isImage() || it.isVideo() } }
    val fileResources = remember(memo.resources) { memo.resources.filter { !it.isImage() && !it.isVideo() } }
    val displayReactions = reactionOverrides ?: memo.reactions
    val hasInteraction = onReactionClick != null || onAddReaction != null
    val hasOverflowMenu = onEdit != null || onArchive != null || onDelete != null
    var showOverflowMenu by remember { mutableStateOf(false) }
    val isArchived = memo.state == MemoState.ARCHIVED

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = com.whtis.memosly.core.ui.theme.MemosShapes.Card,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Content area — clickable to navigate to detail
            Column(modifier = Modifier.clickable(onClick = onClick)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        if (creatorLabel != null) {
                            Text(
                                text = creatorLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "\u00B7",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = memo.displayTime.toLocalDateTime().toRelativeTimeString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (memo.pinned) {
                            Text(
                                text = "Pinned",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (hasOverflowMenu) {
                            Box {
                                IconButton(
                                    onClick = { showOverflowMenu = true },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.MoreVert,
                                        contentDescription = stringResource(R.string.menu),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                ) {
                                    onEdit?.let { callback ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.edit)) },
                                            onClick = { showOverflowMenu = false; callback() },
                                        )
                                    }
                                    onArchive?.let { callback ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(stringResource(if (isArchived) R.string.unarchive else R.string.archive))
                                            },
                                            onClick = { showOverflowMenu = false; callback() },
                                        )
                                    }
                                    onDelete?.let { callback ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.delete)) },
                                            onClick = { showOverflowMenu = false; callback() },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (displayContent.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    contentRenderer(displayContent)
                }

                if (visualResources.isNotEmpty() && serverUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ResourceVisualRow(
                        resources = visualResources,
                        serverUrl = serverUrl,
                        onMediaClick = onMediaClick,
                    )
                } else if (contentImageUrls.isNotEmpty() && serverUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ContentImageRow(
                        imageUrls = contentImageUrls,
                        serverUrl = serverUrl,
                        onMediaClick = onMediaClick,
                    )
                }

                if (fileResources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ResourceFileRow(files = fileResources)
                }

                if (memo.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TagRow(tags = memo.tags, onTagClick = onTagClick)
                }
            }

            // Comment previews (outside clickable area)
            if (commentPreviews.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(6.dp))
                CommentPreviewSection(
                    comments = commentPreviews,
                    totalCount = commentCount,
                    onClick = onClick,
                    creatorNames = creatorNames,
                )
            }

            // Interactive area — reactions, comment button (outside clickable area)
            if (displayReactions.isNotEmpty() || commentCount > 0 || hasInteraction) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (displayReactions.isNotEmpty()) {
                        ReactionRow(
                            reactions = displayReactions,
                            onReactionClick = onReactionClick ?: {},
                            modifier = Modifier.weight(1f, fill = false),
                            onAddReaction = onAddReaction,
                        )
                    } else if (onAddReaction != null) {
                        IconButton(
                            onClick = onAddReaction,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Outlined.AddReaction,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        if (onCommentClick != null) {
                            IconButton(
                                onClick = onCommentClick,
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.ModeComment,
                                    contentDescription = stringResource(R.string.comments),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (commentCount > 0) {
                            Text(
                                text = "$commentCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Inline emoji picker (outside clickable area)
            AnimatedVisibility(
                visible = showEmojiPicker,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                EmojiPicker(
                    onEmojiSelected = { emoji -> onReactionClick?.invoke(emoji) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CommentPreviewSection(
    comments: List<Memo>,
    totalCount: Int,
    onClick: () -> Unit,
    creatorNames: Map<String, String> = emptyMap(),
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        comments.take(3).forEach { comment ->
            val creatorName = creatorNames[comment.creator] ?: comment.creator.substringAfterLast("/")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = creatorName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = comment.content.replace('\n', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (totalCount > comments.size) {
            Text(
                text = stringResource(R.string.view_more_comments, totalCount - comments.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onClick),
            )
        }
    }
}

@Composable
private fun ResourceVisualRow(
    resources: List<Resource>,
    serverUrl: String,
    onMediaClick: ((ViewableMedia) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        resources.take(3).forEach { resource ->
            val media = ViewableMedia(
                url = resource.displayUrl(serverUrl),
                isVideo = resource.isVideo(),
                filename = resource.filename,
                size = resource.size,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 120.dp)
                    .clip(shape)
                    .then(
                        if (onMediaClick != null) {
                            Modifier.clickable { onMediaClick(media) }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (resource.isImage()) {
                    AsyncImage(
                        model = resource.thumbnailUrl(serverUrl),
                        contentDescription = resource.filename,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    // Video: show frame thumbnail with play icon overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = resource.displayUrl(serverUrl),
                            contentDescription = resource.filename,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop,
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (resources.size > 3) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+${resources.size - 3}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourceFileRow(
    files: List<Resource>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        files.forEach { file ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    when {
                        file.isAudio() -> Icons.Outlined.AudioFile
                        else -> Icons.Outlined.AttachFile
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = file.filename,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val sizeText = file.formattedSize()
                if (sizeText.isNotEmpty()) {
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

private val imageMarkdownRegex = Regex("""!\[([^\]]*)\]\(([^)]+)\)""")

private fun stripImageMarkdown(content: String): String =
    imageMarkdownRegex.replace(content, "").trim()

private fun extractImageUrls(content: String): List<Pair<String, String>> =
    imageMarkdownRegex.findAll(content).map { match ->
        val alt = match.groupValues[1]
        val url = match.groupValues[2]
        alt to url
    }.toList()

private val videoExtensions = setOf("mp4", "webm", "mov", "avi", "mkv", "3gp", "m4v")

private fun isVideoUrl(url: String): Boolean {
    val path = url.substringBefore("?").substringBefore("#")
    val decoded = try { java.net.URLDecoder.decode(path, "UTF-8") } catch (_: Exception) { path }
    val ext = decoded.substringAfterLast('.', "").lowercase()
    return ext in videoExtensions
}

@Composable
private fun ContentImageRow(
    imageUrls: List<Pair<String, String>>,
    serverUrl: String,
    onMediaClick: ((ViewableMedia) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        imageUrls.take(3).forEach { (alt, url) ->
            val resolvedUrl = if (url.startsWith("/") && serverUrl.isNotBlank()) "$serverUrl$url" else url
            val isVideo = isVideoUrl(url)
            val media = ViewableMedia(
                url = resolvedUrl,
                isVideo = isVideo,
                filename = alt.ifBlank { url.substringAfterLast("/") },
                size = 0L,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 120.dp)
                    .clip(shape)
                    .then(
                        if (onMediaClick != null) Modifier.clickable { onMediaClick(media) } else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = resolvedUrl,
                    contentDescription = alt,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop,
                )
                if (isVideo) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun stripInlineTags(content: String, tags: List<String>): String {
    if (tags.isEmpty()) return content
    var result = content
    for (tag in tags.sortedByDescending { it.length }) {
        result = result.replace("#$tag", "")
    }
    return result.trim()
}
