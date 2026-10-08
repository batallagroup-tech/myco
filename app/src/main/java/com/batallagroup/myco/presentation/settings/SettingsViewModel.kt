package com.batallagroup.myco.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.utils.PreferenceManager
import com.batallagroup.myco.data.local.dao.TransitMessageDao
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val transitMessageDao: TransitMessageDao,
    private val messageDao: com.batallagroup.myco.data.local.dao.MessageDao
) : ViewModel() {

    private val gson = Gson()

    val myUserId: String
        get() = preferenceManager.getString(Constants.PREF_USER_ID) ?: "—"

    val myEncPublicKey: String
        get() = preferenceManager.getString(Constants.PREF_ENC_PUBLIC_KEY) ?: ""

    val mySignPublicKey: String
        get() = preferenceManager.getString(Constants.PREF_SIGN_PUBLIC_KEY) ?: ""

    private val _userAlias = MutableStateFlow(
        preferenceManager.getString(Constants.PREF_USER_ALIAS) ?: ""
    )
    val userAlias: StateFlow<String> = _userAlias

    private val _relayEnabled = MutableStateFlow(
        preferenceManager.getBoolean(Constants.PREF_RELAY_ENABLED, true)
    )
    val relayEnabled: StateFlow<Boolean> = _relayEnabled

    private val _clearResult = MutableStateFlow<String?>(null)
    val clearResult: StateFlow<String?> = _clearResult

    fun setUserAlias(alias: String) {
        val trimmed = alias.trim()
        preferenceManager.putString(Constants.PREF_USER_ALIAS, trimmed)
        _userAlias.value = trimmed
    }

    fun setRelayEnabled(enabled: Boolean) {
        preferenceManager.putBoolean(Constants.PREF_RELAY_ENABLED, enabled)
        _relayEnabled.value = enabled
    }

    fun clearTransitMessages() {
        viewModelScope.launch {
            transitMessageDao.clearAll()
            messageDao.markTransitMessagesAsFailed()
            _clearResult.value = "Mensajes en tránsito limpiados y cancelados"
        }
    }

    fun getQrPayload(): String {
        val userId = myUserId
        val encPK = myEncPublicKey
        val signPK = mySignPublicKey
        val alias = _userAlias.value
        return "myco://$userId:$encPK:$signPK:$alias"
    }

    fun exportIdentityBackup(): String {
        val backupObj = JsonObject().apply {
            addProperty("user_id", myUserId)
            addProperty("user_alias", _userAlias.value)
            addProperty("enc_public_key", myEncPublicKey)
            addProperty("enc_private_key", preferenceManager.getString(Constants.PREF_ENC_PRIVATE_KEY) ?: "")
            addProperty("sign_public_key", mySignPublicKey)
            addProperty("sign_private_key", preferenceManager.getString(Constants.PREF_SIGN_PRIVATE_KEY) ?: "")
            addProperty("created_at", System.currentTimeMillis())
        }
        return backupObj.toString()
    }

    fun restoreIdentityBackup(json: String): Boolean {
        return try {
            val obj = gson.fromJson(json, JsonObject::class.java)
            val userId = obj.get("user_id")?.asString ?: return false
            val encPK = obj.get("enc_public_key")?.asString ?: return false
            val encSK = obj.get("enc_private_key")?.asString ?: return false
            val signPK = obj.get("sign_public_key")?.asString ?: return false
            val signSK = obj.get("sign_private_key")?.asString ?: return false
            val alias = obj.get("user_alias")?.asString ?: ""

            preferenceManager.putString(Constants.PREF_USER_ID, userId)
            preferenceManager.putString(Constants.PREF_ENC_PUBLIC_KEY, encPK)
            preferenceManager.putString(Constants.PREF_ENC_PRIVATE_KEY, encSK)
            preferenceManager.putString(Constants.PREF_SIGN_PUBLIC_KEY, signPK)
            preferenceManager.putString(Constants.PREF_SIGN_PRIVATE_KEY, signSK)
            preferenceManager.putString(Constants.PREF_USER_ALIAS, alias)
            _userAlias.value = alias
            true
        } catch (e: Exception) {
            false
        }
    }
}
