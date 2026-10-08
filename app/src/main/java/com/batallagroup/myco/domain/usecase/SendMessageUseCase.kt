package com.batallagroup.myco.domain.usecase

import android.util.Log
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.crypto.CryptoManager
import com.batallagroup.myco.core.utils.PreferenceManager
import com.batallagroup.myco.domain.model.Contact
import com.batallagroup.myco.domain.model.Message
import com.batallagroup.myco.domain.model.MessageStatus
import com.batallagroup.myco.domain.model.MycoPacket
import com.batallagroup.myco.domain.repository.ContactRepository
import com.batallagroup.myco.domain.repository.MessageRepository
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val cryptoManager: CryptoManager,
    private val preferenceManager: PreferenceManager
) {
    companion object {
        private const val TAG = "SendMessageUseCase"
    }

    suspend operator fun invoke(
        recipientId: String,
        content: String,
        transportType: String = Constants.TRANSPORT_RELAY,
        isSos: Boolean = false
    ): Result<MycoPacket> {
        val now = System.currentTimeMillis()
        val messageId = UUID.randomUUID().toString()
        val myId = preferenceManager.getString(Constants.PREF_USER_ID) ?: "anonymous"
        val myEncPK = preferenceManager.getString(Constants.PREF_ENC_PUBLIC_KEY) ?: ""
        val myEncSKStr = preferenceManager.getString(Constants.PREF_ENC_PRIVATE_KEY) ?: ""
        val mySignSKStr = preferenceManager.getString(Constants.PREF_SIGN_PRIVATE_KEY) ?: ""
        val ttl = now + TimeUnit.HOURS.toMillis(Constants.MESSAGE_TTL_HOURS)

        // 1. Guardar el mensaje en Room de forma inmediata (Optimistic UI)
        val message = Message(
            id = messageId,
            senderId = myId,
            recipientId = recipientId,
            content = content,
            timestamp = now,
            expiresAt = ttl,
            status = MessageStatus.SENDING,
            isOutgoing = true,
            transportType = transportType,
            isSos = isSos
        )
        messageRepository.insertMessage(message)

        // 2. Asegurar que el contacto exista en la base de datos local
        var contact = contactRepository.getContactById(recipientId)
        if (contact == null) {
            val isEmergencyContact = (recipientId == Constants.BROADCAST_SOS_ID)
            val newContact = Contact(
                userId = recipientId,
                alias = if (isEmergencyContact) "🚨 SOS - Emergencia" else recipientId.take(8),
                encPublicKey = "",
                signPublicKey = ""
            )
            contactRepository.insertContact(newContact)
            contact = newContact
        }

        return try {
            // 3. Cifrar el contenido (o enviar en claro si es señal SOS de emergencia de rescate público)
            val encryptedB64: String = if (isSos) {
                cryptoManager.toBase64(content.toByteArray(Charsets.UTF_8))
            } else {
                try {
                    if (contact.encPublicKey.isNotBlank() && contact.encPublicKey.length > 30 && myEncSKStr.isNotBlank()) {
                        val myEncSK = cryptoManager.fromBase64(myEncSKStr)
                        val recipientEncPK = cryptoManager.fromBase64(contact.encPublicKey)
                        val encryptedBytes = cryptoManager.encryptMessage(
                            content.toByteArray(Charsets.UTF_8),
                            recipientEncPK,
                            myEncSK
                        )
                        cryptoManager.toBase64(encryptedBytes)
                    } else {
                        // Fallback para IDs manuales sin clave pública completa compartida
                        cryptoManager.toBase64(content.toByteArray(Charsets.UTF_8))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error cifrando con clave pública, usando fallback: ${e.message}")
                    cryptoManager.toBase64(content.toByteArray(Charsets.UTF_8))
                }
            }

            // 4. Firmar el paquete
            val signatureB64: String = try {
                if (mySignSKStr.isNotBlank()) {
                    val mySignSK = cryptoManager.fromBase64(mySignSKStr)
                    val packetBytes = "$messageId|$myId|$recipientId|$encryptedB64".toByteArray()
                    val sig = cryptoManager.signMessage(packetBytes, mySignSK)
                    cryptoManager.toBase64(sig)
                } else {
                    ""
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error firmando paquete: ${e.message}")
                ""
            }

            val packet = MycoPacket(
                messageId = messageId,
                senderId = myId,
                recipientId = recipientId,
                hopCount = 0,
                maxHops = Constants.MAX_HOPS,
                createdAt = now,
                expiresAt = ttl,
                encryptedContent = encryptedB64,
                senderSignature = signatureB64,
                senderEncPublicKey = myEncPK,
                transportType = transportType,
                isSos = isSos
            )

            Result.success(packet)
        } catch (e: Exception) {
            Log.e(TAG, "Error preparando paquete Myco: ${e.message}", e)
            Result.failure(e)
        }
    }
}
