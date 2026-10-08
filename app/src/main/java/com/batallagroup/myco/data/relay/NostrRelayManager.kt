package com.batallagroup.myco.data.relay

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.utils.PreferenceManager
import com.batallagroup.myco.data.ble.MeshPacketProcessor
import com.batallagroup.myco.domain.model.MycoPacket
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NostrRelayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager,
    private val meshPacketProcessor: MeshPacketProcessor
) {
    companion object {
        private const val TAG = "NostrRelayManager"
        private const val MYCO_EVENT_KIND = 20004
        private const val NTFY_BASE_URL = "https://ntfy.sh"
        private const val NTFY_WS_URL = "wss://ntfy.sh"
        private const val TOPIC_PREFIX = "myco_inbox_"

        val DEFAULT_RELAYS = listOf(
            "wss://relay.damus.io",
            "wss://nos.lol",
            "wss://relay.snort.social",
            "wss://relay.primal.net"
        )
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Sin timeout de lectura para mantener WebSocket siempre vivo
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val activeSockets = ConcurrentHashMap<String, WebSocket>()
    private var ntfyWebSocket: WebSocket? = null

    private val _connectedRelaysCount = MutableStateFlow(0)
    val connectedRelaysCount: StateFlow<Int> = _connectedRelaysCount

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    private var connectivityManager: ConnectivityManager? = null

    init {
        setupNetworkListener()
        _isOnline.value = checkCurrentNetwork()
        if (_isOnline.value) {
            connectToAllRelays()
        }
    }

    fun checkCurrentNetwork(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNet = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNet) ?: return false
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                               caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                               caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            hasInternet && hasTransport
        } catch (e: Exception) {
            false
        }
    }

    fun start() {
        scope.launch {
            _isOnline.value = checkCurrentNetwork() || (_connectedRelaysCount.value > 0)
            connectToAllRelays()
        }
    }

    fun stop() {
        ntfyWebSocket?.close(1000, "App closed")
        ntfyWebSocket = null
        activeSockets.values.forEach { it.close(1000, "App closed") }
        activeSockets.clear()
        _connectedRelaysCount.value = 0
    }

    private fun setupNetworkListener() {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        try {
            connectivityManager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    Log.d(TAG, "Conexión a red disponible. Sincronizando con relays...")
                    connectToAllRelays()
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    _isOnline.value = (hasInternet && isValidated) || _connectedRelaysCount.value > 0
                    if (hasInternet && isValidated && _connectedRelaysCount.value == 0) {
                        connectToAllRelays()
                    }
                }

                override fun onLost(network: Network) {
                    val isStillOnline = checkCurrentNetwork()
                    _isOnline.value = isStillOnline || (_connectedRelaysCount.value > 0)
                    Log.d(TAG, "Red perdida. Estado online restante: ${_isOnline.value}")
                    updateConnectedCount()
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Error registrando DefaultNetworkCallback: ${e.message}")
        }
    }

    fun connectToAllRelays() {
        val myUserId = preferenceManager.getString(Constants.PREF_USER_ID) ?: return

        // 1. Conectar al canal instantáneo WebSocket / Buffer descentralizado
        connectToNtfyWebSocket(myUserId)
        pollPendingMessages(myUserId)

        // 2. Conectar a los relays públicos Nostr
        DEFAULT_RELAYS.forEach { relayUrl ->
            connectToNostrRelay(relayUrl, myUserId)
        }
    }

    private fun connectToNtfyWebSocket(myUserId: String) {
        val topics = "$TOPIC_PREFIX$myUserId,$TOPIC_PREFIX${Constants.BROADCAST_SOS_ID}"
        val wsUrl = "$NTFY_WS_URL/$topics/ws"
        Log.d(TAG, "Iniciando WebSocket de mensajería rápida a: $wsUrl")

        val request = Request.Builder().url(wsUrl).build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Myco conectado con éxito para buzón $myUserId y canal SOS")
                ntfyWebSocket = webSocket
                updateConnectedCount()
                meshPacketProcessor.flushPendingOutbox()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Mensaje entrante por WebSocket: $text")
                try {
                    val json = gson.fromJson(text, JsonObject::class.java)
                    val event = json.get("event")?.asString
                    if (event == "message") {
                        val messageBody = json.get("message")?.asString
                        if (!messageBody.isNullOrBlank()) {
                            meshPacketProcessor.processIncoming(messageBody, transportType = Constants.TRANSPORT_RELAY)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando mensaje WebSocket: ${e.message}")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket cerrado: $reason")
                ntfyWebSocket = null
                updateConnectedCount()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Fallo en WebSocket Myco: ${t.message}. Reintentando en 5s...")
                ntfyWebSocket = null
                updateConnectedCount()
                scope.launch {
                    delay(5000)
                    if (_isOnline.value && ntfyWebSocket == null) {
                        connectToNtfyWebSocket(myUserId)
                    }
                }
            }
        }

        try {
            client.newWebSocket(request, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear WebSocket: ${e.message}")
        }
    }

    private fun pollPendingMessages(myUserId: String) {
        scope.launch {
            try {
                val topics = "$TOPIC_PREFIX$myUserId,$TOPIC_PREFIX${Constants.BROADCAST_SOS_ID}"
                val pollUrl = "$NTFY_BASE_URL/$topics/json?poll=1&since=24h"
                val request = Request.Builder().url(pollUrl).get().build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.w(TAG, "Error consultando mensajes en cola: ${e.message}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use { resp ->
                            if (!resp.isSuccessful) return
                            val lines = resp.body?.string()?.lines() ?: return
                            for (line in lines) {
                                if (line.isBlank()) continue
                                try {
                                    val json = gson.fromJson(line, JsonObject::class.java)
                                    val event = json.get("event")?.asString
                                    if (event == "message") {
                                        val messageBody = json.get("message")?.asString
                                        if (!messageBody.isNullOrBlank()) {
                                            meshPacketProcessor.processIncoming(messageBody, transportType = Constants.TRANSPORT_RELAY)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Ignorar líneas que no sean mensajes JSON
                                }
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error en poll de mensajes: ${e.message}")
            }
        }
    }

    private fun connectToNostrRelay(url: String, myUserId: String) {
        if (activeSockets.containsKey(url)) return

        val request = Request.Builder().url(url).build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Conectado al relay Nostr: $url")
                activeSockets[url] = webSocket
                updateConnectedCount()
                subscribeToNostrMessages(webSocket, myUserId)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleNostrMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                activeSockets.remove(url)
                updateConnectedCount()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                activeSockets.remove(url)
                updateConnectedCount()
                scope.launch {
                    delay(15000)
                    if (_isOnline.value && !activeSockets.containsKey(url)) {
                        connectToNostrRelay(url, myUserId)
                    }
                }
            }
        }

        try {
            client.newWebSocket(request, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando WebSocket Nostr a $url: ${e.message}")
        }
    }

    private fun subscribeToNostrMessages(webSocket: WebSocket, myUserId: String) {
        val reqArray = JsonArray().apply {
            add("REQ")
            add("myco_$myUserId")
            val filter = JsonObject().apply {
                val kinds = JsonArray().apply { add(MYCO_EVENT_KIND) }
                val pTags = JsonArray().apply {
                    add(myUserId)
                    add(Constants.BROADCAST_SOS_ID)
                }
                add("kinds", kinds)
                add("#p", pTags)
            }
            add(filter)
        }
        webSocket.send(reqArray.toString())
    }

    fun publishPacket(packet: MycoPacket) {
        scope.launch {
            val packetJson = gson.toJson(packet)
            val recipientTopic = "$TOPIC_PREFIX${packet.recipientId}"
            val postUrl = "$NTFY_BASE_URL/$recipientTopic"

            // 1. Envío ultrarrápido al buzón del destinatario
            val mediaType = "text/plain; charset=utf-8".toMediaType()
            val body = packetJson.toRequestBody(mediaType)
            val postRequest = try {
                Request.Builder()
                    .url(postUrl)
                    .post(body)
                    .addHeader("Title", "Myco")
                    .addHeader("Priority", "urgent")
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "Error construyendo request HTTP: ${e.message}")
                null
            }

            if (postRequest != null) {
                client.newCall(postRequest).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "Error enviando paquete a $postUrl: ${e.message}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use { resp ->
                            Log.d(TAG, "Paquete publicado a $recipientTopic con código ${resp.code}")
                        }
                    }
                })
            }

            // 2. Envío a Nostr Relays
            val nostrEvent = createNostrEvent(packet)
            val reqArray = JsonArray().apply {
                add("EVENT")
                add(gson.fromJson(nostrEvent, JsonObject::class.java))
            }
            val nostrPayload = reqArray.toString()

            activeSockets.values.forEach { socket ->
                try {
                    socket.send(nostrPayload)
                } catch (e: Exception) {
                    Log.w(TAG, "Error enviando a relay Nostr: ${e.message}")
                }
            }
        }
    }

    private fun createNostrEvent(packet: MycoPacket): String {
        val nowSec = System.currentTimeMillis() / 1000
        val packetJson = gson.toJson(packet)

        val tagsArray = JsonArray().apply {
            val pTag = JsonArray().apply {
                add("p")
                add(packet.recipientId)
            }
            val sTag = JsonArray().apply {
                add("s")
                add(packet.senderId)
            }
            add(pTag)
            add(sTag)
        }

        val eventObj = JsonObject().apply {
            addProperty("pubkey", packet.senderId.padStart(64, '0'))
            addProperty("created_at", nowSec)
            addProperty("kind", MYCO_EVENT_KIND)
            add("tags", tagsArray)
            addProperty("content", packetJson)
            val idHash = sha256("$nowSec|${packet.senderId}|${packet.recipientId}|${packet.messageId}")
            addProperty("id", idHash)
            addProperty("sig", packet.senderSignature.ifEmpty { idHash })
        }

        return eventObj.toString()
    }

    private fun handleNostrMessage(text: String) {
        try {
            val array = gson.fromJson(text, JsonArray::class.java) ?: return
            if (array.size() < 2) return

            val messageType = array[0].asString
            if (messageType == "EVENT" && array.size() >= 3) {
                val eventObj = array[2].asJsonObject
                val content = eventObj.get("content")?.asString ?: return
                meshPacketProcessor.processIncoming(content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando mensaje de relay: ${e.message}")
        }
    }

    private fun updateConnectedCount() {
        val count = activeSockets.size + (if (ntfyWebSocket != null) 1 else 0)
        _connectedRelaysCount.value = count
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
