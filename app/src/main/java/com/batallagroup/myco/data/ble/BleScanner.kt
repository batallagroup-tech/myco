package com.batallagroup.myco.data.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.utils.PreferenceManager
import com.batallagroup.myco.domain.model.MycoNode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleScanner @Inject constructor(
    private val preferenceManager: PreferenceManager
) {
    private val tag = "MycoBleScanner"
    private var isScanning = false

    private val _detectedNodes = MutableStateFlow<List<MycoNode>>(emptyList())
    val detectedNodes: StateFlow<List<MycoNode>> = _detectedNodes

    // Mapa deduplicado indexado estrictamente por nodeId único (evita repetición por MACs rotativas)
    private val nodeMap = ConcurrentHashMap<String, MycoNode>()
    private val deviceMap = ConcurrentHashMap<String, android.bluetooth.BluetoothDevice>()

    fun getDiscoveredBluetoothDevices(): List<android.bluetooth.BluetoothDevice> = deviceMap.values.toList()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val scanRecord = result.scanRecord

            // Comprobar si el dispositivo pertenece a la red Myco
            val hasServiceUuid = scanRecord?.serviceUuids?.any {
                it.uuid == Constants.MYCO_SERVICE_UUID || it.uuid == Constants.MYCO_16BIT_UUID
            } == true

            val serviceData16 = scanRecord?.getServiceData(ParcelUuid(Constants.MYCO_16BIT_UUID))
            val serviceData128 = scanRecord?.getServiceData(ParcelUuid(Constants.MYCO_CHARACTERISTIC_UUID))
            val rawServiceData = serviceData16 ?: serviceData128

            // Si no tiene el UUID ni service data de Myco, ignorar dispositivos Bluetooth no relacionados
            if (!hasServiceUuid && rawServiceData == null) {
                return
            }

            deviceMap[device.address] = device

            var resolvedNodeId = device.address.replace(":", "").takeLast(8).lowercase()
            var resolvedAlias = ""

            try {
                if (rawServiceData != null) {
                    val infoStr = String(rawServiceData, Charsets.UTF_8)
                    val parts = infoStr.split("|")
                    if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                        resolvedNodeId = parts[0].trim()
                    }
                    if (parts.size > 1 && parts[1].isNotBlank()) {
                        resolvedAlias = parts[1].trim()
                    }
                }
            } catch (e: Exception) {
                // Fallback a identificador local
            }

            // Ignorar mi propio nodo para no detectarme a mí mismo como nodo cercano
            val myId = preferenceManager.getString(Constants.PREF_USER_ID)
            if (myId != null && resolvedNodeId.equals(myId, ignoreCase = true)) {
                return
            }

            val existing = nodeMap[resolvedNodeId]
            val finalAlias = if (resolvedAlias.isNotBlank()) resolvedAlias else (existing?.alias ?: "")

            val node = MycoNode(
                deviceAddress = device.address,
                nodeId = resolvedNodeId,
                rssi = result.rssi,
                lastSeen = System.currentTimeMillis(),
                alias = finalAlias
            )

            // Indexar por nodeId para garantizar que NUNCA se duplique el mismo dispositivo
            nodeMap[resolvedNodeId] = node
            _detectedNodes.value = nodeMap.values.sortedByDescending { it.rssi }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(tag, "Escaneo falló: $errorCode")
            isScanning = false
        }
    }

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
    private var cleanupJob: kotlinx.coroutines.Job? = null

    fun startScanning(bluetoothAdapter: BluetoothAdapter, scanMode: Int = ScanSettings.SCAN_MODE_LOW_LATENCY) {
        if (isScanning) return
        val scanner = bluetoothAdapter.bluetoothLeScanner ?: return

        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(Constants.MYCO_16BIT_UUID)).build(),
            ScanFilter.Builder().setServiceUuid(ParcelUuid(Constants.MYCO_SERVICE_UUID)).build()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(scanMode)
            .setReportDelay(0)
            .build()

        try {
            scanner.startScan(filters, settings, scanCallback)
            isScanning = true
            Log.d(tag, "Escaneo BLE iniciado con filtros específicos")
        } catch (e: Exception) {
            Log.w(tag, "Fallo al iniciar escaneo con filtros, intentando escaneo general: ${e.message}")
            try {
                scanner.startScan(emptyList(), settings, scanCallback)
                isScanning = true
            } catch (e2: Exception) {
                Log.e(tag, "Fallo total de escaneo BLE: ${e2.message}")
            }
        }

        // Iniciar limpieza periódica en vivo de nodos inactivos (cada 3 segundos, TTL de 15s)
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isScanning) {
                kotlinx.coroutines.delay(3000L)
                clearStaleNodes(maxAgeMs = 15_000L)
            }
        }
    }

    fun updateScanMode(bluetoothAdapter: BluetoothAdapter, newScanMode: Int) {
        if (isScanning) {
            stopScanning(bluetoothAdapter)
        }
        startScanning(bluetoothAdapter, newScanMode)
    }

    fun stopScanning(bluetoothAdapter: BluetoothAdapter) {
        cleanupJob?.cancel()
        cleanupJob = null
        if (!isScanning) return
        try {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(tag, "Error deteniendo escaneo BLE: ${e.message}")
        }
        isScanning = false
        nodeMap.clear()
        deviceMap.clear()
        _detectedNodes.value = emptyList()
        Log.d(tag, "Escaneo BLE detenido y lista de nodos limpiada")
    }

    fun clearStaleNodes(maxAgeMs: Long = 15_000L) {
        val now = System.currentTimeMillis()
        var removedAny = false
        val iterator = nodeMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.lastSeen > maxAgeMs) {
                deviceMap.remove(entry.value.deviceAddress)
                iterator.remove()
                removedAny = true
                Log.d(tag, "Nodo expirado / fuera de alcance eliminado: ${entry.key}")
            }
        }
        if (removedAny) {
            _detectedNodes.value = nodeMap.values.sortedByDescending { it.rssi }
        }
    }
}
