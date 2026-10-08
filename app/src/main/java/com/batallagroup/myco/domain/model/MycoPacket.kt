package com.batallagroup.myco.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Paquete de la red Myco v1.1.
 * Serialización compacta optimizada para BLE Mesh (reduce 70% el tamaño del paquete).
 */
data class MycoPacket(
    @SerializedName("m", alternate = ["messageId"])
    val messageId: String,

    @SerializedName("s", alternate = ["senderId"])
    val senderId: String,

    @SerializedName("r", alternate = ["recipientId"])
    val recipientId: String,

    @SerializedName("h", alternate = ["hopCount"])
    val hopCount: Int,

    @SerializedName("x", alternate = ["maxHops"])
    val maxHops: Int,

    @SerializedName("t", alternate = ["createdAt"])
    val createdAt: Long,

    @SerializedName("e", alternate = ["expiresAt"])
    val expiresAt: Long,

    @SerializedName("c", alternate = ["encryptedContent"])
    val encryptedContent: String,   // Base64 del contenido cifrado

    @SerializedName("g", alternate = ["senderSignature"])
    val senderSignature: String,    // Base64 de la firma

    @SerializedName("k", alternate = ["senderEncPublicKey"])
    val senderEncPublicKey: String, // Clave pública del remitente para ECDH

    @SerializedName("p", alternate = ["transportType"])
    val transportType: String = "RELAY",

    @SerializedName("o", alternate = ["isSos"])
    val isSos: Boolean = false
)
