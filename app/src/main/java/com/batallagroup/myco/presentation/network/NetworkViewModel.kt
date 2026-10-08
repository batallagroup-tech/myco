package com.batallagroup.myco.presentation.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batallagroup.myco.data.ble.BleScanner
import com.batallagroup.myco.data.local.dao.TransitMessageDao
import com.batallagroup.myco.domain.model.Contact
import com.batallagroup.myco.domain.model.MycoNode
import com.batallagroup.myco.domain.usecase.GetContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.batallagroup.myco.data.ble.BleManager
import com.batallagroup.myco.data.ble.MeshPacketProcessor
import com.batallagroup.myco.data.relay.NostrRelayManager

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val bleScanner: BleScanner,
    private val bleManager: BleManager,
    private val transitMessageDao: TransitMessageDao,
    private val nostrRelayManager: NostrRelayManager,
    private val meshPacketProcessor: MeshPacketProcessor,
    private val getContacts: GetContactsUseCase
) : ViewModel() {

    val nearbyNodes: StateFlow<List<MycoNode>> = bleScanner.detectedNodes
    val connectedRelaysCount: StateFlow<Int> = nostrRelayManager.connectedRelaysCount
    val isOnline: StateFlow<Boolean> = nostrRelayManager.isOnline

    // Métricas en vivo del monitor de diagnóstico Mesh
    val blePacketsSent: StateFlow<Int> = bleManager.blePacketsSent
    val blePacketsReceived: StateFlow<Int> = bleManager.blePacketsReceived
    val meshHopsRelayed: StateFlow<Int> = bleManager.meshHopsRelayed
    val lastMeshActivity: StateFlow<String> = bleManager.lastMeshActivity

    val existingContacts: StateFlow<List<Contact>> = getContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sentRequestNodeIds = MutableStateFlow<Set<String>>(emptySet())
    val sentRequestNodeIds: StateFlow<Set<String>> = _sentRequestNodeIds

    private val _transitCount = MutableStateFlow(0)
    val transitCount: StateFlow<Int> = _transitCount

    init {
        viewModelScope.launch {
            transitMessageDao.getActiveCount().collectLatest {
                _transitCount.value = it
            }
        }
    }

    fun syncRelays() {
        nostrRelayManager.connectToAllRelays()
    }

    fun sendContactRequest(targetUserId: String, targetAlias: String) {
        _sentRequestNodeIds.value = _sentRequestNodeIds.value + targetUserId
        meshPacketProcessor.sendContactRequest(targetUserId, targetAlias)
    }
}
