package com.batallagroup.myco.data.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import android.util.Log
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.utils.PreferenceManager
import javax.inject.Inject

class BleAdvertiser @Inject constructor(
    private val preferenceManager: PreferenceManager
) {
    private val tag = "MycoBleAdvertiser"
    private var advertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(tag, "Advertising BLE activo con alias visible")
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

        val myUserId = preferenceManager.getString(Constants.PREF_USER_ID) ?: ""
        val myAlias = preferenceManager.getString(Constants.PREF_USER_ALIAS) ?: "Nodo"

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val infoString = "$myUserId|$myAlias".take(18)
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(Constants.MYCO_16BIT_UUID))
            .addServiceData(ParcelUuid(Constants.MYCO_16BIT_UUID), infoString.toByteArray(Charsets.UTF_8))
            .build()

        // Scan response con el UUID completo de 128-bit para compatibilidad total
        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(Constants.MYCO_SERVICE_UUID))
            .build()

        advertiser?.startAdvertising(settings, data, scanResponse, advertiseCallback)
    }

    fun stopAdvertising() {
        if (!isAdvertising) return
        advertiser?.stopAdvertising(advertiseCallback)
        isAdvertising = false
        Log.d(tag, "Advertising detenido")
    }
}
