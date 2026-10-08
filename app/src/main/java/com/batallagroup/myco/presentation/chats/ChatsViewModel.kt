package com.batallagroup.myco.presentation.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.data.ble.BleManager
import com.batallagroup.myco.data.ble.BleScanner
import com.batallagroup.myco.data.ble.ContactRequest
import com.batallagroup.myco.data.ble.MeshPacketProcessor
import com.batallagroup.myco.data.p2p.WifiDirectManager
import com.batallagroup.myco.data.relay.NostrRelayManager
import com.batallagroup.myco.domain.model.Contact
import com.batallagroup.myco.domain.model.Message
import com.batallagroup.myco.domain.model.MessageStatus
import com.batallagroup.myco.domain.model.MycoNode
import com.batallagroup.myco.domain.repository.ContactRepository
import com.batallagroup.myco.domain.repository.MessageRepository
import com.batallagroup.myco.domain.usecase.GetContactsUseCase
import com.batallagroup.myco.domain.usecase.GetMessagesUseCase
import com.batallagroup.myco.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationItem(
    val contact: Contact,
    val lastMessage: Message?,
    val unreadCount: Int = 0
)

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val getMessages: GetMessagesUseCase,
    private val getContacts: GetContactsUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val bleScanner: BleScanner,
    private val bleManager: BleManager,
    private val wifiDirectManager: WifiDirectManager,
    private val nostrRelayManager: NostrRelayManager,
    private val meshPacketProcessor: MeshPacketProcessor
) : ViewModel() {

    private val _rawConversations = MutableStateFlow<List<ConversationItem>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val incomingContactRequests: StateFlow<List<ContactRequest>> = meshPacketProcessor.incomingContactRequests

    private val _sentRequestNodeIds = MutableStateFlow<Set<String>>(emptySet())
    val sentRequestNodeIds: StateFlow<Set<String>> = _sentRequestNodeIds

    val existingContacts: StateFlow<List<Contact>> = getContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalUnreadCount: StateFlow<Int> = messageRepository.getTotalUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val conversations: StateFlow<List<ConversationItem>> = combine(
        _rawConversations,
        _searchQuery
    ) { items, query ->
        if (query.isBlank()) {
            items
        } else {
            val q = query.trim().lowercase()
            items.filter {
                it.contact.alias.lowercase().contains(q) ||
                it.contact.userId.lowercase().contains(q) ||
                (it.lastMessage?.content?.lowercase()?.contains(q) == true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nearbyNodes: StateFlow<List<MycoNode>> = bleScanner.detectedNodes

    init {
        viewModelScope.launch {
            combine(
                getContacts(),
                getMessages.getAllConversations()
            ) { contacts, allMessages ->
                contacts.map { contact ->
                    val contactMessages = allMessages.filter { msg ->
                        msg.senderId == contact.userId || msg.recipientId == contact.userId
                    }
                    val last = contactMessages.firstOrNull()
                    val unread = contactMessages.count { !it.isRead && !it.isOutgoing }
                    ConversationItem(contact, last, unreadCount = unread)
                }.sortedByDescending { it.lastMessage?.timestamp ?: it.contact.addedAt }
            }.collect { _rawConversations.value = it }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteConversation(contactId: String) {
        viewModelScope.launch {
            messageRepository.deleteChatHistory(contactId)
            contactRepository.deleteContact(contactId)
        }
    }

    fun sendEmergencySos(customContent: String) {
        viewModelScope.launch {
            val distressMessage = customContent.ifBlank {
                "🚨 ¡EMERGENCIA SOS! Se requiere asistencia inmediata. Transmitido a través de la red de nodos Myco."
            }
            val result = sendMessageUseCase(
                recipientId = Constants.BROADCAST_SOS_ID,
                content = distressMessage,
                transportType = Constants.TRANSPORT_RELAY,
                isSos = true
            )
            result.onSuccess { packet ->
                messageRepository.updateMessageStatus(packet.messageId, MessageStatus.IN_TRANSIT, packet.hopCount)
                bleManager.sendPacket(packet)
                wifiDirectManager.broadcastPacket(packet)
                nostrRelayManager.publishPacket(packet)
            }
        }
    }

    fun acceptContactRequest(request: ContactRequest) {
        meshPacketProcessor.acceptContactRequest(request)
    }

    fun rejectContactRequest(request: ContactRequest) {
        meshPacketProcessor.rejectContactRequest(request)
    }

    fun sendContactRequest(targetUserId: String, targetAlias: String) {
        _sentRequestNodeIds.value = _sentRequestNodeIds.value + targetUserId
        meshPacketProcessor.sendContactRequest(targetUserId, targetAlias)
    }
}
