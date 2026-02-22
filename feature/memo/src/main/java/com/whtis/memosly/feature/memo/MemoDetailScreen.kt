package com.whtis.memosly.feature.memo

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.PlayArrow
import com.whtis.memosly.core.ui.component.MediaViewerDialog
import com.whtis.memosly.core.ui.component.ViewableMedia
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whtis.memosly.core.ui.R as UiR
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.whtis.memosly.core.common.toLocalDateTime
import com.whtis.memosly.core.common.toRelativeTimeString
import com.whtis.memosly.core.markdown.MarkdownContent
import com.whtis.memosly.core.model.Resource
import com.whtis.memosly.core.model.displayUrl
import com.whtis.memosly.core.model.isImage
import com.whtis.memosly.core.model.isVideo
import com.whtis.memosly.core.ui.component.EmojiPicker
import com.whtis.memosly.core.ui.component.ErrorContent
import com.whtis.memosly.core.ui.component.LoadingContent
import com.whtis.memosly.core.ui.component.ReactionRow
import com.whtis.memosly.core.ui.component.TagRow
import com.whtis.memosly.core.ui.component.VisibilityChip
import com.whtis.memosly.core.ui.theme.MemosShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemoDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onMemoChanged: () -> Unit = onBack,
    viewModel: MemoDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val memo = uiState.memo
    val serverUrl = viewModel.serverUrl
    val authHeaders = viewModel.authHeaders
    var viewerMedia by remember { mutableStateOf<ViewableMedia?>(null) }

    // Intercept system back button/gesture so reaction changes also trigger list refresh
    BackHandler {
        if (viewModel.hasChanges) onMemoChanged() else onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.memo)) },
                navigationIcon = {
                    IconButton(onClick = { if (viewModel.hasChanges) onMemoChanged() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back))
                    }
                },
                actions = {
                    if (memo != null) {
                        if (memo.isArchived) {
                            IconButton(onClick = { viewModel.restoreMemo() }) {
                                Icon(Icons.Default.Unarchive, contentDescription = stringResource(UiR.string.unarchive))
                            }
                        } else {
                            IconButton(onClick = { onEdit(memo.uid) }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(UiR.string.edit))
                            }
                            IconButton(onClick = { viewModel.archiveMemo(onMemoChanged) }) {
                                Icon(Icons.Default.Archive, contentDescription = stringResource(UiR.string.archive))
                            }
                        }
                        IconButton(onClick = { viewModel.deleteMemo(onMemoChanged) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(UiR.string.delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> LoadingContent(modifier = Modifier.padding(padding))
            uiState.error != null -> ErrorContent(
                message = uiState.error!!,
                onRetry = viewModel::loadMemo,
                modifier = Modifier.padding(padding),
            )
            memo != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding(),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = memo.displayTime.toLocalDateTime().toRelativeTimeString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (memo.isArchived) {
                                    Surface(
                                        shape = MemosShapes.Chip,
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                    ) {
                                        Text(
                                            text = stringResource(UiR.string.archived_label),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        )
                                    }
                                }
                                VisibilityChip(visibility = memo.visibility)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        MarkdownContent(
                            content = memo.content,
                            serverUrl = serverUrl,
                            onTagClick = onTagClick,
                            onMediaClick = { url, isVideo ->
                                viewerMedia = ViewableMedia(
                                    url = url,
                                    isVideo = isVideo,
                                    filename = url.substringAfterLast("/"),
                                )
                            },
                        )

                        // Resource attachments (non-inline images and files)
                        val nonInlineResources = memo.resources.filter { res ->
                            // Show resources not already embedded as markdown images
                            !memo.content.contains("/file/${res.name}/${res.filename}")
                        }
                        if (nonInlineResources.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            ResourceSection(
                                resources = nonInlineResources,
                                serverUrl = serverUrl,
                                authHeaders = authHeaders,
                                onMediaClick = { media -> viewerMedia = media },
                            )
                        }

                        if (memo.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            TagRow(tags = memo.tags, onTagClick = onTagClick)
                        }

                        // Reactions - always show (with + button to add)
                        Spacer(modifier = Modifier.height(16.dp))
                        ReactionRow(
                            reactions = memo.reactions,
                            onReactionClick = viewModel::toggleReaction,
                            onAddReaction = viewModel::toggleEmojiPicker,
                        )

                        AnimatedVisibility(visible = uiState.showEmojiPicker) {
                            EmojiPicker(
                                onEmojiSelected = viewModel::toggleReaction,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        if (uiState.comments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(UiR.string.comments_count, uiState.comments.size),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.comments.forEach { comment ->
                                CommentItem(comment = comment, serverUrl = serverUrl)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Comment input
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        OutlinedTextField(
                            value = uiState.commentText,
                            onValueChange = viewModel::updateCommentText,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(UiR.string.add_comment)) },
                            maxLines = 3,
                            shape = MemosShapes.Input,
                        )
                        IconButton(
                            onClick = viewModel::sendComment,
                            enabled = uiState.commentText.isNotBlank() && !uiState.isSendingComment,
                        ) {
                            Icon(Icons.Default.Send, contentDescription = stringResource(UiR.string.send))
                        }
                    }
                }
            }
        }
    }

    viewerMedia?.let { media ->
        MediaViewerDialog(
            media = media,
            headers = authHeaders,
            onDismiss = { viewerMedia = null },
        )
    }
}

@Composable
private fun ResourceSection(
    resources: List<Resource>,
    serverUrl: String,
    authHeaders: Map<String, String> = emptyMap(),
    onMediaClick: ((ViewableMedia) -> Unit)? = null,
) {
    val context = LocalContext.current
    val images = resources.filter { it.isImage() }
    val videos = resources.filter { it.isVideo() }
    val files = resources.filter { !it.isImage() && !it.isVideo() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        images.forEach { resource ->
            val url = resource.displayUrl(serverUrl)
            AsyncImage(
                model = url,
                contentDescription = resource.filename,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(MemosShapes.Chip)
                    .clickable {
                        onMediaClick?.invoke(
                            ViewableMedia(url = url, isVideo = false, filename = resource.filename, size = resource.size)
                        )
                    },
                contentScale = ContentScale.FillWidth,
            )
        }
        videos.forEach { resource ->
            val videoUrl = resource.displayUrl(serverUrl)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(MemosShapes.Chip)
                    .clickable {
                        onMediaClick?.invoke(
                            ViewableMedia(url = videoUrl, isVideo = true, filename = resource.filename, size = resource.size)
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = videoUrl,
                    contentDescription = resource.filename,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
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
        files.forEach { resource ->
            val fileUrl = resource.displayUrl(serverUrl)
            val uriHandler = LocalUriHandler.current
            Surface(
                shape = MemosShapes.Card,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.clickable {
                    uriHandler.openUri(fileUrl)
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = resource.filename,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = formatFileSize(resource.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { downloadFile(context, fileUrl, resource.filename, authHeaders) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = stringResource(UiR.string.download),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun downloadFile(
    context: Context,
    url: String,
    filename: String,
    headers: Map<String, String>,
) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(filename)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        headers.forEach { (key, value) ->
            request.addRequestHeader(key, value)
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, context.getString(UiR.string.downloading, filename), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(UiR.string.download_failed), Toast.LENGTH_SHORT).show()
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}

@Composable
private fun CommentItem(comment: com.whtis.memosly.core.model.Memo, serverUrl: String = "") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = comment.displayTime.toLocalDateTime().toRelativeTimeString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        MarkdownContent(content = comment.content, serverUrl = serverUrl)
    }
}
