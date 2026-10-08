package com.batallagroup.myco.data.sms

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.data.ble.MeshPacketProcessor
import com.batallagroup.myco.domain.model.MycoPacket
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsFallbackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packetProcessor: MeshPacketProcessor
) {
    companion object {
        private const val TAG = "MycoSmsFallback"
        const val SMS_PREFIX = "MYCO:"
    }

    private val gson = Gson()

    /**
     * Procesa un SMS entrante que contenga un payload de Myco
     */
    fun processIncomingSms(smsBody: String) {
        if (!smsBody.startsWith(SMS_PREFIX)) return
        try {
            val payload = smsBody.removePrefix(SMS_PREFIX).trim()
            Log.d(TAG, "Recibido paquete Myco de emergencia vía SMS")
            packetProcessor.processIncoming(payload, transportType = Constants.TRANSPORT_SMS)
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando SMS de Myco: ${e.message}")
        }
    }

    /**
     * Genera un Intent para enviar un paquete cifrado por SMS al contacto cuando no hay red ni internet
     */
    fun createSmsIntent(phoneNumber: String, packet: MycoPacket): Intent {
        val packetJson = gson.toJson(packet)
        val smsBody = "$SMS_PREFIX$packetJson"
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", smsBody)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
