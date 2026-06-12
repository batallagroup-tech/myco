package com.batallagroup.myco.presentation.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batallagroup.myco.data.ble.BleScanner
import com.batallagroup.myco.domain.model.Contact
import com.batallagroup.myco.domain.model.Message
import com.batallagroup.myco.domain.model.MycoNode
import com.batallagroup.myco.domain.usecase.GetContactsUseCase
import com.batallagroup.myco.domain.usecase.GetMessagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationItem(
    val contact: Contact,
    val lastMessage: Message?
)

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val getMessages: GetMessagesUseCase,
    private val getContacts: GetContactsUseCase,
    private val bleScanner: BleScanner
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ConversationItem>>(emptyList())
    val conversations: StateFlow<List<ConversationItem>> = _conversations

    val nearbyNodes: StateFlow<List<MycoNode>> = bleScanner.detectedNodes

    init {
        viewModelScope.launch {
            combine(
                getContacts(),
                getMessages.getAllConversations()
            ) { contacts, lastMessages ->
                contacts.map { contact ->
                    val last = lastMessages.firstOrNull { msg ->
                        msg.senderId == contact.userId || msg.recipientId == contact.userId
                    }
                    ConversationItem(contact, last)
                }.sortedByDescending { it.lastMessage?.timestamp ?: it.contact.addedAt }
            }.collect { _conversations.value = it }
        }
    }
}
