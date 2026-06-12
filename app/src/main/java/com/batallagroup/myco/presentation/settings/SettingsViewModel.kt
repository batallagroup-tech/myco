package com.batallagroup.myco.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.utils.PreferenceManager
import com.batallagroup.myco.data.local.dao.TransitMessageDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val transitMessageDao: TransitMessageDao
) : ViewModel() {

    val myUserId: String
        get() = preferenceManager.getString(Constants.PREF_USER_ID) ?: "—"

    val myEncPublicKey: String
        get() = preferenceManager.getString(Constants.PREF_ENC_PUBLIC_KEY) ?: ""

    val mySignPublicKey: String
        get() = preferenceManager.getString(Constants.PREF_SIGN_PUBLIC_KEY) ?: ""

    private val _relayEnabled = MutableStateFlow(
        preferenceManager.getBoolean(Constants.PREF_RELAY_ENABLED, true)
    )
    val relayEnabled: StateFlow<Boolean> = _relayEnabled

    private val _clearResult = MutableStateFlow<String?>(null)
    val clearResult: StateFlow<String?> = _clearResult

    fun setRelayEnabled(enabled: Boolean) {
        preferenceManager.putBoolean(Constants.PREF_RELAY_ENABLED, enabled)
        _relayEnabled.value = enabled
    }

    fun clearTransitMessages() {
        viewModelScope.launch {
            transitMessageDao.clearAll()
            _clearResult.value = "Mensajes en tránsito eliminados"
        }
    }

    fun getQrPayload(): String {
        val userId = myUserId
        val encPK = myEncPublicKey
        val signPK = mySignPublicKey
        return "myco://$userId:$encPK:$signPK"
    }
}
