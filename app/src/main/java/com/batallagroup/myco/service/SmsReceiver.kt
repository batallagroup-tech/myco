package com.batallagroup.myco.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.batallagroup.myco.data.sms.SmsFallbackManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smsFallbackManager: SmsFallbackManager

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val fullBody = messages.joinToString("") { it.messageBody ?: "" }
                if (fullBody.startsWith(SmsFallbackManager.SMS_PREFIX)) {
                    Log.d("MycoSmsReceiver", "Interceptado SMS con carga cifrada Myco")
                    smsFallbackManager.processIncomingSms(fullBody)
                }
            } catch (e: Exception) {
                Log.e("MycoSmsReceiver", "Error procesando SMS recibido: ${e.message}")
            }
        }
    }
}
