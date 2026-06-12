package com.batallagroup.myco.data.local.dao

import androidx.room.*
import com.batallagroup.myco.data.local.entity.TransitMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransitMessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransitMessage(message: TransitMessageEntity): Long

    @Query("SELECT * FROM transit_messages WHERE expiresAt > :now")
    suspend fun getActiveTransitMessages(now: Long = System.currentTimeMillis()): List<TransitMessageEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM transit_messages WHERE messageId = :messageId)")
    suspend fun exists(messageId: String): Boolean

    @Query("DELETE FROM transit_messages WHERE expiresAt < :now")
    suspend fun deleteExpired(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM transit_messages")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM transit_messages WHERE expiresAt > :now")
    fun getActiveCount(now: Long = System.currentTimeMillis()): Flow<Int>
}
