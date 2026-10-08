package com.batallagroup.myco.domain.repository

import com.batallagroup.myco.domain.model.Message
import com.batallagroup.myco.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessagesForContact(contactId: String): Flow<List<Message>>
    fun getAllConversations(): Flow<List<Message>>
    suspend fun insertMessage(message: Message)
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus, hopCount: Int = 0)
    suspend fun deleteExpiredMessages()
    suspend fun markMessagesAsRead(contactId: String)
    fun getTotalUnreadCount(): Flow<Int>
    suspend fun getPendingOutgoingMessages(): List<Message>
    suspend fun getMessageById(messageId: String): Message?
    suspend fun deleteMessages(ids: List<String>)
    suspend fun deleteChatHistory(contactId: String)
    suspend fun markTransitMessagesAsFailed()
}
