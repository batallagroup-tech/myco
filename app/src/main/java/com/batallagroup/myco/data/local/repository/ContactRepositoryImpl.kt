package com.batallagroup.myco.data.local.repository

import com.batallagroup.myco.data.local.dao.ContactDao
import com.batallagroup.myco.data.local.entity.ContactEntity
import com.batallagroup.myco.domain.model.Contact
import com.batallagroup.myco.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {

    override fun getAllContacts(): Flow<List<Contact>> =
        dao.getAllContacts().map { it.map { e -> e.toDomain() } }

    override suspend fun getContactById(userId: String): Contact? =
        dao.getContactById(userId)?.toDomain()

    override suspend fun insertContact(contact: Contact) =
        dao.insertContact(ContactEntity.fromDomain(contact))

    override suspend fun updateLastSeen(userId: String, timestamp: Long) =
        dao.updateLastSeen(userId, timestamp)

    override suspend fun deleteContact(userId: String) =
        dao.deleteContact(userId)
}
