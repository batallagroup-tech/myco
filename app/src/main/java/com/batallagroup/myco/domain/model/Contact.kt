package com.batallagroup.myco.domain.model

data class Contact(
    val userId: String,          // Primeros 8 hex del hash de clave pública
    val encPublicKey: String,    // Clave pública de cifrado (base64)
    val signPublicKey: String,   // Clave pública de firma (base64)
    val alias: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = 0L
)
