package com.batallagroup.myco.domain.usecase

import com.batallagroup.myco.domain.model.Message
import com.batallagroup.myco.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    operator fun invoke(contactId: String): Flow<List<Message>> =
        repository.getMessagesForContact(contactId)

    fun getAllConversations(): Flow<List<Message>> =
        repository.getAllConversations()
}
