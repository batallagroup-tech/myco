package com.batallagroup.myco.presentation.chat

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.crypto.CryptoManager
import com.batallagroup.myco.core.utils.PreferenceManager
import com.batallagroup.myco.data.ble.BleManager
import com.batallagroup.myco.data.p2p.WifiDirectManager
import com.batallagroup.myco.data.relay.NostrRelayManager
import com.batallagroup.myco.data.sms.SmsFallbackManager
import com.batallagroup.myco.domain.model.Contact
import com.batallagroup.myco.domain.model.Message
import com.batallagroup.myco.domain.model.MessageStatus
import com.batallagroup.myco.domain.model.MycoPacket
import com.batallagroup.myco.domain.repository.ContactRepository
import com.batallagroup.myco.domain.repository.MessageRepository
import com.batallagroup.myco.domain.usecase.GetMessagesUseCase
import com.batallagroup.myco.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMessages: GetMessagesUseCase,
    private val sendMessage: SendMessageUseCase,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val bleManager: BleManager,
    private val wifiDirectManager: WifiDirectManager,
    private val nostrRelayManager: NostrRelayManager,
    private val smsFallbackManager: SmsFallbackManager,
    private val preferenceManager: PreferenceManager,
    private val cryptoManager: CryptoManager
) : ViewModel() {

    val contactId: String = checkNotNull(savedStateHandle["contactId"])

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _contact = MutableStateFlow<Contact?>(null)
    val contact: StateFlow<Contact?> = _contact

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError

    private val _selectedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMessageIds: StateFlow<Set<String>> = _selectedMessageIds

    init {
        viewModelScope.launch {
            messageRepository.markMessagesAsRead(contactId)
            getMessages(contactId).collect {
                _messages.value = it
                messageRepository.markMessagesAsRead(contactId)
            }
        }
        viewModelScope.launch {
            _contact.value = contactRepository.getContactById(contactId)
        }
    }

    fun toggleSelectMessage(messageId: String) {
        val current = _selectedMessageIds.value
        _selectedMessageIds.value = if (current.contains(messageId)) {
            current - messageId
        } else {
            current + messageId
        }
    }

    fun clearSelection() {
        _selectedMessageIds.value = emptySet()
    }

    fun deleteSelectedMessages() {
        val idsToDelete = _selectedMessageIds.value.toList()
        if (idsToDelete.isEmpty()) return
        viewModelScope.launch {
            messageRepository.deleteMessages(idsToDelete)
            clearSelection()
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            messageRepository.deleteChatHistory(contactId)
            clearSelection()
        }
    }

    fun getSelectedMessagesText(): String {
        val ids = _selectedMessageIds.value
        return _messages.value
            .filter { ids.contains(it.id) }
            .joinToString("\n") { it.content }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isSending.value) return
        viewModelScope.launch {
            _isSending.value = true
            _sendError.value = null
            val isSosChannel = (contactId == com.batallagroup.myco.core.Constants.BROADCAST_SOS_ID)
            val result = sendMessage(contactId, text.trim(), isSos = isSosChannel)
            result.onSuccess { packet ->
                messageRepository.updateMessageStatus(packet.messageId, MessageStatus.IN_TRANSIT, packet.hopCount)
                bleManager.sendPacket(packet)
                wifiDirectManager.broadcastPacket(packet)
                nostrRelayManager.publishPacket(packet)
            }.onFailure { e ->
                _sendError.value = e.message ?: "Error al enviar"
            }
            _isSending.value = false
        }
    }

    fun updateContactPhone(phone: String) {
        val currentContact = _contact.value ?: return
        viewModelScope.launch {
            val updated = currentContact.copy(phoneNumber = phone.trim())
            contactRepository.insertContact(updated)
            _contact.value = updated
        }
    }

    fun updateCustomNickname(nickname: String) {
        val currentContact = _contact.value ?: return
        viewModelScope.launch {
            val updated = currentContact.copy(customNickname = nickname.trim())
            contactRepository.insertContact(updated)
            _contact.value = updated
        }
    }

    fun createSmsFallbackIntent(text: String): Intent? {
        val phone = _contact.value?.phoneNumber
        if (phone.isNullOrBlank()) return null
        val myId = preferenceManager.getString(Constants.PREF_USER_ID) ?: ""
        val myEncPK = preferenceManager.getString(Constants.PREF_ENC_PUBLIC_KEY) ?: ""
        val packet = MycoPacket(
            messageId = UUID.randomUUID().toString(),
            senderId = myId,
            recipientId = contactId,
            hopCount = 0,
            maxHops = 1,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 86400000,
            encryptedContent = cryptoManager.toBase64(text.toByteArray(Charsets.UTF_8)),
            senderSignature = "",
            senderEncPublicKey = myEncPK,
            transportType = Constants.TRANSPORT_SMS
        )
        // Guardar también en la base de datos local
        viewModelScope.launch {
            val localMsg = Message(
                id = packet.messageId,
                senderId = myId,
                recipientId = contactId,
                content = text,
                timestamp = packet.createdAt,
                expiresAt = packet.expiresAt,
                status = MessageStatus.IN_TRANSIT,
                isOutgoing = true,
                transportType = Constants.TRANSPORT_SMS
            )
            messageRepository.insertMessage(localMsg)
        }
        return smsFallbackManager.createSmsIntent(phone, packet)
    }
}
