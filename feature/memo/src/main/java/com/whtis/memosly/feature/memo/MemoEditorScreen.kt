package com.whtis.memosly.feature.memo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.whtis.memosly.core.ui.R as UiR
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whtis.memosly.core.markdown.MarkdownContent
import com.whtis.memosly.core.markdown.MarkdownToolbar
import com.whtis.memosly.core.model.Visibility
import com.whtis.memosly.core.ui.component.LoadingContent
import com.whtis.memosly.core.ui.component.VisibilityChip
import com.whtis.memosly.core.ui.theme.MemosShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemoEditorScreen(
    onBack: () -> Unit,
    onSaved: (isPublic: Boolean) -> Unit,
    viewModel: MemoEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val serverUrl = viewModel.serverUrl

    // Media picker launcher
    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val contentResolver = context.contentResolver
        val filename = run {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex >= 0) it.getString(nameIndex) else null
            } ?: "file_${System.currentTimeMillis()}"
        }
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        val rawMimeType = contentResolver.getType(uri)
        val mimeType = rawMimeType?.takeIf { it != "application/octet-stream" }
            ?: android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(filename.substringAfterLast('.', "").lowercase())
            ?: guessMimeTypeFromBytes(bytes)
            ?: rawMimeType
            ?: "application/octet-stream"
        // Ensure filename has an extension so renderers can detect media type from the URL
        val filenameWithExt = if (!filename.contains('.')) {
            val ext = android.webkit.MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
            if (ext != null) "$filename.$ext" else filename
        } else filename
        viewModel.uploadMedia(filenameWithExt, mimeType, bytes)
    }

    if (uiState.isLoading) {
        LoadingContent()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (uiState.isEditMode) UiR.string.edit_memo else UiR.string.new_memo))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back))
                    }
                },
                actions = {
                    // Visibility selector in top bar
                    var showMenu by remember { mutableStateOf(false) }
                    TextButton(onClick = { showMenu = true }) {
                        VisibilityChip(visibility = uiState.visibility)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        Visibility.entries.filter { it != Visibility.UNKNOWN }.forEach { v ->
                            DropdownMenuItem(
                                text = { Text(v.name) },
                                onClick = {
                                    viewModel.setVisibility(v)
                                    showMenu = false
                                },
                            )
                        }
                    }

                    IconButton(onClick = viewModel::togglePreview) {
                        Icon(
                            Icons.Default.Preview,
                            contentDescription = stringResource(UiR.string.preview),
                            tint = if (uiState.showPreview) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.save(onSaved) },
                        enabled = !uiState.isSaving && !uiState.isUploading,
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(UiR.string.save),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            if (uiState.error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            if (uiState.showPreview) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    MarkdownContent(content = uiState.textFieldValue.text)
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    TextField(
                        value = uiState.textFieldValue,
                        onValueChange = viewModel::updateContent,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text(stringResource(UiR.string.write_memo_placeholder)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge,
                    )

                    // Upload progress overlay
                    if (uiState.isUploading) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = MemosShapes.Chip,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(UiR.string.uploading_media),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            // Attachment preview strip
            if (uiState.attachments.isNotEmpty()) {
                AttachmentPreviewStrip(
                    attachments = uiState.attachments,
                    serverUrl = serverUrl,
                    onRemove = viewModel::removeAttachment,
                )
            }

            // Tag suggestions
            AnimatedVisibility(visible = uiState.showTagSuggestions) {
                TagSuggestionBar(
                    suggestions = uiState.tagSuggestions,
                    onTagSelected = viewModel::selectTag,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                MarkdownToolbar(
                    textFieldValue = uiState.textFieldValue,
                    onValueChange = viewModel::updateContent,
                    onAttachClick = { mediaLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TagSuggestionBar(
    suggestions: List<String>,
    onTagSelected: (String) -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            suggestions.forEach { tag ->
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { onTagSelected(tag) },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MemosShapes.Chip,
                ) {
                    Text(
                        text = "#$tag",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewStrip(
    attachments: List<UploadedAttachment>,
    serverUrl: String,
    onRemove: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEachIndexed { index, attachment ->
            val resolvedUrl = if (attachment.url.startsWith("/")) "$serverUrl${attachment.url}" else attachment.url
            Box {
                when {
                    attachment.mimeType.startsWith("image/") -> {
                        AsyncImage(
                            model = resolvedUrl,
                            contentDescription = attachment.filename,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(MemosShapes.Chip),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    attachment.mimeType.startsWith("video/") -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(MemosShapes.Chip),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = resolvedUrl,
                                contentDescription = attachment.filename,
                                modifier = Modifier.size(56.dp),
                                contentScale = ContentScale.Crop,
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Surface(
                            shape = MemosShapes.Chip,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.height(56.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.InsertDriveFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = attachment.filename,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 80.dp),
                                )
                            }
                        }
                    }
                }
                // Close button overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clickable { onRemove(index) },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(UiR.string.remove_attachment),
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun guessMimeTypeFromBytes(bytes: ByteArray): String? {
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
