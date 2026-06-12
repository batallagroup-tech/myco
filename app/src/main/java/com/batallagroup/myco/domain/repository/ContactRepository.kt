package com.batallagroup.myco.domain.repository

import com.batallagroup.myco.domain.model.Contact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getAllContacts(): Flow<List<Contact>>
    suspend fun getContactById(userId: String): Contact?
    suspend fun insertContact(contact: Contact)
    suspend fun updateLastSeen(userId: String, timestamp: Long)
    suspend fun deleteContact(userId: String)
}
