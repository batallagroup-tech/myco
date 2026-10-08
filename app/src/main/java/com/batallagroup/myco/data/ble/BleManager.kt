package com.batallagroup.myco.data.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.batallagroup.myco.domain.model.MycoNode
import com.batallagroup.myco.domain.model.MycoPacket
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val advertiser: BleAdvertiser,
    private val scanner: BleScanner,
    private val gattServer: GattServer,
    private val packetProcessor: MeshPacketProcessor
) {
    private val tag = "MycoBleMgr"
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var bluetoothAdapter: BluetoothAdapter? = null

    val detectedNodes: StateFlow<List<MycoNode>> = scanner.detectedNodes

    // Métricas en vivo de actividad de la malla Mesh
    val blePacketsSent = MutableStateFlow(0)
    val blePacketsReceived = MutableStateFlow(0)
    val meshHopsRelayed = MutableStateFlow(0)
    val lastMeshActivity = MutableStateFlow("Radio BLE activo")

    fun start() {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return
        bluetoothAdapter = manager.adapter ?: return

        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(tag, "Bluetooth desactivado")
            return
        }

        gattServer.start(manager)
        bluetoothAdapter?.let {
            advertiser.startAdvertising(it)
            scanner.startScanning(it)
        }

        // Retransmitir paquetes pendientes a nodos cercanos
        scope.launch {
            packetProcessor.packetsToRelay.collect { packet ->
                meshHopsRelayed.value += 1
                lastMeshActivity.value = "Retransmitiendo salto ${packet.hopCount} (Nodo ${packet.senderId.take(4)})"
                relayPacketToNearbyNodes(packet)
            }
        }

        // Auto-flush de cola de salida cuando se detecta un nodo cercano
        scope.launch {
            detectedNodes.collect { nodes ->
                if (nodes.isNotEmpty()) {
                    lastMeshActivity.value = "${nodes.size} nodo(s) cercano(s) detectado(s)"
                    packetProcessor.flushPendingOutbox()
                }
            }
        }

        Log.d(tag, "BLE Mesh activo")
    }

    fun stop() {
        bluetoothAdapter?.let {
            advertiser.stopAdvertising()
            scanner.stopScanning(it)
        }
        gattServer.stop()
        Log.d(tag, "BLE Mesh detenido")
    }

    fun restartAdvertising() {
        bluetoothAdapter?.let {
            advertiser.stopAdvertising()
            advertiser.startAdvertising(it)
            Log.d(tag, "Advertising BLE reiniciado con nuevo alias")
        }
    }

    fun setAdaptivePowerMode(isStationary: Boolean) {
        val adapter = bluetoothAdapter ?: return
        val scanMode = if (isStationary) {
            android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_POWER
        } else {
            android.bluetooth.le.ScanSettings.SCAN_MODE_BALANCED
        }
        scanner.updateScanMode(adapter, scanMode)
        Log.d(tag, "Ahorro adaptativo: isStationary=$isStationary, scanMode=$scanMode")
    }

    fun sendPacket(packet: MycoPacket) {
        scope.launch {
            blePacketsSent.value += 1
            lastMeshActivity.value = "Emitiendo paquete a ${packet.recipientId.take(6)}"
            relayPacketToNearbyNodes(packet)
        }
    }

    private fun relayPacketToNearbyNodes(packet: MycoPacket) {
        val packetJson = gson.toJson(packet)
        val devices = scanner.getDiscoveredBluetoothDevices()
        Log.d(tag, "Retransmitiendo paquete ${packet.messageId} por BLE a ${devices.size} dispositivos Bluetooth")
        for (device in devices) {
            connectAndSendPacket(device, packetJson)
        }
    }

    private fun connectAndSendPacket(device: android.bluetooth.BluetoothDevice, jsonPayload: String) {
        val framedPayload = "<MYCO_MSG>$jsonPayload</MYCO_MSG>"
        var negotiatedChunkSize = 240
        var chunks = framedPayload.chunked(negotiatedChunkSize)
        var chunkIndex = 0

        val callback = object : android.bluetooth.BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: android.bluetooth.BluetoothGatt, status: Int, newState: Int) {
                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                    Log.d(tag, "Conectado a GATT de ${device.address}. Negociando MTU alta...")
                    // Negociar MTU alta (512 bytes) para transferencias rápidas y completas
                    val requested = gatt.requestMtu(512)
                    if (!requested) {
                        gatt.discoverServices()
                    }
                } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(tag, "Desconectado de GATT de ${device.address}")
                    try { gatt.close() } catch (e: Exception) {}
                }
            }

            override fun onMtuChanged(gatt: android.bluetooth.BluetoothGatt, mtu: Int, status: Int) {
                Log.d(tag, "MTU BLE negociada con éxito: $mtu bytes (status $status)")
                negotiatedChunkSize = (mtu - 3).coerceIn(20, 480)
                chunks = framedPayload.chunked(negotiatedChunkSize)
                gatt.discoverServices()
            }

            override fun onServicesDiscovered(gatt: android.bluetooth.BluetoothGatt, status: Int) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(com.batallagroup.myco.core.Constants.MYCO_SERVICE_UUID)
                    val characteristic = service?.getCharacteristic(com.batallagroup.myco.core.Constants.MYCO_CHARACTERISTIC_UUID)
                    if (characteristic != null && chunks.isNotEmpty()) {
                        writeNextChunk(gatt, characteristic)
                    } else {
                        try { gatt.disconnect() } catch (e: Exception) {}
                    }
                } else {
                    try { gatt.disconnect() } catch (e: Exception) {}
                }
            }

            override fun onCharacteristicWrite(
                gatt: android.bluetooth.BluetoothGatt,
                characteristic: android.bluetooth.BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    chunkIndex++
                    if (chunkIndex < chunks.size) {
                        writeNextChunk(gatt, characteristic)
                    } else {
                        Log.d(tag, "Paquete completo entregado por BLE a ${device.address}")
                        lastMeshActivity.value = "✓ Paquete entregado a ${device.address.takeLast(5)}"
                        try { gatt.disconnect() } catch (e: Exception) {}
                    }
                } else {
                    Log.w(tag, "Fallo al escribir chunk $chunkIndex a ${device.address}: status $status")
                    try { gatt.disconnect() } catch (e: Exception) {}
                }
            }

            private fun writeNextChunk(gatt: android.bluetooth.BluetoothGatt, characteristic: android.bluetooth.BluetoothGattCharacteristic) {
                try {
                    val chunkBytes = chunks[chunkIndex].toByteArray(Charsets.UTF_8)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeCharacteristic(characteristic, chunkBytes, android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    } else {
                        characteristic.value = chunkBytes
                        characteristic.writeType = android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        gatt.writeCharacteristic(characteristic)
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Error escribiendo chunk BLE: ${e.message}")
                }
            }
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                device.connectGatt(context, false, callback, android.bluetooth.BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, callback)
            }
        } catch (e: SecurityException) {
            Log.e(tag, "Permiso Bluetooth denegado: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "Error conectando GATT a ${device.address}: ${e.message}")
        }
    }
}
