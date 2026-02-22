package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.InboxMessage
import com.whtis.memosly.core.network.api.InboxApi
import com.whtis.memosly.core.network.dto.UpdateInboxRequest
import com.whtis.memosly.core.network.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboxRepositoryImpl @Inject constructor(
    private val inboxApi: InboxApi,
) : InboxRepository {

    override suspend fun listInbox(): List<InboxMessage> =
        inboxApi.listInbox().inboxes.map { it.toDomain() }

    override suspend fun updateInboxStatus(name: String, status: String): InboxMessage {
        val id = name.substringAfterLast("/")
        return inboxApi.updateInbox(id, UpdateInboxRequest(status)).toDomain()
    }
}
