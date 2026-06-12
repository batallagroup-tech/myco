package com.batallagroup.myco.data.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.domain.model.MycoNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleScanner @Inject constructor() {
    private val tag = "MycoBleScanner"
    private var isScanning = false

    private val _detectedNodes = MutableStateFlow<List<MycoNode>>(emptyList())
    val detectedNodes: StateFlow<List<MycoNode>> = _detectedNodes

    private val nodeMap = mutableMapOf<String, MycoNode>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val node = MycoNode(
                deviceAddress = device.address,
                nodeId = device.address.replace(":", "").takeLast(8).lowercase(),
                rssi = result.rssi
            )
            nodeMap[device.address] = node
            _detectedNodes.value = nodeMap.values.toList()
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(tag, "Escaneo falló: $errorCode")
            isScanning = false
        }
    }

    fun startScanning(bluetoothAdapter: BluetoothAdapter) {
        if (isScanning) return
        val scanner = bluetoothAdapter.bluetoothLeScanner ?: return

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(Constants.MYCO_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
        isScanning = true
        Log.d(tag, "Escaneo BLE iniciado")
    }

    fun stopScanning(bluetoothAdapter: BluetoothAdapter) {
        if (!isScanning) return
        bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false
        Log.d(tag, "Escaneo BLE detenido")
    }

    fun clearStaleNodes(maxAgeMs: Long = 60_000L) {
        val now = System.currentTimeMillis()
        nodeMap.entries.removeIf { now - it.value.lastSeen > maxAgeMs }
        _detectedNodes.value = nodeMap.values.toList()
    }
}
