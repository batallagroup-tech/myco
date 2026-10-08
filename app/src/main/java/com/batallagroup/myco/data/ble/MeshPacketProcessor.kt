package com.batallagroup.myco.data.ble

import android.util.Log
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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import com.batallagroup.myco.core.utils.MycoNotificationManager
import com.batallagroup.myco.domain.model.Contact

data class ContactRequest(
    val senderId: String,
    val senderAlias: String,
    val senderEncPK: String,
    val senderSignPK: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class MeshPacketProcessor @Inject constructor(
    private val transitMessageDao: TransitMessageDao,
    private val messageDao: MessageDao,
    private val contactRepository: ContactRepository,
    private val cryptoManager: CryptoManager,
    private val preferenceManager: PreferenceManager,
    private val notificationManager: MycoNotificationManager
) {
    companion object {
        private const val TAG = "MeshPacketProcessor"
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val seenPackets = java.util.concurrent.ConcurrentHashMap<String, Long>()

    // Solicitudes de contacto entrantes (para diálogo personalizado en UI)
    private val _incomingContactRequests = kotlinx.coroutines.flow.MutableStateFlow<List<ContactRequest>>(emptyList())
    val incomingContactRequests: kotlinx.coroutines.flow.StateFlow<List<ContactRequest>> = _incomingContactRequests

    // Emite paquetes que deben retransmitirse a otros nodos / relays
    private val _packetsToRelay = MutableSharedFlow<MycoPacket>(extraBufferCapacity = 64)
    val packetsToRelay: SharedFlow<MycoPacket> = _packetsToRelay

    private fun cleanExpiredSeenPackets() {
        val now = System.currentTimeMillis()
        val expiryThreshold = 24 * 60 * 60 * 1000L
        seenPackets.entries.removeIf { now - it.value > expiryThreshold }
    }

    fun processIncoming(packetJson: String, transportType: String = Constants.TRANSPORT_RELAY) {
        scope.launch {
            try {
                cleanExpiredSeenPackets()
                val packet = gson.fromJson(packetJson, MycoPacket::class.java) ?: return@launch
                if (seenPackets.putIfAbsent(packet.messageId, System.currentTimeMillis()) != null) {
                    // Ya procesado recientemente, descartar duplicado
                    return@launch
                }
                val resolvedTransport = if (packet.transportType != Constants.TRANSPORT_RELAY) packet.transportType else transportType
                val packetWithTransport = packet.copy(transportType = resolvedTransport)
                handlePacket(packetWithTransport)
            } catch (e: Exception) {
                Log.w(TAG, "Error procesando paquete JSON: ${e.message}")
            }
        }
    }

    private suspend fun handlePacket(packet: MycoPacket) {
        // Verificar expiración
        if (System.currentTimeMillis() > packet.expiresAt) return

        // Verificar hops
        if (packet.hopCount >= packet.maxHops) return

        val myId = preferenceManager.getString(Constants.PREF_USER_ID) ?: return

        // Caso 1: Paquete SOS de Emergencia Global (Broadcast)
        if (packet.isSos || packet.recipientId == Constants.BROADCAST_SOS_ID) {
            handleSosBroadcast(packet, myId)
            return
        }

        // Caso 2: ¿Es para mí directamente?
        if (packet.recipientId == myId) {
            deliverToMe(packet, myId)
            return
        }

        // Si es un paquete de ACK retransmitiéndose, liberar la cola de tránsito local
        try {
            val rawContent = cryptoManager.fromBase64(packet.encryptedContent).toString(Charsets.UTF_8)
            if (rawContent.startsWith("MYCO_ACK:")) {
                val ackMsgId = rawContent.removePrefix("MYCO_ACK:").trim()
                transitMessageDao.deleteByMessageId(ackMsgId)
            }
        } catch (_: Exception) {}

        // Anti-loop: ignorar si ya procesamos este ID
        if (transitMessageDao.exists(packet.messageId)) return

        // ¿Tengo retransmisión habilitada?
        val relayEnabled = preferenceManager.getBoolean(Constants.PREF_RELAY_ENABLED, true)
        if (!relayEnabled) return

        // Almacenar en tránsito y retransmitir (Malla Gateway)
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

    private suspend fun handleSosBroadcast(packet: MycoPacket, myId: String) {
        // Anti-loop para SOS
        if (transitMessageDao.exists(packet.messageId)) return

        // Registrar en tránsito
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
        transitMessageDao.insertTransitMessage(transitEntity)

        // Si fue emitido por nosotros mismos, no duplicar la entrega local
        if (packet.senderId != myId) {
            val content = try {
                cryptoManager.fromBase64(packet.encryptedContent).toString(Charsets.UTF_8)
            } catch (e: Exception) {
                packet.encryptedContent
            }

            // Asegurar que el contacto de Emergencia SOS exista
            var sosContact = contactRepository.getContactById(Constants.BROADCAST_SOS_ID)
            if (sosContact == null) {
                val newSos = Contact(
                    userId = Constants.BROADCAST_SOS_ID,
                    alias = "🚨 SOS - Emergencia",
                    encPublicKey = "",
                    signPublicKey = ""
                )
                contactRepository.insertContact(newSos)
            }

            // Guardar en la base de datos bajo el canal SOS
            val msg = MessageEntity(
                id = packet.messageId,
                senderId = packet.senderId,
                recipientId = Constants.BROADCAST_SOS_ID,
                content = content,
                timestamp = packet.createdAt,
                expiresAt = packet.expiresAt,
                status = MessageStatus.DELIVERED.name,
                hopCount = packet.hopCount,
                isOutgoing = false,
                transportType = packet.transportType,
                isSos = true,
                isRead = false
            )
            messageDao.insertMessage(msg)

            // Disparar Notificación de Emergencia de Máxima Prioridad
            notificationManager.showSosEmergencyNotification(
                senderId = packet.senderId,
                senderAlias = "Nodo ${packet.senderId.take(6).uppercase()}",
                messageContent = content
            )
        }

        // Retransmitir a toda la malla circundante (BLE + Wi-Fi Direct + Relays)
        if (packet.hopCount < packet.maxHops) {
            _packetsToRelay.emit(packet.copy(hopCount = packet.hopCount + 1))
        }
    }

    private suspend fun deliverToMe(packet: MycoPacket, myId: String) {
        try {
            val content: String = try {
                val myEncSKStr = preferenceManager.getString(Constants.PREF_ENC_PRIVATE_KEY)
                if (myEncSKStr != null && packet.senderEncPublicKey.isNotBlank()) {
                    val myEncSK = cryptoManager.fromBase64(myEncSKStr)
                    val senderEncPK = cryptoManager.fromBase64(packet.senderEncPublicKey)
                    val encryptedBytes = cryptoManager.fromBase64(packet.encryptedContent)
                    val plaintext = cryptoManager.decryptMessage(encryptedBytes, senderEncPK, myEncSK)
                    plaintext.toString(Charsets.UTF_8)
                } else {
                    cryptoManager.fromBase64(packet.encryptedContent).toString(Charsets.UTF_8)
                }
            } catch (e: Exception) {
                // Fallback si no fue descifrable por clave o venía en base64 plano
                try {
                    cryptoManager.fromBase64(packet.encryptedContent).toString(Charsets.UTF_8)
                } catch (e2: Exception) {
                    packet.encryptedContent
                }
            }

            // 1. Manejo de paquete de confirmación de entrega (ACK)
            if (content.startsWith("MYCO_ACK:")) {
                val ackMessageId = content.removePrefix("MYCO_ACK:").trim()
                Log.d(TAG, "Recibido ACK para mensaje: $ackMessageId")
                messageDao.updateMessageStatus(ackMessageId, MessageStatus.DELIVERED.name, packet.hopCount)
                transitMessageDao.deleteByMessageId(ackMessageId)
                if (packet.hopCount < packet.maxHops) {
                    _packetsToRelay.emit(packet.copy(hopCount = packet.hopCount + 1))
                }
                return
            }

            // 2. Manejo de Solicitud de Conversación P2P (Contact Request)
            if (content.startsWith(Constants.PREFIX_CONTACT_REQ)) {
                val payload = content.removePrefix(Constants.PREFIX_CONTACT_REQ)
                val parts = payload.split(":")
                val senderAlias = if (parts.isNotEmpty()) parts[0].ifEmpty { "Nodo ${packet.senderId.take(4).uppercase()}" } else "Nodo"
                val senderEncPK = if (parts.size > 1) parts[1] else packet.senderEncPublicKey
                val senderSignPK = if (parts.size > 2) parts[2] else ""

                // Anti-spam: comprobar si el usuario ya rechazó a este nodo
                val rejectedSet = preferenceManager.getStringSet(Constants.PREF_REJECTED_CONTACT_IDS)
                if (rejectedSet.contains(packet.senderId)) {
                    Log.d(TAG, "Solicitud de contacto de ${packet.senderId} ignorada por lista de rechazo anti-spam")
                    return
                }

                val existingContact = contactRepository.getContactById(packet.senderId)
                if (existingContact != null) {
                    // Ya es contacto, actualizar datos y confirmar
                    contactRepository.insertContact(existingContact.copy(alias = senderAlias, encPublicKey = senderEncPK))
                    return
                }

                // Evitar notificaciones duplicadas de la misma solicitud si ya está pendiente
                if (_incomingContactRequests.value.any { it.senderId == packet.senderId }) {
                    return
                }

                // Agregar solicitud a la lista observable para notificación/diálogo
                val req = ContactRequest(
                    senderId = packet.senderId,
                    senderAlias = senderAlias,
                    senderEncPK = senderEncPK,
                    senderSignPK = senderSignPK
                )
                val current = _incomingContactRequests.value.filter { it.senderId != packet.senderId }
                _incomingContactRequests.value = current + req

                notificationManager.showContactRequestNotification(packet.senderId, senderAlias)
                return
            }

            // 3. Manejo de Aceptación de Solicitud de Conversación (Contact Accept)
            if (content.startsWith(Constants.PREFIX_CONTACT_ACCEPT)) {
                val payload = content.removePrefix(Constants.PREFIX_CONTACT_ACCEPT)
                val parts = payload.split(":")
                val senderAlias = if (parts.isNotEmpty()) parts[0].ifEmpty { "Nodo ${packet.senderId.take(4).uppercase()}" } else "Nodo"
                val senderEncPK = if (parts.size > 1) parts[1] else packet.senderEncPublicKey
                val senderSignPK = if (parts.size > 2) parts[2] else ""

                val newContact = Contact(
                    userId = packet.senderId,
                    alias = senderAlias,
                    encPublicKey = senderEncPK,
                    signPublicKey = senderSignPK
                )
                contactRepository.insertContact(newContact)

                val welcomeMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    senderId = packet.senderId,
                    recipientId = myId,
                    content = "👋 ¡$senderAlias ha aceptado tu solicitud de conversación!",
                    timestamp = packet.createdAt,
                    expiresAt = packet.expiresAt,
                    status = MessageStatus.DELIVERED.name,
                    hopCount = packet.hopCount,
                    isOutgoing = false,
                    transportType = packet.transportType,
                    isSos = false
                )
                messageDao.insertMessage(welcomeMsg)

                notificationManager.showIncomingMessageNotification(
                    senderId = packet.senderId,
                    senderAlias = senderAlias,
                    messageContent = "¡Ha aceptado tu solicitud de chat!"
                )
                return
            }

            // 4. Si ya existía este mensaje en nuestra base de datos, no duplicar
            val existing = messageDao.getMessageById(packet.messageId)
            if (existing != null) return

            // 5. Verificar si el contacto existe; si no, crearlo con su clave pública
            var contact = contactRepository.getContactById(packet.senderId)
            if (contact == null) {
                val newContact = Contact(
                    userId = packet.senderId,
                    alias = "Nodo ${packet.senderId.take(4).uppercase()}",
                    encPublicKey = packet.senderEncPublicKey,
                    signPublicKey = ""
                )
                contactRepository.insertContact(newContact)
                contact = newContact
            } else if (contact.encPublicKey.isBlank() && packet.senderEncPublicKey.isNotBlank()) {
                contactRepository.insertContact(contact.copy(encPublicKey = packet.senderEncPublicKey))
            }

            // 6. Guardar mensaje recibido en SQLite local
            val msg = MessageEntity(
                id = packet.messageId,
                senderId = packet.senderId,
                recipientId = myId,
                content = content,
                timestamp = packet.createdAt,
                expiresAt = packet.expiresAt,
                status = MessageStatus.DELIVERED.name,
                hopCount = packet.hopCount,
                isOutgoing = false,
                transportType = packet.transportType,
                isSos = packet.isSos,
                isRead = false
            )
            messageDao.insertMessage(msg)

            // 7. Actualizar última conexión del remitente
            contactRepository.updateLastSeen(packet.senderId, System.currentTimeMillis())

            // 8. Notificación nativa en la barra de estado
            notificationManager.showIncomingMessageNotification(
                senderId = packet.senderId,
                senderAlias = contact.displayName,
                messageContent = content
            )

            // 9. Enviar acuse de recibo (ACK) al remitente
            val myEncPK = preferenceManager.getString(Constants.PREF_ENC_PUBLIC_KEY) ?: ""
            val ackPacket = MycoPacket(
                messageId = UUID.randomUUID().toString(),
                senderId = myId,
                recipientId = packet.senderId,
                hopCount = 0,
                maxHops = Constants.MAX_HOPS,
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 86400000,
                encryptedContent = cryptoManager.toBase64("MYCO_ACK:${packet.messageId}".toByteArray(Charsets.UTF_8)),
                senderSignature = "",
                senderEncPublicKey = myEncPK,
                transportType = packet.transportType
            )
            _packetsToRelay.emit(ackPacket)

        } catch (e: Exception) {
            Log.e(TAG, "Error entregando mensaje a la app: ${e.message}", e)
        }
    }

    fun acceptContactRequest(request: ContactRequest) {
        scope.launch {
            val myId = preferenceManager.getString(Constants.PREF_USER_ID) ?: return@launch
            val myAlias = preferenceManager.getString(Constants.PREF_USER_ALIAS) ?: "Nodo"
            val myEncPK = preferenceManager.getString(Constants.PREF_ENC_PUBLIC_KEY) ?: ""
            val mySignPK = preferenceManager.getString(Constants.PREF_SIGN_PUBLIC_KEY) ?: ""

            // Guardar contacto en base de datos preservando apodo personalizado previo
            val existing = contactRepository.getContactById(request.senderId)
            val contact = existing?.copy(
                alias = request.senderAlias,
                encPublicKey = request.senderEncPK,
                signPublicKey = request.senderSignPK
            ) ?: Contact(
                userId = request.senderId,
                alias = request.senderAlias,
                encPublicKey = request.senderEncPK,
                signPublicKey = request.senderSignPK
            )
            contactRepository.insertContact(contact)

            // Remover de la lista de solicitudes pendientes
            _incomingContactRequests.value = _incomingContactRequests.value.filter { it.senderId != request.senderId }

            // Enviar respuesta de Aceptación al remitente
            val acceptPayload = "${Constants.PREFIX_CONTACT_ACCEPT}$myAlias:$myEncPK:$mySignPK"
            val acceptPacket = MycoPacket(
                messageId = UUID.randomUUID().toString(),
                senderId = myId,
                recipientId = request.senderId,
                hopCount = 0,
                maxHops = Constants.MAX_HOPS,
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 86400000,
                encryptedContent = cryptoManager.toBase64(acceptPayload.toByteArray(Charsets.UTF_8)),
                senderSignature = "",
                senderEncPublicKey = myEncPK,
                transportType = Constants.TRANSPORT_BLE
            )
            _packetsToRelay.emit(acceptPacket)
        }
    }

    fun rejectContactRequest(request: ContactRequest) {
        scope.launch {
            // Guardar en la lista negra anti-spam
            val currentRejected = preferenceManager.getStringSet(Constants.PREF_REJECTED_CONTACT_IDS).toMutableSet()
            currentRejected.add(request.senderId)
            preferenceManager.putStringSet(Constants.PREF_REJECTED_CONTACT_IDS, currentRejected)

            // Remover de la lista de pendientes
            _incomingContactRequests.value = _incomingContactRequests.value.filter { it.senderId != request.senderId }
        }
    }

    fun sendContactRequest(targetUserId: String, targetAlias: String) {
        scope.launch {
            val myId = preferenceManager.getString(Constants.PREF_USER_ID) ?: return@launch
            val myAlias = preferenceManager.getString(Constants.PREF_USER_ALIAS) ?: "Nodo"
            val myEncPK = preferenceManager.getString(Constants.PREF_ENC_PUBLIC_KEY) ?: ""
            val mySignPK = preferenceManager.getString(Constants.PREF_SIGN_PUBLIC_KEY) ?: ""

            val reqPayload = "${Constants.PREFIX_CONTACT_REQ}$myAlias:$myEncPK:$mySignPK"
            val reqPacket = MycoPacket(
                messageId = UUID.randomUUID().toString(),
                senderId = myId,
                recipientId = targetUserId,
                hopCount = 0,
                maxHops = Constants.MAX_HOPS,
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 86400000,
                encryptedContent = cryptoManager.toBase64(reqPayload.toByteArray(Charsets.UTF_8)),
                senderSignature = "",
                senderEncPublicKey = myEncPK,
                transportType = Constants.TRANSPORT_BLE
            )
            _packetsToRelay.emit(reqPacket)
        }
    }

    fun flushPendingOutbox() {
        scope.launch {
            try {
                val pending = messageDao.getPendingOutgoingMessages()
                if (pending.isEmpty()) return@launch

                Log.d(TAG, "Despachando cola de salida offline: ${pending.size} mensajes pendientes encontrados")
                val myId = preferenceManager.getString(Constants.PREF_USER_ID) ?: return@launch
                val myEncPK = preferenceManager.getString(Constants.PREF_ENC_PUBLIC_KEY) ?: ""
                val myEncSKStr = preferenceManager.getString(Constants.PREF_ENC_PRIVATE_KEY) ?: ""
                val mySignSKStr = preferenceManager.getString(Constants.PREF_SIGN_PRIVATE_KEY) ?: ""

                val myEncSK = if (myEncSKStr.isNotBlank()) cryptoManager.fromBase64(myEncSKStr) else null
                val mySignSK = if (mySignSKStr.isNotBlank()) cryptoManager.fromBase64(mySignSKStr) else null

                for (msg in pending) {
                    val contact = contactRepository.getContactById(msg.recipientId)
                    val recipientEncPK = if (contact?.encPublicKey?.isNotBlank() == true) {
                        cryptoManager.fromBase64(contact.encPublicKey)
                    } else null

                    val encryptedContentStr: String
                    val signatureStr: String

                    if (msg.isSos || msg.recipientId == Constants.BROADCAST_SOS_ID) {
                        encryptedContentStr = cryptoManager.toBase64(msg.content.toByteArray(Charsets.UTF_8))
                        signatureStr = if (mySignSK != null) {
                            val sig = cryptoManager.signMessage(msg.content.toByteArray(Charsets.UTF_8), mySignSK)
                            cryptoManager.toBase64(sig)
                        } else ""
                    } else if (recipientEncPK != null && myEncSK != null) {
                        val encryptedBytes = cryptoManager.encryptMessage(msg.content.toByteArray(Charsets.UTF_8), recipientEncPK, myEncSK)
                        encryptedContentStr = cryptoManager.toBase64(encryptedBytes)
                        signatureStr = if (mySignSK != null) {
                            val sig = cryptoManager.signMessage(encryptedBytes, mySignSK)
                            cryptoManager.toBase64(sig)
                        } else ""
                    } else {
                        encryptedContentStr = cryptoManager.toBase64(msg.content.toByteArray(Charsets.UTF_8))
                        signatureStr = ""
                    }

                    val packet = MycoPacket(
                        messageId = msg.id,
                        senderId = myId,
                        recipientId = msg.recipientId,
                        hopCount = 0,
                        maxHops = Constants.MAX_HOPS,
                        createdAt = msg.timestamp,
                        expiresAt = msg.expiresAt,
                        encryptedContent = encryptedContentStr,
                        senderSignature = signatureStr,
                        senderEncPublicKey = myEncPK,
                        transportType = msg.transportType,
                        isSos = msg.isSos
                    )

                    // Actualizar a estado en tránsito y emitir a la red
                    messageDao.updateMessageStatus(msg.id, MessageStatus.IN_TRANSIT.name, 0)
                    _packetsToRelay.emit(packet)
                    Log.d(TAG, "Mensaje pendiente retransmitido: ${msg.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error despachando cola de salida: ${e.message}")
            }
        }
    }
}
