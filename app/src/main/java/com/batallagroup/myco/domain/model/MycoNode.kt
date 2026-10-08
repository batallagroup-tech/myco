package com.batallagroup.myco.domain.model

data class MycoNode(
    val deviceAddress: String,
    val nodeId: String,
    val rssi: Int,
    val lastSeen: Long = System.currentTimeMillis(),
    val messagesRelayed: Int = 0,
    val alias: String = ""
) {
    val proximityLabel: String
        get() = when {
            rssi >= -65 -> "Muy cerca (< 5m)"
            rssi >= -80 -> "Media distancia (5-15m)"
            else -> "Límite de alcance (> 15m)"
        }

    val proximityColorHex: Long
        get() = when {
            rssi >= -65 -> 0xFF22C55E // Verde intenso
            rssi >= -80 -> 0xFFEAB308 // Amarillo ámbar
            else -> 0xFF94A3B8        // Gris slate
        }

    val signalBars: String
        get() = when {
            rssi >= -65 -> "📶 Excelente"
            rssi >= -80 -> "📶 Buena"
            else -> "📶 Débil"
        }
}
