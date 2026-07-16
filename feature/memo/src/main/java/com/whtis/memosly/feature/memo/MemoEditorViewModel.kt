package com.whtis.memosly.feature.memo

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whtis.memosly.core.common.AnalyticsHelper
import com.whtis.memosly.core.common.SharedMediaBuffer
import com.whtis.memosly.core.common.extractTags
import com.whtis.memosly.core.data.repository.MemoRepository
import com.whtis.memosly.core.data.repository.ResourceRepository
import com.whtis.memosly.core.data.repository.TagRepository
import com.whtis.memosly.core.model.Resource
import com.whtis.memosly.core.model.Visibility
import com.whtis.memosly.core.network.TokenManager
import com.whtis.memosly.core.ui.R as UiR
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Cap on files accepted from a single share. Uploads run serially so the count doesn't
 * threaten the heap; this is about the wait, since there is no per-file progress UI.
 */
const val MAX_SHARE_ITEMS: Int = 9

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
    @ApplicationContext private val context: Context,
    private val memoRepository: MemoRepository,
    private val resourceRepository: ResourceRepository,
    private val tagRepository: TagRepository,
    private val analyticsHelper: AnalyticsHelper,
    private val tokenManager: TokenManager,
    private val mediaUriReader: MediaUriReader,
    private val sharedMediaBuffer: SharedMediaBuffer,
) : ViewModel() {

    val serverUrl: String get() = tokenManager.serverUrl.value ?: ""

    private val memoId: String = savedStateHandle["memoId"] ?: ""
    private val sharedText: String = savedStateHandle["sharedText"] ?: ""
    private val hasSharedMedia: Boolean = savedStateHandle["sharedMedia"] ?: false

    /** Serializes uploads: each file is held in memory whole, so overlapping them risks OOM. */
    private val uploadMutex = Mutex()

    private val _uiState = MutableStateFlow(
        MemoEditorUiState(
            isEditMode = memoId.isNotBlank(),
            textFieldValue = if (memoId.isBlank() && sharedText.isNotBlank()) {
                TextFieldValue(sharedText, TextRange(sharedText.length))
            } else {
                TextFieldValue()
            },
        )
    )
    val uiState: StateFlow<MemoEditorUiState> = _uiState.asStateFlow()

    private var allTags: List<String> = emptyList()

    init {
        if (memoId.isNotBlank()) {
            loadMemo()
        }
        loadTags()
        if (hasSharedMedia) {
            enqueueMedia(sharedMediaBuffer.take())
        }
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

                // Link every uploaded resource to the memo via SetMemoResources/SetMemoAttachments.
                // Nothing uploaded here is embedded in the markdown content, so the attachment
                // list is the only thing that makes it show up — on web and on Android alike.
                // Images used to be excluded on v0.25+ back when they were also embedded as
                // ![](url) and would render twice (issue #5); the embed is gone, so excluding
                // them now just orphans them.
                val pendingResources = _uiState.value.pendingResources
                val existingResources = _uiState.value.existingResources
                val existingChanged = memoId.isNotBlank() && existingResources.size != memo.resources.size
                if (pendingResources.isNotEmpty() || existingChanged) {
                    try {
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

    /**
     * Uploads [uris] one at a time. Files past the size cap or ones that can't be read are
     * skipped and reported together at the end, so a single bad item doesn't sink the batch.
     */
    fun enqueueMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val accepted = uris.take(MAX_SHARE_ITEMS)
        val overflow = uris.size - accepted.size

        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }

            val tooLarge = mutableListOf<String>()
            val unreadable = mutableListOf<String>()
            var uploadError: String? = null

            accepted.forEach { uri ->
                uploadMutex.withLock {
                    when (val media = mediaUriReader.read(uri)) {
                        is MediaReadResult.TooLarge -> tooLarge += media.displayName
                        is MediaReadResult.Unreadable -> unreadable += media.displayName
                        is MediaReadResult.Success -> uploadOne(media)?.let { uploadError = it }
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isUploading = false,
                    error = uploadError ?: buildSkipMessage(tooLarge, unreadable, overflow),
                )
            }
        }
    }

    /** Returns an error message, or null once the upload has landed. */
    private suspend fun uploadOne(media: MediaReadResult.Success): String? = try {
        val resource = resourceRepository.uploadResource(media.filename, media.mimeType, media.bytes)
        val encodedFilename = java.net.URLEncoder.encode(resource.filename, "UTF-8").replace("+", "%20")
        val resourcePath = "/file/${resource.name}/$encodedFilename"

        val attachment = UploadedAttachment(
            url = resourcePath,
            filename = resource.filename,
            mimeType = media.mimeType,
        )

        val isImage = media.mimeType.startsWith("image/")
        val isVideo = media.mimeType.startsWith("video/")
        // Images and videos are linked only via SetMemoResources/SetMemoAttachments —
        // never embedded as markdown. Embedding image markdown caused duplicate
        // display on the web client (issue #5): once inline from ![](url) and again
        // from the attachment list. Both platforms render attached resources via the
        // attachment system, so a single source-of-truth avoids duplication.
        // Other files: embed as [name](url) link so users can reference them inline.
        _uiState.update { state ->
            if (isImage || isVideo) {
                state.copy(
                    attachments = state.attachments + attachment,
                    pendingResources = state.pendingResources + resource,
                )
            } else {
                val markdown = "\n\n[${resource.filename}]($resourcePath)\n"
                val current = state.textFieldValue
                val cursorPos = current.selection.start
                val newText = buildString {
                    append(current.text.substring(0, cursorPos))
                    append(markdown)
                    append(current.text.substring(cursorPos))
                }
                state.copy(
                    textFieldValue = TextFieldValue(newText, TextRange(cursorPos + markdown.length)),
                    attachments = state.attachments + attachment,
                    pendingResources = state.pendingResources + resource,
                )
            }
        }
        null
    } catch (e: Exception) {
        e.message ?: context.getString(UiR.string.upload_failed)
    }

    private fun buildSkipMessage(
        tooLarge: List<String>,
        unreadable: List<String>,
        overflow: Int,
    ): String? {
        val parts = buildList {
            if (tooLarge.isNotEmpty()) {
                add(context.getString(UiR.string.media_too_large, tooLarge.joinToString(", "), MAX_UPLOAD_MB))
            }
            if (unreadable.isNotEmpty()) {
                add(context.getString(UiR.string.media_unreadable, unreadable.joinToString(", ")))
            }
            if (overflow > 0) {
                add(context.getString(UiR.string.media_too_many, MAX_SHARE_ITEMS))
            }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString("\n")
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
