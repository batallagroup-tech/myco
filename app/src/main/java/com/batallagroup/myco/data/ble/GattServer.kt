package com.batallagroup.myco.data.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.batallagroup.myco.core.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GattServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packetProcessor: MeshPacketProcessor
) {
    private val tag = "MycoGattServer"
    private var gattServer: BluetoothGattServer? = null
    private val deviceBuffers = java.util.concurrent.ConcurrentHashMap<String, StringBuilder>()

    private val serverCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(tag, "Nodo conectado: ${device.address}")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(tag, "Nodo desconectado: ${device.address}")
                deviceBuffers.remove(device.address)
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (characteristic.uuid == Constants.MYCO_CHARACTERISTIC_UUID) {
                val buffer = deviceBuffers.getOrPut(device.address) { StringBuilder() }
                val chunk = String(value, Charsets.UTF_8)
                buffer.append(chunk)

                val buffered = buffer.toString()

                // 1. Detección por delimitador de marco robusto <MYCO_MSG> ... </MYCO_MSG>
                if (buffered.contains("<MYCO_MSG>") && buffered.contains("</MYCO_MSG>")) {
                    val startIdx = buffered.indexOf("<MYCO_MSG>") + "<MYCO_MSG>".length
                    val endIdx = buffered.indexOf("</MYCO_MSG>")
                    if (endIdx > startIdx) {
                        val payload = buffered.substring(startIdx, endIdx).trim()
                        packetProcessor.processIncoming(payload, transportType = Constants.TRANSPORT_BLE)
                        buffer.delete(0, endIdx + "</MYCO_MSG>".length)
                    }
                } else if (buffered.startsWith("{") && buffered.trimEnd().endsWith("}")) {
                    // 2. Soporte para JSON directo
                    try {
                        packetProcessor.processIncoming(buffered.trim(), transportType = Constants.TRANSPORT_BLE)
                        deviceBuffers.remove(device.address)
                    } catch (e: Exception) {
                        // Esperar más fragmentos si aún no está completo
                    }
                } else if (buffered.length > 8192) {
                    // Prevenir desbordamiento de memoria por datos corruptos
                    deviceBuffers.remove(device.address)
                }
            }

            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }
    }

    fun start(bluetoothManager: BluetoothManager) {
        gattServer = bluetoothManager.openGattServer(context, serverCallback)

        val service = BluetoothGattService(
            Constants.MYCO_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        val characteristic = BluetoothGattCharacteristic(
            Constants.MYCO_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristic)
        gattServer?.addService(service)
        Log.d(tag, "GATT Server iniciado")
    }

    fun stop() {
        gattServer?.close()
        gattServer = null
        Log.d(tag, "GATT Server detenido")
    }
}
