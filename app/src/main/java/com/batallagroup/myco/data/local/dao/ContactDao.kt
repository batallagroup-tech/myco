package com.batallagroup.myco.data.local.dao

import androidx.room.*
import com.batallagroup.myco.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY alias ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE userId = :userId LIMIT 1")
    suspend fun getContactById(userId: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Query("UPDATE contacts SET lastSeenAt = :timestamp WHERE userId = :userId")
    suspend fun updateLastSeen(userId: String, timestamp: Long)

    @Query("DELETE FROM contacts WHERE userId = :userId")
    suspend fun deleteContact(userId: String)
}
