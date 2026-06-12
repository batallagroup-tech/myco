package com.batallagroup.myco.domain.model

/**
 * Paquete de la red Myco v1.0.
 * Los nodos intermedios solo ven senderId, recipientId, hopCount y expiresAt.
 * El contenido (encryptedContent) es opaco para ellos.
 */
data class MycoPacket(
    val messageId: String,
    val senderId: String,
    val recipientId: String,
    val hopCount: Int,
    val maxHops: Int,
    val createdAt: Long,
    val expiresAt: Long,
    val encryptedContent: String,   // Base64 del contenido cifrado
    val senderSignature: String,    // Base64 de la firma Ed25519
    val senderEncPublicKey: String  // Clave pública del remitente para ECDH
)
