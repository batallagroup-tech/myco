package com.batallagroup.myco.domain.model

data class Message(
    val id: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val timestamp: Long,
    val expiresAt: Long,
    val status: MessageStatus,
    val hopCount: Int = 0,
    val isOutgoing: Boolean = false,
    val transportType: String = "RELAY",
    val isSos: Boolean = false,
    val isRead: Boolean = true
)
