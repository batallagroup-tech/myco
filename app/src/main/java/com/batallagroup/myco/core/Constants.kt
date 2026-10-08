package com.batallagroup.myco.core

import java.util.UUID

object Constants {
    // UUIDs del servicio BLE Myco (únicos para la red)
    val MYCO_SERVICE_UUID: UUID = UUID.fromString("4d59434f-0000-1000-8000-00805f9b34fb")
    val MYCO_CHARACTERISTIC_UUID: UUID = UUID.fromString("4d59434f-0001-1000-8000-00805f9b34fb")
    val MYCO_16BIT_UUID: UUID = UUID.fromString("0000fd55-0000-1000-8000-00805f9b34fb")

    // Protocolo Myco v1.1
    const val MAX_HOPS = 64
    const val MESSAGE_TTL_HOURS = 24L
    const val ANTI_LOOP_CACHE_SIZE = 1000
    const val MAX_PACKET_SIZE = 512

    // Prefijos de protocolo de contacto P2P
    const val PREFIX_CONTACT_REQ = "MYCO_REQ:"
    const val PREFIX_CONTACT_ACCEPT = "MYCO_ACC:"

    // Claves de preferencias
    const val PREF_FILE_NAME = "myco_secure_prefs"
    const val PREF_USER_ID = "user_id"
    const val PREF_ENC_PUBLIC_KEY = "enc_public_key"
    const val PREF_ENC_PRIVATE_KEY = "enc_private_key"
    const val PREF_SIGN_PUBLIC_KEY = "sign_public_key"
    const val PREF_SIGN_PRIVATE_KEY = "sign_private_key"
    const val PREF_USER_ALIAS = "user_alias"
    const val PREF_ONBOARDING_DONE = "onboarding_done"
    const val PREF_RELAY_ENABLED = "relay_enabled"
    const val PREF_REJECTED_CONTACT_IDS = "rejected_contact_ids"

    // Protocolo de Transporte Myco
    const val TRANSPORT_RELAY = "RELAY"
    const val TRANSPORT_WIFI_DIRECT = "WIFI_DIRECT"
    const val TRANSPORT_BLE = "BLE"
    const val TRANSPORT_SMS = "SMS"

    // Emergencia SOS Broadcast
    const val BROADCAST_SOS_ID = "MYCO_SOS_EMERGENCY"

    // Notificaciones
    const val NOTIFICATION_CHANNEL_ID = "myco_ble_service"
    const val MESSAGES_CHANNEL_ID = "myco_messages_channel"
    const val SOS_CHANNEL_ID = "myco_sos_channel"
    const val NOTIFICATION_ID = 1001

    // QR
    const val QR_SIZE_PX = 512
}
