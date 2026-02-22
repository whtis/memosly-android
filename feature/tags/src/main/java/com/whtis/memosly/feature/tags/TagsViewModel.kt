package com.whtis.memosly.feature.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whtis.memosly.core.data.repository.TagRepository
import com.whtis.memosly.core.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagsUiState(
    val tags: List<Tag> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagRepository: TagRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagsUiState())
    val uiState: StateFlow<TagsUiState> = _uiState.asStateFlow()

    init {
        loadTags()
    }

    fun loadTags() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val tags = tagRepository.listTags()
                _uiState.value = TagsUiState(tags = tags, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = TagsUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun deleteTag(tag: String) {
        viewModelScope.launch {
            try {
                tagRepository.deleteTag(tag)
                loadTags()
            } catch (_: Exception) {}
        }
    }
}
