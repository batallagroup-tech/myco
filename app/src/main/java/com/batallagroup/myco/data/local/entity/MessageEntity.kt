package com.batallagroup.myco.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.batallagroup.myco.domain.model.Message
import com.batallagroup.myco.domain.model.MessageStatus

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val timestamp: Long,
    val expiresAt: Long,
    val status: String,
    val hopCount: Int,
    val isOutgoing: Boolean
) {
    fun toDomain() = Message(
        id = id,
        senderId = senderId,
        recipientId = recipientId,
        content = content,
        timestamp = timestamp,
        expiresAt = expiresAt,
        status = MessageStatus.valueOf(status),
        hopCount = hopCount,
        isOutgoing = isOutgoing
    )

    companion object {
        fun fromDomain(m: Message) = MessageEntity(
            id = m.id,
            senderId = m.senderId,
            recipientId = m.recipientId,
            content = m.content,
            timestamp = m.timestamp,
            expiresAt = m.expiresAt,
            status = m.status.name,
            hopCount = m.hopCount,
            isOutgoing = m.isOutgoing
        )
    }
}
