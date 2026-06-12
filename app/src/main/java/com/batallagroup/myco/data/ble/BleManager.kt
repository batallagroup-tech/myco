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
                relayPacketToNearbyNodes(packet)
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

    fun sendPacket(packet: MycoPacket) {
        scope.launch {
            relayPacketToNearbyNodes(packet)
        }
    }

    private fun relayPacketToNearbyNodes(packet: MycoPacket) {
        val packetJson = gson.toJson(packet)
        val nodes = scanner.detectedNodes.value
        Log.d(tag, "Retransmitiendo paquete ${packet.messageId} a ${nodes.size} nodos")
        // La transmisión real ocurre vía GattClient a cada nodo detectado
        // (implementación completa en versión futura de producción)
    }
}
