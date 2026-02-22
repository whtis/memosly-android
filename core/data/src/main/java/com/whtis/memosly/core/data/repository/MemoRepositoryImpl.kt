package com.whtis.memosly.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.whtis.memosly.core.data.paging.MemoPagingSource
import com.whtis.memosly.core.model.Memo
import com.whtis.memosly.core.model.Reaction
import com.whtis.memosly.core.network.ServerVersion
import com.whtis.memosly.core.network.TokenManager
import com.whtis.memosly.core.network.api.MemoApi
import com.whtis.memosly.core.network.dto.CreateMemoRequest
import com.whtis.memosly.core.network.dto.MemoUpdateFields
import com.whtis.memosly.core.network.dto.ReactionFields
import com.whtis.memosly.core.network.dto.ResourceRef
import com.whtis.memosly.core.network.dto.SetMemoAttachmentsRequest
import com.whtis.memosly.core.network.dto.SetMemoResourcesRequest
import com.whtis.memosly.core.network.dto.UpsertReactionRequest
import com.whtis.memosly.core.network.toDomain
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoRepositoryImpl @Inject constructor(
    private val memoApi: MemoApi,
    private val tokenManager: TokenManager,
) : MemoRepository {

    private val version: ServerVersion
        get() = tokenManager.serverVersion.value

    override fun getMemosPaged(filter: String?, state: String?): Flow<PagingData<Memo>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = { MemoPagingSource(memoApi, filter, state) },
    ).flow

    override suspend fun getMemo(id: String): Memo =
        memoApi.getMemo(id).toDomain()

    override suspend fun createMemo(content: String, visibility: String?): Memo =
        memoApi.createMemo(CreateMemoRequest(content = content, visibility = visibility)).toDomain()

    override suspend fun updateMemo(id: String, content: String, visibility: String?): Memo {
        val updateFields = mutableListOf<String>()
        val fields = MemoUpdateFields(
            content = content.also { updateFields.add("content") },
            visibility = visibility?.also { updateFields.add("visibility") },
        )
        return memoApi.updateMemo(
            id,
            memo = fields,
            updateMask = updateFields.joinToString(","),
        ).toDomain()
    }

    override suspend fun deleteMemo(id: String) =
        memoApi.deleteMemo(id)

    override suspend fun archiveMemo(id: String): Memo =
        memoApi.updateMemo(
            id,
            memo = MemoUpdateFields(state = "ARCHIVED"),
            updateMask = "state",
        ).toDomain()

    override suspend fun restoreMemo(id: String): Memo =
        memoApi.updateMemo(
            id,
            memo = MemoUpdateFields(state = "NORMAL"),
            updateMask = "state",
        ).toDomain()

    override suspend fun getComments(memoId: String): List<Memo> =
        memoApi.listMemoComments(memoId).memos.map { it.toDomain() }

    override suspend fun createComment(memoId: String, content: String): Memo =
        memoApi.createMemoComment(
            memoId,
            CreateMemoRequest(content = content),
        ).toDomain()

    override suspend fun getReactions(memoId: String): List<Reaction> =
        memoApi.listMemoReactions(memoId).reactions.map { it.toDomain() }

    override suspend fun upsertReaction(memoId: String, reactionType: String): Reaction =
        memoApi.upsertMemoReaction(
            memoId,
            UpsertReactionRequest(
                reaction = ReactionFields(
                    contentId = "memos/$memoId",
                    reactionType = reactionType,
                ),
            ),
        ).toDomain()

    override suspend fun deleteReaction(reactionId: Int, memoId: String) =
        when (version) {
            ServerVersion.V026 -> memoApi.deleteMemoReaction(memoId, reactionId)
            else -> memoApi.deleteReaction(reactionId)
        }

    override suspend fun setMemoResources(memoName: String, resourceNames: List<String>) {
        val memoUid = memoName.substringAfterLast("/")
        val refs = resourceNames.map { ResourceRef(name = it) }
        when (version) {
            ServerVersion.V024 -> {
                memoApi.setMemoResources(memoUid, SetMemoResourcesRequest(refs))
            }
            // v0.25+: use attachments endpoint
            else -> {
                memoApi.setMemoAttachments(memoUid, SetMemoAttachmentsRequest(refs))
            }
        }
    }
}
