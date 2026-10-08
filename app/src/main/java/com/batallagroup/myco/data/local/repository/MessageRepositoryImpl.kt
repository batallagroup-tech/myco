package com.batallagroup.myco.data.local.repository

import com.batallagroup.myco.data.local.dao.MessageDao
import com.batallagroup.myco.data.local.entity.MessageEntity
import com.batallagroup.myco.domain.model.Message
import com.batallagroup.myco.domain.model.MessageStatus
import com.batallagroup.myco.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val dao: MessageDao
) : MessageRepository {

    override fun getMessagesForContact(contactId: String): Flow<List<Message>> =
        dao.getMessagesForContact(contactId).map { it.map { e -> e.toDomain() } }

    override fun getAllConversations(): Flow<List<Message>> =
        dao.getAllConversations().map { it.map { e -> e.toDomain() } }

    override suspend fun insertMessage(message: Message) =
        dao.insertMessage(MessageEntity.fromDomain(message))

    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus, hopCount: Int) =
        dao.updateMessageStatus(messageId, status.name, hopCount)

    override suspend fun deleteExpiredMessages() =
        dao.deleteExpiredMessages()

    override suspend fun markMessagesAsRead(contactId: String) =
        dao.markMessagesAsRead(contactId)

    override fun getTotalUnreadCount(): Flow<Int> =
        dao.getTotalUnreadCount()

    override suspend fun getPendingOutgoingMessages(): List<Message> =
        dao.getPendingOutgoingMessages().map { it.toDomain() }

    override suspend fun getMessageById(messageId: String): Message? =
        dao.getMessageById(messageId)?.toDomain()

    override suspend fun deleteMessages(ids: List<String>) =
        dao.deleteMessagesByIds(ids)

    override suspend fun deleteChatHistory(contactId: String) =
        dao.deleteMessagesForContact(contactId)

    override suspend fun markTransitMessagesAsFailed() =
        dao.markTransitMessagesAsFailed()
}
