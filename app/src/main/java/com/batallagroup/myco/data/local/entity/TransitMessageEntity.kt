package com.batallagroup.myco.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Mensajes ajenos que este nodo está retransmitiendo. */
@Entity(tableName = "transit_messages")
data class TransitMessageEntity(
    @PrimaryKey val messageId: String,
    val senderId: String,
    val recipientId: String,
    val hopCount: Int,
    val maxHops: Int,
    val createdAt: Long,
    val expiresAt: Long,
    val encryptedContent: String,
    val senderSignature: String,
    val senderEncPublicKey: String,
    val receivedAt: Long = System.currentTimeMillis()
)
