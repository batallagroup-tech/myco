package com.batallagroup.myco.data.ble

import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.crypto.CryptoManager
import com.batallagroup.myco.core.utils.PreferenceManager
import com.batallagroup.myco.data.local.dao.MessageDao
import com.batallagroup.myco.data.local.dao.TransitMessageDao
import com.batallagroup.myco.data.local.entity.MessageEntity
import com.batallagroup.myco.data.local.entity.TransitMessageEntity
import com.batallagroup.myco.domain.model.MessageStatus
import com.batallagroup.myco.domain.model.MycoPacket
import com.batallagroup.myco.domain.repository.ContactRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshPacketProcessor @Inject constructor(
    private val transitMessageDao: TransitMessageDao,
    private val messageDao: MessageDao,
    private val contactRepository: ContactRepository,
    private val cryptoManager: CryptoManager,
    private val preferenceManager: PreferenceManager
) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Emite paquetes que deben retransmitirse a otros nodos
    private val _packetsToRelay = MutableSharedFlow<MycoPacket>(extraBufferCapacity = 64)
    val packetsToRelay: SharedFlow<MycoPacket> = _packetsToRelay

    fun processIncoming(packetJson: String) {
        scope.launch {
            try {
                val packet = gson.fromJson(packetJson, MycoPacket::class.java) ?: return@launch
                handlePacket(packet)
            } catch (e: Exception) {
                // Paquete malformado — ignorar silenciosamente
            }
        }
    }

    private suspend fun handlePacket(packet: MycoPacket) {
        // Anti-loop: ignorar si ya procesamos este ID
        if (transitMessageDao.exists(packet.messageId)) return

        // Verificar expiración
        if (System.currentTimeMillis() > packet.expiresAt) return

        // Verificar hops
        if (packet.hopCount >= packet.maxHops) return

        val myId = preferenceManager.getString(Constants.PREF_USER_ID) ?: return

        // ¿Es para mí?
        if (packet.recipientId == myId) {
            deliverToMe(packet, myId)
            return
        }

        // ¿Tengo retransmisión habilitada?
        val relayEnabled = preferenceManager.getBoolean(Constants.PREF_RELAY_ENABLED, true)
        if (!relayEnabled) return

        // Almacenar en tránsito y retransmitir
        val transitEntity = TransitMessageEntity(
            messageId = packet.messageId,
            senderId = packet.senderId,
            recipientId = packet.recipientId,
            hopCount = packet.hopCount + 1,
            maxHops = packet.maxHops,
            createdAt = packet.createdAt,
            expiresAt = packet.expiresAt,
            encryptedContent = packet.encryptedContent,
            senderSignature = packet.senderSignature,
            senderEncPublicKey = packet.senderEncPublicKey
        )
        val inserted = transitMessageDao.insertTransitMessage(transitEntity)
        if (inserted != -1L) {
            _packetsToRelay.emit(packet.copy(hopCount = packet.hopCount + 1))
        }
    }

    private suspend fun deliverToMe(packet: MycoPacket, myId: String) {
        try {
            val myEncSK = cryptoManager.fromBase64(
                preferenceManager.getString(Constants.PREF_ENC_PRIVATE_KEY) ?: return
            )
            val senderEncPK = cryptoManager.fromBase64(packet.senderEncPublicKey)
            val encryptedBytes = cryptoManager.fromBase64(packet.encryptedContent)

            val plaintext = cryptoManager.decryptMessage(encryptedBytes, senderEncPK, myEncSK)
            val content = plaintext.toString(Charsets.UTF_8)

            // Verificar si el contacto existe; si no, guardar de todos modos
            val msg = MessageEntity(
                id = packet.messageId,
                senderId = packet.senderId,
                recipientId = myId,
                content = content,
                timestamp = packet.createdAt,
                expiresAt = packet.expiresAt,
                status = MessageStatus.DELIVERED.name,
                hopCount = packet.hopCount,
                isOutgoing = false
            )
            messageDao.insertMessage(msg)

            // Actualizar lastSeen del contacto si existe
            contactRepository.updateLastSeen(packet.senderId, System.currentTimeMillis())
        } catch (e: Exception) {
            // Descifrado fallido — posiblemente no es un contacto conocido
        }
    }
}
