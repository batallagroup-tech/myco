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
}
