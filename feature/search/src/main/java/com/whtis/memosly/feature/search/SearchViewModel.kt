package com.whtis.memosly.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.whtis.memosly.core.common.AnalyticsHelper
import com.whtis.memosly.core.data.repository.MemoRepository
import com.whtis.memosly.core.data.repository.TagRepository
import com.whtis.memosly.core.model.Memo
import com.whtis.memosly.core.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    private val tagRepository: TagRepository,
    private val analyticsHelper: AnalyticsHelper,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: Flow<PagingData<Memo>> = _query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(PagingData.empty())
            else memoRepository.getMemosPaged("content.contains(\"$query\")")
        }
        .cachedIn(viewModelScope)

    init {
        loadTags()
    }

    private fun loadTags() {
        viewModelScope.launch {
            try {
                _tags.value = tagRepository.listTags()
            } catch (_: Exception) {}
        }
    }

    fun updateQuery(query: String) {
        _query.value = query
        if (query.isNotBlank()) {
            analyticsHelper.logEvent("memo_search", mapOf("query_length" to query.length.toString()))
        }
    }
}
