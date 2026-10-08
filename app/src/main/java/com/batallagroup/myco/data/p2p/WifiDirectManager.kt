package com.batallagroup.myco.data.p2p

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.utils.PreferenceManager
import com.batallagroup.myco.data.ble.MeshPacketProcessor
import com.batallagroup.myco.domain.model.MycoPacket
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiDirectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packetProcessor: MeshPacketProcessor,
    private val preferenceManager: PreferenceManager
) {
    companion object {
        private const val TAG = "MycoWifiDirect"
        private const val P2P_PORT = 8988
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var wifiP2pManager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var isReceiverRegistered = false
    private var isServerRunning = false
    private var serverSocket: ServerSocket? = null

    private val _discoveredPeers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<WifiP2pDevice>> = _discoveredPeers

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var groupOwnerHost: String? = null

    private val peerListListener = WifiP2pManager.PeerListListener { peerList: WifiP2pDeviceList? ->
        val peers = peerList?.deviceList?.toList() ?: emptyList()
        _discoveredPeers.value = peers
        Log.d(TAG, "Wi-Fi Direct: ${peers.size} dispositivos encontrados en el radar P2P")
    }

    private val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info: WifiP2pInfo? ->
        if (info == null) return@ConnectionInfoListener
        _isConnected.value = info.groupFormed
        groupOwnerHost = info.groupOwnerAddress?.hostAddress

        if (info.groupFormed && info.isGroupOwner) {
            Log.d(TAG, "Wi-Fi Direct: Este dispositivo es el Servidor de Malla (Group Owner)")
            startSocketServer()
        } else if (info.groupFormed) {
            Log.d(TAG, "Wi-Fi Direct: Conectado a Grupo. IP del dueño: $groupOwnerHost")
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    wifiP2pManager?.let { mgr ->
                        channel?.let { ch ->
                            try {
                                mgr.requestPeers(ch, peerListListener)
                            } catch (e: SecurityException) {
                                Log.w(TAG, "Permiso no concedido para requestPeers: ${e.message}")
                            }
                        }
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    wifiP2pManager?.let { mgr ->
                        channel?.let { ch ->
                            try {
                                mgr.requestConnectionInfo(ch, connectionInfoListener)
                            } catch (e: SecurityException) {
                                Log.w(TAG, "Permiso no concedido para requestConnectionInfo: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    fun start() {
        try {
            wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
            channel = wifiP2pManager?.initialize(context, context.mainLooper, null)

            val intentFilter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }

            if (!isReceiverRegistered) {
                context.registerReceiver(receiver, intentFilter)
                isReceiverRegistered = true
            }

            discoverPeers()
            startSocketServer()

            // Retransmisión automática de paquetes recibidos por la malla Wi-Fi
            scope.launch {
                packetProcessor.packetsToRelay.collect { packet ->
                    broadcastPacket(packet)
                }
            }

            Log.d(TAG, "Wi-Fi Direct Mesh de alta distancia (200m) iniciado")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo inicializar Wi-Fi Direct: ${e.message}")
        }
    }

    fun stop() {
        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(receiver)
                isReceiverRegistered = false
            }
            stopSocketServer()
            channel?.close()
            channel = null
            wifiP2pManager = null
            Log.d(TAG, "Wi-Fi Direct Mesh detenido")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun discoverPeers() {
        try {
            wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Escaneo Wi-Fi Direct (200m) activo")
                }

                override fun onFailure(reasonCode: Int) {
                    Log.w(TAG, "Fallo al iniciar descubrimiento Wi-Fi Direct: $reasonCode")
                }
            })
        } catch (e: SecurityException) {
            Log.w(TAG, "Permiso de Wi-Fi no concedido: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error en discoverPeers: ${e.message}")
        }
    }

    private fun startSocketServer() {
        if (isServerRunning) return
        scope.launch {
            try {
                serverSocket = ServerSocket(P2P_PORT)
                isServerRunning = true
                Log.d(TAG, "Servidor de socket Wi-Fi Direct escuchando en puerto $P2P_PORT")

                while (isServerRunning && serverSocket?.isClosed == false) {
                    val socket = serverSocket?.accept() ?: break
                    handleIncomingSocket(socket)
                }
            } catch (e: Exception) {
                if (isServerRunning) {
                    Log.w(TAG, "Socket Server Wi-Fi Direct terminado: ${e.message}")
                }
            }
        }
    }

    private fun stopSocketServer() {
        isServerRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignorar
        }
        serverSocket = null
    }

    private fun handleIncomingSocket(socket: Socket) {
        scope.launch {
            try {
                socket.use { s ->
                    val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                    val line = reader.readLine()
                    if (!line.isNullOrBlank()) {
                        Log.d(TAG, "Paquete recibido por Wi-Fi Direct (200m)")
                        packetProcessor.processIncoming(line, transportType = Constants.TRANSPORT_WIFI_DIRECT)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error procesando socket entrante: ${e.message}")
            }
        }
    }

    fun sendPacketToHost(hostAddress: String, packet: MycoPacket) {
        scope.launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(hostAddress, P2P_PORT), 3000)
                socket.use { s ->
                    val writer = PrintWriter(s.getOutputStream(), true)
                    val packetJson = gson.toJson(packet)
                    writer.println(packetJson)
                    Log.d(TAG, "Paquete enviado con éxito por Wi-Fi Direct a $hostAddress")
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo entregar por Wi-Fi Direct a $hostAddress: ${e.message}")
            }
        }
    }

    fun connectToPeer(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        try {
            wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Conexión Wi-Fi Direct iniciada con ${device.deviceName}")
                }

                override fun onFailure(reason: Int) {
                    Log.w(TAG, "Fallo al conectar Wi-Fi Direct: $reason")
                }
            })
        } catch (e: SecurityException) {
            Log.w(TAG, "Permiso Wi-Fi no concedido: ${e.message}")
        }
    }

    fun broadcastPacket(packet: MycoPacket) {
        val host = groupOwnerHost
        if (host != null) {
            Log.d(TAG, "Enviando paquete por socket Wi-Fi Direct al Group Owner $host")
            sendPacketToHost(host, packet)
        }
    }
}
