package com.whtis.memosly.core.data.repository

import androidx.paging.PagingData
import com.whtis.memosly.core.model.Memo
import com.whtis.memosly.core.model.Reaction
import kotlinx.coroutines.flow.Flow

interface MemoRepository {
    fun getMemosPaged(filter: String? = null, state: String? = null): Flow<PagingData<Memo>>
    suspend fun getMemo(id: String): Memo
    suspend fun createMemo(content: String, visibility: String? = null): Memo
    suspend fun updateMemo(id: String, content: String, visibility: String? = null): Memo
    suspend fun deleteMemo(id: String)
    suspend fun archiveMemo(id: String): Memo
    suspend fun restoreMemo(id: String): Memo
    suspend fun getComments(memoId: String): List<Memo>
    suspend fun createComment(memoId: String, content: String): Memo
    suspend fun getReactions(memoId: String): List<Reaction>
    suspend fun upsertReaction(memoId: String, reactionType: String): Reaction
    suspend fun deleteReaction(reactionId: Int, memoId: String)
    suspend fun setMemoResources(memoName: String, resourceNames: List<String>)
}
