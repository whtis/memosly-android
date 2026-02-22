package com.whtis.memosly.core.data.repository

import com.whtis.memosly.core.model.InboxMessage

interface InboxRepository {
    suspend fun listInbox(): List<InboxMessage>
    suspend fun updateInboxStatus(name: String, status: String): InboxMessage
}
