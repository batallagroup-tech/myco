package com.batallagroup.myco.core.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.batallagroup.myco.R
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MycoNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Caché de deduplicación de notificaciones para evitar repeticiones de mensajes o solicitudes idénticas
    private val recentNotificationTimestamps = ConcurrentHashMap<String, Long>()

    private fun shouldNotify(key: String, cooldownMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val lastNotified = recentNotificationTimestamps[key]
        if (lastNotified != null && now - lastNotified < cooldownMs) {
            return false
        }
        recentNotificationTimestamps[key] = now
        // Limpieza periódica de claves antiguas
        if (recentNotificationTimestamps.size > 200) {
            recentNotificationTimestamps.entries.removeIf { now - it.value > 120_000L }
        }
        return true
    }

    fun showIncomingMessageNotification(
        senderId: String,
        senderAlias: String,
        messageContent: String
    ) {
        val deduplicationKey = "msg_${senderId}_${messageContent.hashCode()}"
        if (!shouldNotify(deduplicationKey, 4_000L)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_CHAT_CONTACT_ID", senderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            senderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = senderAlias.ifEmpty { "ID: ${senderId.take(8)}" }

        val notification = NotificationCompat.Builder(context, Constants.MESSAGES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_myco_notification)
            .setContentTitle("🍄 $title")
            .setContentText(messageContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(senderId.hashCode(), notification)
    }

    fun showSosEmergencyNotification(
        senderId: String,
        senderAlias: String,
        messageContent: String
    ) {
        val deduplicationKey = "sos_${senderId}_${messageContent.hashCode()}"
        if (!shouldNotify(deduplicationKey, 15_000L)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_CHAT_CONTACT_ID", Constants.BROADCAST_SOS_ID)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            Constants.BROADCAST_SOS_ID.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🚨 ALERTA SOS: ${senderAlias.ifEmpty { senderId.take(8) }}"

        // Patrón táctico de vibración en Código Morse SOS: (... --- ...)
        val sosMorsePattern = longArrayOf(0, 150, 150, 150, 150, 150, 300, 400, 200, 400, 200, 400, 300, 150, 150, 150, 150, 150)

        // Disparar vibración inmediata por hardware
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    android.os.VibrationEffect.createWaveform(sosMorsePattern, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(sosMorsePattern, -1)
            }
        } catch (e: Exception) {
            // Manejo silencioso si permisos de vibración no están listos
        }

        val notification = NotificationCompat.Builder(context, Constants.SOS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_myco_notification)
            .setContentTitle(title)
            .setContentText(messageContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText("🚨 SEÑAL DE AUXILIO:\n$messageContent\n\n(Retransmitido por nodos Myco)"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(sosMorsePattern)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(Constants.BROADCAST_SOS_ID.hashCode(), notification)
    }

    fun showContactRequestNotification(
        senderId: String,
        senderAlias: String
    ) {
        val deduplicationKey = "req_$senderId"
        if (!shouldNotify(deduplicationKey, 30_000L)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            senderId.hashCode() + 100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val name = senderAlias.ifEmpty { "Nodo ${senderId.take(4).uppercase()}" }

        val notification = NotificationCompat.Builder(context, Constants.MESSAGES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_myco_notification)
            .setContentTitle("👋 Solicitud de chat recibida")
            .setContentText("$name quiere iniciar una conversación contigo.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(senderId.hashCode() + 100, notification)
    }
}
