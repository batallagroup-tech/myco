package com.batallagroup.myco.domain.model

data class Contact(
    val userId: String,          // Primeros 8 hex del hash de clave pública
    val encPublicKey: String,    // Clave pública de cifrado (base64)
    val signPublicKey: String,   // Clave pública de firma (base64)
    val alias: String = "",
    val phoneNumber: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = 0L,
    val customNickname: String = ""
) {
    val displayName: String
        get() = when {
            customNickname.isNotBlank() -> customNickname
            alias.isNotBlank() -> alias
            else -> "Nodo ${userId.take(4).uppercase()}"
        }
}
