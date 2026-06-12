package com.batallagroup.myco.domain.usecase

import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.crypto.CryptoManager
import com.batallagroup.myco.core.utils.PreferenceManager
import com.batallagroup.myco.domain.model.Message
import com.batallagroup.myco.domain.model.MessageStatus
import com.batallagroup.myco.domain.model.MycoPacket
import com.batallagroup.myco.domain.repository.ContactRepository
import com.batallagroup.myco.domain.repository.MessageRepository
import com.google.gson.Gson
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val cryptoManager: CryptoManager,
    private val preferenceManager: PreferenceManager
) {
    private val gson = Gson()

    suspend operator fun invoke(recipientId: String, content: String): Result<MycoPacket> {
        return try {
            val contact = contactRepository.getContactById(recipientId)
                ?: return Result.failure(Exception("Contacto no encontrado"))

            val myId = preferenceManager.getString(Constants.PREF_USER_ID) ?: ""
            val myEncSK = cryptoManager.fromBase64(
                preferenceManager.getString(Constants.PREF_ENC_PRIVATE_KEY) ?: ""
            )
            val myEncPK = preferenceManager.getString(Constants.PREF_ENC_PUBLIC_KEY) ?: ""
            val mySignSK = cryptoManager.fromBase64(
                preferenceManager.getString(Constants.PREF_SIGN_PRIVATE_KEY) ?: ""
            )

            val recipientEncPK = cryptoManager.fromBase64(contact.encPublicKey)
            val encryptedBytes = cryptoManager.encryptMessage(
                content.toByteArray(Charsets.UTF_8),
                recipientEncPK,
                myEncSK
            )
            val encryptedB64 = cryptoManager.toBase64(encryptedBytes)

            val now = System.currentTimeMillis()
            val messageId = UUID.randomUUID().toString()

            val packetBytes = "$messageId|$myId|$recipientId|$encryptedB64".toByteArray()
            val signature = cryptoManager.signMessage(packetBytes, mySignSK)

            val packet = MycoPacket(
                messageId = messageId,
                senderId = myId,
                recipientId = recipientId,
                hopCount = 0,
                maxHops = Constants.MAX_HOPS,
                createdAt = now,
                expiresAt = now + TimeUnit.HOURS.toMillis(Constants.MESSAGE_TTL_HOURS),
                encryptedContent = encryptedB64,
                senderSignature = cryptoManager.toBase64(signature),
                senderEncPublicKey = myEncPK
            )

            val message = Message(
                id = messageId,
                senderId = myId,
                recipientId = recipientId,
                content = content,
                timestamp = now,
                expiresAt = packet.expiresAt,
                status = MessageStatus.SENDING,
                isOutgoing = true
            )
            messageRepository.insertMessage(message)

            Result.success(packet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
