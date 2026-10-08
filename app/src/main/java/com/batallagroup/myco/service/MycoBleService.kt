package com.batallagroup.myco.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.data.ble.BleManager
import com.batallagroup.myco.data.ble.MeshPacketProcessor
import com.batallagroup.myco.data.p2p.WifiDirectManager
import com.batallagroup.myco.data.relay.NostrRelayManager
import com.batallagroup.myco.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

@AndroidEntryPoint
class MycoBleService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "MycoBleService"
        private const val MOTION_THRESHOLD = 1.2f
        private const val STATIONARY_TIMEOUT_MS = 90_000L // 90 segundos sin movimiento = Reposo
    }

    @Inject
    lateinit var bleManager: BleManager

    @Inject
    lateinit var wifiDirectManager: WifiDirectManager

    @Inject
    lateinit var nostrRelayManager: NostrRelayManager

    @Inject
    lateinit var meshPacketProcessor: MeshPacketProcessor

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastMotionTimestamp = System.currentTimeMillis()
    private var isCurrentlyStationary = false

    override fun onCreate() {
        super.onCreate()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    Constants.NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(Constants.NOTIFICATION_ID, buildNotification())
            }

            // Iniciar todos los canales de la red mesh híbrida
            bleManager.start()
            wifiDirectManager.start()
            nostrRelayManager.start()

            // Iniciar sensor de movimiento para Ahorro Inteligente Adaptativo
            setupMotionSensor()

            // Retransmisión automática (Gateway / Puente multi-capa)
            serviceScope.launch {
                meshPacketProcessor.packetsToRelay.collect { packet ->
                    bleManager.sendPacket(packet)
                    wifiDirectManager.broadcastPacket(packet)
                    nostrRelayManager.publishPacket(packet)
                }
            }

            // Monitor periódico de estado de reposo vs movimiento
            serviceScope.launch {
                while (isActive) {
                    delay(30_000L)
                    checkStationaryStatus()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupMotionSensor() {
        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometer?.let { sensor ->
                sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                Log.d(TAG, "Sensor de acelerómetro activado para Ahorro Inteligente")
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo registrar sensor de movimiento: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt(x * x + y * y + z * z)
            val delta = abs(magnitude - SensorManager.GRAVITY_EARTH)

            if (delta > MOTION_THRESHOLD) {
                lastMotionTimestamp = System.currentTimeMillis()
                if (isCurrentlyStationary) {
                    isCurrentlyStationary = false
                    Log.d(TAG, "Movimiento detectado -> Activando escaneo Mesh en alta sensibilidad")
                    bleManager.setAdaptivePowerMode(isStationary = false)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun checkStationaryStatus() {
        val elapsed = System.currentTimeMillis() - lastMotionTimestamp
        if (elapsed > STATIONARY_TIMEOUT_MS && !isCurrentlyStationary) {
            isCurrentlyStationary = true
            Log.d(TAG, "Dispositivo en reposo (>90s) -> Cambiando a Ultra Bajo Consumo (Ahorro Inteligente)")
            bleManager.setAdaptivePowerMode(isStationary = true)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            // Ignorar
        }
        bleManager.stop()
        wifiDirectManager.stop()
        nostrRelayManager.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Myco Malla Activa")
            .setContentText("Red híbrida BLE + Wi-Fi Direct + Relays conectada")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
