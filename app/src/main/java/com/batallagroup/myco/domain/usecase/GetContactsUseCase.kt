package com.batallagroup.myco.domain.usecase

import com.batallagroup.myco.domain.model.Contact
import com.batallagroup.myco.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    private val repository: ContactRepository
) {
    operator fun invoke(): Flow<List<Contact>> = repository.getAllContacts()
}
