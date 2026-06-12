package com.batallagroup.myco.domain.usecase

import com.batallagroup.myco.domain.model.Contact
import com.batallagroup.myco.domain.repository.ContactRepository
import javax.inject.Inject

class AddContactUseCase @Inject constructor(
    private val repository: ContactRepository
) {
    suspend operator fun invoke(contact: Contact): Result<Unit> {
        return try {
            repository.insertContact(contact)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
