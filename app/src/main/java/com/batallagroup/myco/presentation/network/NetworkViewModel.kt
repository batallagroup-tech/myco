package com.batallagroup.myco.presentation.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batallagroup.myco.data.ble.BleScanner
import com.batallagroup.myco.data.local.dao.TransitMessageDao
import com.batallagroup.myco.domain.model.MycoNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val bleScanner: BleScanner,
    private val transitMessageDao: TransitMessageDao
) : ViewModel() {

    val nearbyNodes: StateFlow<List<MycoNode>> = bleScanner.detectedNodes

    private val _transitCount = MutableStateFlow(0)
    val transitCount: StateFlow<Int> = _transitCount

    init {
        viewModelScope.launch {
            transitMessageDao.getActiveCount().collectLatest {
                _transitCount.value = it
            }
        }
    }
}
