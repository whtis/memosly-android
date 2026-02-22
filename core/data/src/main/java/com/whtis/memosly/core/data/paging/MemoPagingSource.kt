package com.whtis.memosly.core.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.whtis.memosly.core.model.Memo
import com.whtis.memosly.core.network.api.MemoApi
import com.whtis.memosly.core.network.toDomain

private const val TAG = "MemoPagingSource"

class MemoPagingSource(
    private val memoApi: MemoApi,
    private val filter: String?,
    private val memoState: String? = null,
) : PagingSource<String, Memo>() {

    override fun getRefreshKey(state: PagingState<String, Memo>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Memo> {
        return try {
            val pageToken = params.key
            Log.d(TAG, "load() called: pageToken=$pageToken, pageSize=${params.loadSize}, filter=$filter, state=$memoState")
            val response = memoApi.listMemos(
                pageSize = params.loadSize,
                pageToken = pageToken,
                filter = filter,
                state = memoState,
            )
            Log.d(TAG, "load() success: ${response.memos.size} memos, nextPageToken=${response.nextPageToken}")
            val domainMemos = response.memos.mapIndexed { index, dto ->
                try {
                    dto.toDomain().also { memo ->
                        Log.d(TAG, "  memo[$index]: uid=${memo.uid}, name=${memo.name}, content=${memo.content.take(50)}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "  memo[$index] toDomain() FAILED: name=${dto.name}", e)
                    throw e
                }
            }
            LoadResult.Page(
                data = domainMemos,
                prevKey = null,
                nextKey = response.nextPageToken?.takeIf { it.isNotEmpty() },
            )
        } catch (e: Exception) {
            Log.e(TAG, "load() FAILED", e)
            LoadResult.Error(e)
        }
    }
}
