package com.batallagroup.myco.domain.model

enum class MessageStatus {
    SENDING,    // En cola local, buscando ruta
    IN_TRANSIT, // Retransmitido, en camino
    DELIVERED,  // Confirmado entregado
    EXPIRED,    // No entregado en 24h
    FAILED      // Error local
}
