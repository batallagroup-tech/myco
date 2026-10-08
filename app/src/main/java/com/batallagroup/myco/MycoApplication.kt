package com.batallagroup.myco

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.data.relay.NostrRelayManager
import com.batallagroup.myco.domain.usecase.GenerateIdentityUseCase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MycoApplication : Application() {

    @Inject
    lateinit var generateIdentity: GenerateIdentityUseCase

    @Inject
    lateinit var nostrRelayManager: NostrRelayManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Asegurar que la identidad criptográfica y el buzón existan siempre desde el arranque
        try {
            generateIdentity()
            nostrRelayManager.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                "Myco Red Mesh",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene activa la red Myco en segundo plano"
            }

            val messagesChannel = NotificationChannel(
                Constants.MESSAGES_CHANNEL_ID,
                "Mensajes de Myco",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de nuevos mensajes recibidos"
                enableVibration(true)
                enableLights(true)
            }

            val sosChannel = NotificationChannel(
                Constants.SOS_CHANNEL_ID,
                "🚨 Alertas de Emergencia SOS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas críticas y emisiones de auxilio en la red Myco"
                enableVibration(true)
                enableLights(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(messagesChannel)
            manager.createNotificationChannel(sosChannel)
        }
    }
}
