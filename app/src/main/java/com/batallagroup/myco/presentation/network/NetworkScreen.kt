package com.batallagroup.myco.presentation.network

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batallagroup.myco.domain.model.MycoNode
import com.batallagroup.myco.presentation.theme.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val nodes by viewModel.nearbyNodes.collectAsStateWithLifecycle()
    val transitCount by viewModel.transitCount.collectAsStateWithLifecycle()
    val connectedRelaysCount by viewModel.connectedRelaysCount.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val blePacketsSent by viewModel.blePacketsSent.collectAsStateWithLifecycle()
    val blePacketsReceived by viewModel.blePacketsReceived.collectAsStateWithLifecycle()
    val meshHopsRelayed by viewModel.meshHopsRelayed.collectAsStateWithLifecycle()
    val lastMeshActivity by viewModel.lastMeshActivity.collectAsStateWithLifecycle()
    val existingContacts by viewModel.existingContacts.collectAsStateWithLifecycle()
    val sentRequestNodeIds by viewModel.sentRequestNodeIds.collectAsStateWithLifecycle()
    var selectedNodeForRequest by remember { mutableStateOf<MycoNode?>(null) }
    var activeHelpInfo by remember { mutableStateOf<StatHelpInfo?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Diálogo de Ayuda e Información sobre los módulos de red
    activeHelpInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { activeHelpInfo = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(info.icon, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(info.title, color = MycoGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    info.description,
                    color = MycoOnSurface,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { activeHelpInfo = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MycoGreen)
                ) {
                    Text("Entendido")
                }
            },
            containerColor = MycoSurface
        )
    }

    selectedNodeForRequest?.let { node ->
        val nodeName = node.alias.ifEmpty { "Nodo ${node.nodeId}" }
        AlertDialog(
            onDismissRequest = { selectedNodeForRequest = null },
            title = { Text("Conectar con nodo", color = MycoGreen, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Deseas enviar una solicitud de conversación cifrada a \"$nodeName\"?",
                    color = MycoOnSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendContactRequest(node.nodeId, node.alias)
                        selectedNodeForRequest = null
                        android.widget.Toast.makeText(context, "Solicitud enviada a $nodeName", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MycoGreen)
                ) {
                    Text("Enviar solicitud")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedNodeForRequest = null }) {
                    Text("Cancelar", color = MycoSubtle)
                }
            },
            containerColor = MycoSurface
        )
    }

    Scaffold(
        containerColor = MycoBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MycoSurface),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MycoGreen)
                    }
                },
                title = { Text("Red Myco Híbrida", color = MycoOnSurface) },
                actions = {
                    TextButton(onClick = { viewModel.syncRelays() }) {
                        Text("Sincronizar", color = MycoGreen, fontSize = 13.sp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Stats cards con botón de ayuda e información interactivo
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    label = "Mesh BLE",
                    value = nodes.size.toString(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activeHelpInfo = StatHelpInfo(
                            title = "Malla Mesh Bluetooth (BLE)",
                            description = "Detecta y conecta con dispositivos Myco a tu alrededor dentro de un rango físico de 10 a 30 metros de forma 100% offline (sin internet ni saldo móvil).\n\nLos mensajes saltan de teléfono en teléfono hasta llegar a su destino de forma encriptada.",
                            icon = "🔵"
                        )
                    }
                )
                StatCard(
                    label = "Tránsito",
                    value = transitCount.toString(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activeHelpInfo = StatHelpInfo(
                            title = "Mensajes en Tránsito",
                            description = "Indica la cantidad de paquetes cifrados que tu dispositivo está ayudando a retransmitir temporalmente para otros usuarios cercanos de la red mesh (modo Gateway o puente).\n\n🔒 Tu teléfono jamás puede leer el mensaje porque viaja protegido con cifrado de extremo a extremo (E2EE). Se eliminan automáticamente al entregarse.",
                            icon = "🔄"
                        )
                    }
                )
                StatCard(
                    label = "Relays P2P",
                    value = "$connectedRelaysCount/4",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activeHelpInfo = StatHelpInfo(
                            title = "Servidores Relay P2P",
                            description = "Conexiones descentralizadas a nivel mundial (basadas en Nostr) que se activan cuando tienes conexión a internet o datos móviles.\n\nPermiten entregar mensajes a larga distancia entre nodos que no se encuentran físicamente cerca, sin intermediarios centrales.",
                            icon = "🌐"
                        )
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Global Relay status indicator
            Surface(
                color = if (isOnline) MycoGreenDark.copy(alpha = 0.25f) else MycoSurface2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isOnline) "🌐" else "📶", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isOnline) "Red Global Activa (Larga distancia P2P)" else "Modo Mesh Offline (Sin internet)",
                            color = if (isOnline) MycoGreen else MycoSubtle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Monitor de diagnóstico de Malla Mesh en vivo
            Card(
                colors = CardDefaults.cardColors(containerColor = MycoSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(MycoGreen, RoundedCornerShape(4.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Monitor de Malla Mesh (En vivo)",
                                color = MycoOnSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            "MTU 512B",
                            color = MycoGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(blePacketsSent.toString(), color = MycoGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Enviados", color = MycoSubtle, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(blePacketsReceived.toString(), color = MycoGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Recibidos", color = MycoSubtle, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(meshHopsRelayed.toString(), color = MycoGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Retransmisiones", color = MycoSubtle, fontSize = 10.sp)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        color = MycoSurface2,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡", fontSize = 11.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                lastMeshActivity,
                                color = MycoSubtle,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Topología de la red", color = MycoSubtle, fontSize = 13.sp)

            Spacer(Modifier.height(8.dp))

            // Grafo de la red
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(containerColor = MycoSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                NetworkGraph(
                    nodes = nodes,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(16.dp))

            if (nodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sin nodos BLE cercanos. Tus mensajes se enviarán automáticamente por los Relays Globales Descentralizados.",
                        color = MycoSubtle,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                Text("Nodos Mesh detectados (${nodes.size} únicos)", color = MycoSubtle, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                nodes.forEach { node ->
                    val isAlreadyContact = existingContacts.any { it.userId.equals(node.nodeId, ignoreCase = true) }
                    val isRequestPending = sentRequestNodeIds.contains(node.nodeId)
                    NodeRow(
                        node = node,
                        isAlreadyContact = isAlreadyContact,
                        isRequestPending = isRequestPending,
                        onClick = {
                            when {
                                isAlreadyContact -> {
                                    android.widget.Toast.makeText(context, "${node.alias.ifEmpty { node.nodeId }} ya está en tus contactos", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                isRequestPending -> {
                                    android.widget.Toast.makeText(context, "Solicitud ya enviada a ${node.alias.ifEmpty { node.nodeId }}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    selectedNodeForRequest = node
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

data class StatHelpInfo(
    val title: String,
    val description: String,
    val icon: String
)

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MycoSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)) {
            // Icono de información / ayuda en la esquina superior
            Surface(
                color = MycoSurface2,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("ℹ️", fontSize = 10.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(value, color = MycoGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = MycoSubtle, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun NetworkGraph(nodes: List<MycoNode>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = minOf(cx, cy) * 0.6f

        val myNodeColor = MycoGreen
        val nodeColor = MycoGreenDark
        val lineColor = MycoGreenDark.copy(alpha = 0.4f)

        // Nodo central (yo)
        drawCircle(color = myNodeColor.copy(alpha = 0.2f), radius = 24f, center = Offset(cx, cy))
        drawCircle(color = myNodeColor, radius = 12f, center = Offset(cx, cy))

        // Nodos externos
        nodes.forEachIndexed { i, _ ->
            val angle = (2 * Math.PI * i / nodes.size) - Math.PI / 2
            val x = cx + radius * cos(angle).toFloat()
            val y = cy + radius * sin(angle).toFloat()
            val pos = Offset(x, y)

            drawLine(color = lineColor, start = Offset(cx, cy), end = pos, strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawCircle(color = nodeColor, radius = 10f, center = pos)
            drawCircle(color = myNodeColor.copy(alpha = 0.7f), radius = 6f, center = pos)
        }
    }
}

@Composable
private fun NodeRow(
    node: MycoNode,
    isAlreadyContact: Boolean = false,
    isRequestPending: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = MycoSurface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (isAlreadyContact) MycoGreen else MycoGreenDark, RoundedCornerShape(5.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (node.alias.isNotBlank()) {
                    Text(node.alias, color = MycoOnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("ID: ${node.nodeId}", color = MycoSubtle, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                } else {
                    Text("ID: ${node.nodeId}", color = MycoOnSurface, fontSize = 13.sp)
                }
            }
            Text("${node.rssi} dBm", color = MycoSubtle, fontSize = 12.sp)
            Spacer(Modifier.width(10.dp))
            when {
                isAlreadyContact -> {
                    Text(
                        "✓ Contacto",
                        color = MycoGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                isRequestPending -> {
                    Text(
                        "✓ Enviada",
                        color = MycoSubtle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                else -> {
                    Text(
                        "Conectar",
                        color = MycoGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
