package com.batallagroup.myco.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val addContact: AddContactUseCase
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    private val _addResult = MutableStateFlow<String?>(null)
    val addResult: StateFlow<String?> = _addResult

    init {
        viewModelScope.launch {
            getContacts().collect { _contacts.value = it }
        }
    }

    fun addContactFromQr(qrPayload: String) {
        viewModelScope.launch {
            try {
                // Formato QR: myco://userId:encPK:signPK
                val parts = qrPayload.removePrefix("myco://").split(":")
                if (parts.size < 3) {
                    _addResult.value = "QR inválido"
                    return@launch
                }
                val contact = Contact(
                    userId = parts[0],
                    encPublicKey = parts[1],
                    signPublicKey = parts[2]
                )
                addContact(contact).onSuccess {
                    _addResult.value = "Contacto agregado: ${contact.userId}"
                }.onFailure {
                    _addResult.value = "Error: ${it.message}"
                }
            } catch (e: Exception) {
                _addResult.value = "Error al procesar QR"
            }
        }
    }
}
