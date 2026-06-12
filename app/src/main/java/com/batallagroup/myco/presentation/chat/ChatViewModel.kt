package com.batallagroup.myco.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batallagroup.myco.data.ble.BleManager
import com.batallagroup.myco.domain.model.Contact
import com.batallagroup.myco.domain.model.Message
import com.batallagroup.myco.domain.repository.ContactRepository
import com.batallagroup.myco.domain.usecase.GetMessagesUseCase
import com.batallagroup.myco.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMessages: GetMessagesUseCase,
    private val sendMessage: SendMessageUseCase,
    private val contactRepository: ContactRepository,
    private val bleManager: BleManager
) : ViewModel() {

    private val contactId: String = checkNotNull(savedStateHandle["contactId"])

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _contact = MutableStateFlow<Contact?>(null)
    val contact: StateFlow<Contact?> = _contact

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError

    init {
        viewModelScope.launch {
            getMessages(contactId).collect { _messages.value = it }
        }
        viewModelScope.launch {
            _contact.value = contactRepository.getContactById(contactId)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isSending.value) return
        viewModelScope.launch {
            _isSending.value = true
            _sendError.value = null
            val result = sendMessage(contactId, text.trim())
            result.onSuccess { packet ->
                bleManager.sendPacket(packet)
            }.onFailure { e ->
                _sendError.value = e.message ?: "Error al enviar"
            }
            _isSending.value = false
        }
    }
}
