package com.batallagroup.myco.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.batallagroup.myco.domain.model.Contact

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val userId: String,
    val encPublicKey: String,
    val signPublicKey: String,
    val alias: String,
    val addedAt: Long,
    val lastSeenAt: Long,
    val phoneNumber: String = "",
    val customNickname: String = ""
) {
    fun toDomain() = Contact(
        userId = userId,
        encPublicKey = encPublicKey,
        signPublicKey = signPublicKey,
        alias = alias,
        addedAt = addedAt,
        lastSeenAt = lastSeenAt,
        phoneNumber = phoneNumber,
        customNickname = customNickname
    )

    companion object {
        fun fromDomain(c: Contact) = ContactEntity(
            userId = c.userId,
            encPublicKey = c.encPublicKey,
            signPublicKey = c.signPublicKey,
            alias = c.alias,
            addedAt = c.addedAt,
            lastSeenAt = c.lastSeenAt,
            phoneNumber = c.phoneNumber,
            customNickname = c.customNickname
        )
    }
}
