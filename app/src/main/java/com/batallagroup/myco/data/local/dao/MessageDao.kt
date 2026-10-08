package com.batallagroup.myco.data.local.dao

import androidx.room.*
import com.batallagroup.myco.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE senderId = :contactId OR recipientId = :contactId ORDER BY timestamp ASC")
    fun getMessagesForContact(contactId: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages WHERE id IN (
            SELECT MAX(id) FROM messages GROUP BY CASE
                WHEN isOutgoing = 1 THEN recipientId ELSE senderId END
        ) ORDER BY timestamp DESC
    """)
    fun getAllConversations(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status, hopCount = :hopCount WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String, hopCount: Int)

    @Query("DELETE FROM messages WHERE expiresAt < :now")
    suspend fun deleteExpiredMessages(now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM messages WHERE id IN (:messageIds)")
    suspend fun deleteMessagesByIds(messageIds: List<String>)

    @Query("UPDATE messages SET isRead = 1 WHERE (senderId = :contactId OR recipientId = :contactId) AND isRead = 0")
    suspend fun markMessagesAsRead(contactId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE (senderId = :contactId OR recipientId = :contactId) AND isRead = 0 AND isOutgoing = 0")
    fun getUnreadCountForContact(contactId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE isRead = 0 AND isOutgoing = 0")
    fun getTotalUnreadCount(): Flow<Int>

    @Query("SELECT * FROM messages WHERE isOutgoing = 1 AND (status = 'PENDING' OR status = 'FAILED') ORDER BY timestamp ASC")
    suspend fun getPendingOutgoingMessages(): List<MessageEntity>

    @Query("UPDATE messages SET status = 'FAILED' WHERE isOutgoing = 1 AND status IN ('SENDING', 'IN_TRANSIT', 'PENDING')")
    suspend fun markTransitMessagesAsFailed()

    @Query("DELETE FROM messages WHERE isOutgoing = 1 AND status IN ('SENDING', 'IN_TRANSIT', 'PENDING') AND timestamp < :olderThan")
    suspend fun deleteStaleOutgoingTransitMessages(olderThan: Long)

    @Query("DELETE FROM messages WHERE senderId = :contactId OR recipientId = :contactId")
    suspend fun deleteMessagesForContact(contactId: String)
}
