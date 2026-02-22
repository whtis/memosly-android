package com.whtis.memosly.feature.memo

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whtis.memosly.core.common.AnalyticsHelper
import com.whtis.memosly.core.common.extractTags
import com.whtis.memosly.core.data.repository.MemoRepository
import com.whtis.memosly.core.data.repository.ResourceRepository
import com.whtis.memosly.core.data.repository.TagRepository
import com.whtis.memosly.core.model.Resource
import com.whtis.memosly.core.model.Visibility
import com.whtis.memosly.core.network.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadedAttachment(
    val url: String,
    val filename: String,
    val mimeType: String,
)

data class MemoEditorUiState(
    val textFieldValue: TextFieldValue = TextFieldValue(),
    val visibility: Visibility = Visibility.PRIVATE,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploading: Boolean = false,
    val isEditMode: Boolean = false,
    val showPreview: Boolean = false,
    val attachments: List<UploadedAttachment> = emptyList(),
    val pendingResources: List<Resource> = emptyList(),
    val existingResources: List<Resource> = emptyList(),
    val error: String? = null,
    val tagSuggestions: List<String> = emptyList(),
    val showTagSuggestions: Boolean = false,
)

@HiltViewModel
class MemoEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memoRepository: MemoRepository,
    private val resourceRepository: ResourceRepository,
    private val tagRepository: TagRepository,
    private val analyticsHelper: AnalyticsHelper,
    private val tokenManager: TokenManager,
) : ViewModel() {

    val serverUrl: String get() = tokenManager.serverUrl.value ?: ""

    private val memoId: String = savedStateHandle["memoId"] ?: ""

    private val _uiState = MutableStateFlow(MemoEditorUiState(isEditMode = memoId.isNotBlank()))
    val uiState: StateFlow<MemoEditorUiState> = _uiState.asStateFlow()

    private var allTags: List<String> = emptyList()

    init {
        if (memoId.isNotBlank()) {
            loadMemo()
        }
        loadTags()
    }

    private fun loadMemo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val memo = memoRepository.getMemo(memoId)
                // Build attachment preview from the memo's actual resources
                // (not from markdown parsing — videos are never embedded in markdown)
                val attachments = memo.resources.map { resource ->
                    val encodedFilename = java.net.URLEncoder.encode(resource.filename, "UTF-8")
                        .replace("+", "%20")
                    UploadedAttachment(
                        url = "/file/${resource.name}/$encodedFilename",
                        filename = resource.filename,
                        mimeType = resource.type,
                    )
                }
                _uiState.value = _uiState.value.copy(
                    textFieldValue = TextFieldValue(memo.content),
                    visibility = memo.visibility,
                    isLoading = false,
                    attachments = attachments,
                    existingResources = memo.resources,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            try {
                allTags = tagRepository.listTags().map { it.name }
            } catch (_: Exception) {}
        }
    }

    fun updateContent(value: TextFieldValue) {
        _uiState.value = _uiState.value.copy(textFieldValue = value, error = null)
        updateTagSuggestions(value)
    }

    private fun updateTagSuggestions(value: TextFieldValue) {
        val cursorPos = value.selection.start
        if (cursorPos != value.selection.end || cursorPos == 0) {
            _uiState.value = _uiState.value.copy(showTagSuggestions = false, tagSuggestions = emptyList())
            return
        }

        val text = value.text
        // Walk backwards from cursor to find '#'
        var hashPos = -1
        for (i in (cursorPos - 1) downTo 0) {
            val ch = text[i]
            if (ch == '#') {
                // Check that '#' is at start of text or preceded by whitespace
                if (i == 0 || text[i - 1].isWhitespace()) {
                    hashPos = i
                }
                break
            }
            if (ch.isWhitespace() || ch == '\n') break
        }

        if (hashPos < 0) {
            _uiState.value = _uiState.value.copy(showTagSuggestions = false, tagSuggestions = emptyList())
            return
        }

        val partial = text.substring(hashPos + 1, cursorPos)
        val filtered = allTags.filter { tag ->
            tag.contains(partial, ignoreCase = true)
        }.take(5)

        _uiState.value = _uiState.value.copy(
            showTagSuggestions = filtered.isNotEmpty(),
            tagSuggestions = filtered,
        )
    }

    fun selectTag(tag: String) {
        val value = _uiState.value.textFieldValue
        val cursorPos = value.selection.start
        val text = value.text

        // Find the '#' position
        var hashPos = -1
        for (i in (cursorPos - 1) downTo 0) {
            val ch = text[i]
            if (ch == '#') {
                hashPos = i
                break
            }
            if (ch.isWhitespace() || ch == '\n') break
        }

        if (hashPos < 0) return

        val newText = buildString {
            append(text.substring(0, hashPos))
            append("#$tag ")
            append(text.substring(cursorPos))
        }
        val newCursorPos = hashPos + tag.length + 2 // #tag + space
        _uiState.value = _uiState.value.copy(
            textFieldValue = TextFieldValue(newText, TextRange(newCursorPos)),
            showTagSuggestions = false,
            tagSuggestions = emptyList(),
        )
    }

    fun setVisibility(visibility: Visibility) {
        _uiState.value = _uiState.value.copy(visibility = visibility)
    }

    fun togglePreview() {
        _uiState.value = _uiState.value.copy(showPreview = !_uiState.value.showPreview)
    }

    fun save(onSaved: (isPublic: Boolean) -> Unit) {
        val content = _uiState.value.textFieldValue.text.trim()
        val hasPendingResources = _uiState.value.pendingResources.isNotEmpty()
        if (content.isBlank() && !hasPendingResources) {
            _uiState.value = _uiState.value.copy(error = "Content cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val visibilityStr = _uiState.value.visibility.name
                val memo = if (memoId.isNotBlank()) {
                    memoRepository.updateMemo(memoId, content, visibilityStr)
                } else {
                    memoRepository.createMemo(content, visibilityStr).also {
                        analyticsHelper.logEvent("memo_create", mapOf(
                            "has_tags" to if (content.extractTags().isNotEmpty()) "true" else "false",
                        ))
                    }
                }

                // Link resources to the memo via SetMemoResources/SetMemoAttachments
                val pendingResources = _uiState.value.pendingResources
                val existingResources = _uiState.value.existingResources
                if (pendingResources.isNotEmpty() || (memoId.isNotBlank() && existingResources.size != memo.resources.size)) {
                    try {
                        // Combine existing (possibly trimmed by removals) + newly uploaded
                        val existingNames = existingResources.map { it.name }
                        val newNames = pendingResources.map { it.name }
                        val allNames = (existingNames + newNames).distinct()
                        memoRepository.setMemoResources(memo.name, allNames)
                    } catch (e: Exception) {
                        android.util.Log.e("MemoEditor", "setMemoResources failed: ${e.message}", e)
                    }
                }

                _uiState.value = _uiState.value.copy(isSaving = false)
                val isPublic = _uiState.value.visibility == Visibility.PUBLIC
                onSaved(isPublic)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save",
                )
            }
        }
    }

    fun uploadMedia(filename: String, mimeType: String, bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true)
            try {
                val resource = resourceRepository.uploadResource(filename, mimeType, bytes)
                val encodedFilename = java.net.URLEncoder.encode(resource.filename, "UTF-8").replace("+", "%20")
                val resourcePath = "/file/${resource.name}/$encodedFilename"
                val isImage = mimeType.startsWith("image/")

                val attachment = UploadedAttachment(
                    url = resourcePath,
                    filename = resource.filename,
                    mimeType = mimeType,
                )
                val newPending = _uiState.value.pendingResources + resource

                val isVideo = mimeType.startsWith("video/")
                // Images: embed as ![name](url) for inline rendering on both web and Android
                // Videos: do NOT embed in markdown — web renders ![](video) as broken <img>
                //   Instead, videos are linked via SetMemoResources API and displayed
                //   through the attachment system on both platforms
                // Other files: embed as [name](url) link
                if (!isVideo) {
                    val markdown = if (isImage) {
                        "\n\n![${resource.filename}]($resourcePath)\n"
                    } else {
                        "\n\n[${resource.filename}]($resourcePath)\n"
                    }
                    val current = _uiState.value.textFieldValue
                    val cursorPos = current.selection.start
                    val newText = buildString {
                        append(current.text.substring(0, cursorPos))
                        append(markdown)
                        append(current.text.substring(cursorPos))
                    }
                    val newCursor = cursorPos + markdown.length
                    _uiState.value = _uiState.value.copy(
                        textFieldValue = TextFieldValue(newText, TextRange(newCursor)),
                        isUploading = false,
                        attachments = _uiState.value.attachments + attachment,
                        pendingResources = newPending,
                    )
                } else {
                    // Video: only track in attachments (shown in preview strip) and
                    // pendingResources (linked to memo on save via API)
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        attachments = _uiState.value.attachments + attachment,
                        pendingResources = newPending,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = e.message ?: "Upload failed",
                )
            }
        }
    }

    fun removeAttachment(index: Int) {
        val state = _uiState.value
        val attachment = state.attachments.getOrNull(index) ?: return

        // Remove from attachments display list
        val newAttachments = state.attachments.toMutableList().apply { removeAt(index) }

        // Remove matching resource from pendingResources (match by filename)
        val newPending = state.pendingResources.toMutableList().apply {
            val resIndex = indexOfFirst { it.filename == attachment.filename }
            if (resIndex >= 0) removeAt(resIndex)
        }

        // Remove matching resource from existingResources (match by filename)
        val newExisting = state.existingResources.toMutableList().apply {
            val resIndex = indexOfFirst { it.filename == attachment.filename }
            if (resIndex >= 0) removeAt(resIndex)
        }

        // Remove embedded markdown from content (images: ![name](url), files: [name](url))
        val escapedUrl = Regex.escape(attachment.url)
        val markdownRegex = Regex("""(\n?\n?)!?\[[^\]]*\]\($escapedUrl\)\n?""")
        val currentText = state.textFieldValue.text
        val newText = markdownRegex.replace(currentText, "").trimStart('\n')

        _uiState.value = state.copy(
            textFieldValue = TextFieldValue(newText, TextRange(newText.length)),
            attachments = newAttachments,
            pendingResources = newPending,
            existingResources = newExisting,
        )
    }
}
