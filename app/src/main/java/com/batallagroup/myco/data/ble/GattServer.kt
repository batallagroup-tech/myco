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
    private val receiveBuffer = StringBuilder()

    private val serverCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(tag, "Nodo conectado: ${device.address}")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(tag, "Nodo desconectado: ${device.address}")
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
                val chunk = String(value, Charsets.UTF_8)
                receiveBuffer.append(chunk)

                // Detección de fin de paquete (JSON completo)
                val buffered = receiveBuffer.toString()
                if (buffered.trimEnd().endsWith("}")) {
                    packetProcessor.processIncoming(buffered.trim())
                    receiveBuffer.clear()
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
