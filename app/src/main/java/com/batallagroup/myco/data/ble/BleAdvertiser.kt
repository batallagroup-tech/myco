package com.batallagroup.myco.data.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import android.util.Log
import com.batallagroup.myco.core.Constants
import javax.inject.Inject

class BleAdvertiser @Inject constructor() {
    private val tag = "MycoBleAdvertiser"
    private var advertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(tag, "Advertising iniciado")
            isAdvertising = true
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(tag, "Advertising falló: $errorCode")
            isAdvertising = false
        }
    }

    fun startAdvertising(bluetoothAdapter: BluetoothAdapter) {
        if (isAdvertising) return
        advertiser = bluetoothAdapter.bluetoothLeAdvertiser ?: run {
            Log.e(tag, "Dispositivo no soporta BLE advertising")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(Constants.MYCO_SERVICE_UUID))
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    fun stopAdvertising() {
        if (!isAdvertising) return
        advertiser?.stopAdvertising(advertiseCallback)
        isAdvertising = false
        Log.d(tag, "Advertising detenido")
    }
}
