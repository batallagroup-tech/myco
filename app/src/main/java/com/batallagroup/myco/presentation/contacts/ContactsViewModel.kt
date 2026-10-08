package com.batallagroup.myco.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batallagroup.myco.data.ble.ContactRequest
import com.batallagroup.myco.data.ble.MeshPacketProcessor
import com.batallagroup.myco.domain.model.Contact
import com.batallagroup.myco.domain.usecase.AddContactUseCase
import com.batallagroup.myco.domain.usecase.GetContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getContacts: GetContactsUseCase,
    private val addContact: AddContactUseCase,
    private val meshPacketProcessor: MeshPacketProcessor
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    private val _addResult = MutableStateFlow<String?>(null)
    val addResult: StateFlow<String?> = _addResult

    val incomingContactRequests: StateFlow<List<ContactRequest>> = meshPacketProcessor.incomingContactRequests

    init {
        viewModelScope.launch {
            getContacts().collect { _contacts.value = it }
        }
    }

    fun addContactFromQr(qrPayload: String) {
        viewModelScope.launch {
            try {
                // Formato QR: myco://userId:encPK:signPK:alias
                val parts = qrPayload.removePrefix("myco://").split(":")
                if (parts.size < 3) {
                    _addResult.value = "QR inválido"
                    return@launch
                }
                val userId = parts[0].trim()
                val encPK = parts[1].trim()
                val signPK = parts[2].trim()
                val alias = if (parts.size > 3) parts[3].trim() else ""

                val contact = Contact(
                    userId = userId,
                    alias = alias.ifEmpty { "Contacto ${userId.take(4).uppercase()}" },
                    encPublicKey = encPK,
                    signPublicKey = signPK
                )
                addContact(contact).onSuccess {
                    _addResult.value = "Contacto agregado: ${contact.alias}"
                }.onFailure {
                    _addResult.value = "Error: ${it.message}"
                }
            } catch (e: Exception) {
                _addResult.value = "Error al procesar QR"
            }
        }
    }

    fun addContactManual(userId: String, alias: String, phoneNumber: String = "", encPublicKey: String = "", signPublicKey: String = "") {
        viewModelScope.launch {
            try {
                val cleanId = userId.trim()
                if (cleanId.isEmpty()) {
                    _addResult.value = "El ID no puede estar vacío"
                    return@launch
                }
                val contact = Contact(
                    userId = cleanId,
                    alias = alias.trim(),
                    phoneNumber = phoneNumber.trim(),
                    encPublicKey = encPublicKey.ifEmpty { cleanId },
                    signPublicKey = signPublicKey.ifEmpty { cleanId }
                )
                addContact(contact).onSuccess {
                    _addResult.value = "Contacto agregado: ${contact.alias.ifEmpty { contact.userId }}"
                }.onFailure {
                    _addResult.value = "Error: ${it.message}"
                }
            } catch (e: Exception) {
                _addResult.value = "Error al agregar contacto"
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
        meshPacketProcessor.sendContactRequest(targetUserId, targetAlias)
    }
}
