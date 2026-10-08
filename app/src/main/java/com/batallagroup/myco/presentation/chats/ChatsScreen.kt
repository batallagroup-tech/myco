package com.batallagroup.myco.presentation.chats

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batallagroup.myco.core.utils.toFormattedTime
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.presentation.theme.*

@Composable
fun ChatsScreen(
    viewModel: ChatsViewModel = hiltViewModel(),
    onOpenChat: (String) -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val nearbyNodes by viewModel.nearbyNodes.collectAsStateWithLifecycle()
    val incomingRequests by viewModel.incomingContactRequests.collectAsStateWithLifecycle()
    val existingContacts by viewModel.existingContacts.collectAsStateWithLifecycle()
    val sentRequestNodeIds by viewModel.sentRequestNodeIds.collectAsStateWithLifecycle()
    val totalUnreadCount by viewModel.totalUnreadCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var conversationToDelete by remember { mutableStateOf<ConversationItem?>(null) }
    var showSosDialog by remember { mutableStateOf(false) }
    var showNearbyRadarDialog by remember { mutableStateOf(false) }
    var sosMessageText by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Diálogo de Solicitud de Chat Entrante Personalizada
    incomingRequests.firstOrNull()?.let { req ->
        AlertDialog(
            onDismissRequest = { /* Forzar decisión del usuario */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👋", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Solicitud de Conversación", color = MycoGreen, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "\"${req.senderAlias}\" (ID: ${req.senderId.take(8)}) quiere iniciar una conversación cifrada contigo.",
                        color = MycoOnSurface,
                        fontSize = 14.sp
                    )
                    Text(
                        "Al aceptar, se agregará a tus contactos y podrás chatear de forma segura de inmediato.",
                        color = MycoSubtle,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.acceptContactRequest(req)
                        onOpenChat(req.senderId)
                        Toast.makeText(context, "Solicitud aceptada", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MycoGreen)
                ) {
                    Text("Aceptar y Chatear")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.rejectContactRequest(req)
                        Toast.makeText(context, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Rechazar", color = MycoError)
                }
            },
            containerColor = MycoSurface
        )
    }

    // Diálogo de Radar de Nodos Cercanos
    if (showNearbyRadarDialog) {
        AlertDialog(
            onDismissRequest = { showNearbyRadarDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📡", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Dispositivos Cercanos", color = MycoGreen, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (nearbyNodes.isEmpty()) {
                        Text(
                            "Escaneando nodos BLE y Wi-Fi Direct cercanos...",
                            color = MycoSubtle,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            "Dispositivos detectados en tu radio de alcance:",
                            color = MycoSubtle,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                            items(nearbyNodes) { node ->
                                val name = node.alias.ifEmpty { "Nodo ${node.nodeId}" }
                                val isAlreadyContact = existingContacts.any { it.userId.equals(node.nodeId, ignoreCase = true) }
                                val isRequestPending = sentRequestNodeIds.contains(node.nodeId)

                                Surface(
                                    color = MycoSurface2,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            when {
                                                isAlreadyContact -> {
                                                    showNearbyRadarDialog = false
                                                    onOpenChat(node.nodeId)
                                                }
                                                isRequestPending -> {
                                                    Toast.makeText(context, "Ya enviaste solicitud a $name. Esperando confirmación.", Toast.LENGTH_SHORT).show()
                                                }
                                                else -> {
                                                    viewModel.sendContactRequest(node.nodeId, node.alias)
                                                    Toast.makeText(context, "Solicitud enviada a $name", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(name, color = MycoOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    color = androidx.compose.ui.graphics.Color(node.proximityColorHex).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text(
                                                        node.proximityLabel,
                                                        color = androidx.compose.ui.graphics.Color(node.proximityColorHex),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("ID: ${node.nodeId}", color = MycoSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                                Spacer(Modifier.width(8.dp))
                                                Text(node.signalBars, color = MycoSubtle, fontSize = 11.sp)
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        when {
                                            isAlreadyContact -> {
                                                Text("💬 Chatear", color = MycoGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            isRequestPending -> {
                                                Text("✓ Enviada", color = MycoSubtle, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            }
                                            else -> {
                                                Text("Conectar ➜", color = MycoGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNearbyRadarDialog = false }) {
                    Text("Cerrar", color = MycoGreen)
                }
            },
            containerColor = MycoSurface
        )
    }

    // Diálogo para eliminar conversación
    conversationToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("Eliminar conversación", color = MycoError, fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar todo el historial de chat con \"${item.contact.displayName}\"?", color = MycoOnSurface) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConversation(item.contact.userId)
                        conversationToDelete = null
                        Toast.makeText(context, "Conversación eliminada", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MycoError)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Cancelar", color = MycoSubtle)
                }
            },
            containerColor = MycoSurface
        )
    }

    // Diálogo de Emisión de Emergencia / Pánico SOS
    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚨", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Emitir Alerta SOS", color = MycoError, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Esta señal de auxilio se transmitirá de inmediato a TODOS los dispositivos cercanos (BLE, Wi-Fi Direct y Relays globales) sin destinatario fijo.",
                        color = MycoOnSurface,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = sosMessageText,
                        onValueChange = { sosMessageText = it },
                        placeholder = { Text("Describe la emergencia o ubicación...", color = MycoSubtle, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MycoError,
                            unfocusedBorderColor = MycoSurface3,
                            focusedTextColor = MycoOnSurface,
                            unfocusedTextColor = MycoOnSurface
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    // Botón para detectar ubicación actual por GPS
                    OutlinedButton(
                        onClick = {
                            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (!hasFine && !hasCoarse) {
                                Toast.makeText(context, "Se requiere permiso de ubicación/GPS", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            try {
                                val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                                val loc = locManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                    ?: locManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                                if (loc != null) {
                                    val lat = String.format(java.util.Locale.US, "%.5f", loc.latitude)
                                    val lon = String.format(java.util.Locale.US, "%.5f", loc.longitude)
                                    val locText = "📍 Ubicación: Lat $lat, Lon $lon (https://maps.google.com/?q=$lat,$lon)"
                                    sosMessageText = if (sosMessageText.isBlank()) locText else "$sosMessageText\n$locText"
                                    Toast.makeText(context, "📍 Ubicación GPS añadida", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Buscando satélites GPS... activa la ubicación en tu celular.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error obteniendo ubicación: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MycoGreen),
                        border = BorderStroke(1.dp, MycoGreen.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MycoGreen)
                        Spacer(Modifier.width(6.dp))
                        Text("Detectar mi ubicación actual (GPS)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendEmergencySos(sosMessageText)
                        showSosDialog = false
                        sosMessageText = ""
                        Toast.makeText(context, "🚨 Alerta SOS emitida a toda la red", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MycoError)
                ) {
                    Text("EMITIR SOS AHORA", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) {
                    Text("Cancelar", color = MycoSubtle)
                }
            },
            containerColor = MycoSurface
        )
    }

    Scaffold(
        containerColor = MycoBg,
        bottomBar = {
            MycoBottomBar(
                selectedTab = selectedTab,
                totalUnreadCount = totalUnreadCount,
                onSelectTab = { tab ->
                    selectedTab = tab
                    when (tab) {
                        1 -> onNavigateToContacts()
                        2 -> onNavigateToNetwork()
                        3 -> onNavigateToSettings()
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToContacts,
                containerColor = MycoGreen,
                contentColor = MycoBg,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo chat")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Myco",
                        color = MycoGreen,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Botón SOS de Pánico
                    Surface(
                        color = MycoError.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showSosDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🚨", fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "SOS",
                                color = MycoError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Botón permanente de Radar de Nodos Cercanos
                    Surface(
                        color = if (nearbyNodes.isNotEmpty()) MycoGreenDark.copy(alpha = 0.35f) else MycoSurface2,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showNearbyRadarDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (nearbyNodes.isNotEmpty()) MycoGreen else MycoSubtle)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (nearbyNodes.isNotEmpty()) "${nearbyNodes.size} cercano${if (nearbyNodes.size != 1) "s" else ""}" else "📡 Radar",
                                color = if (nearbyNodes.isNotEmpty()) MycoGreen else MycoOnSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Banner rápido de Radar de Nodos Cercanos
            Surface(
                color = MycoSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { showNearbyRadarDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📡", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Radar de Nodos Cercanos", color = MycoOnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (nearbyNodes.isNotEmpty()) "${nearbyNodes.size} dispositivo${if (nearbyNodes.size != 1) "s" else ""} listo${if (nearbyNodes.size != 1) "s" else ""} para conectar" else "Buscar personas con Myco a mi alrededor",
                                color = if (nearbyNodes.isNotEmpty()) MycoGreen else MycoSubtle,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text("Explorar ➜", color = MycoGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Buscar chat o ID…", color = MycoSubtle, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MycoSubtle) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda", tint = MycoSubtle)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MycoGreen,
                    unfocusedBorderColor = MycoSurface3,
                    focusedContainerColor = MycoSurface,
                    unfocusedContainerColor = MycoSurface,
                    focusedTextColor = MycoOnSurface,
                    unfocusedTextColor = MycoOnSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            HorizontalDivider(color = MycoSurface3, thickness = 0.5.dp)

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍄", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "Sin resultados para \"$searchQuery\""
                            else "Sin conversaciones aún",
                            color = MycoSubtle,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Agrega un contacto para empezar a chatear", color = MycoSurface3, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn {
                    items(conversations, key = { it.contact.userId }) { item ->
                        ConversationRow(
                            item = item,
                            onClick = { onOpenChat(item.contact.userId) },
                            onLongClick = { conversationToDelete = item }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    item: ConversationItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isSos = item.contact.userId == Constants.BROADCAST_SOS_ID || item.lastMessage?.isSos == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(if (isSos) MycoError.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isSos) MycoError.copy(alpha = 0.25f) else MycoGreenDark),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isSos) "🚨" else item.contact.userId.take(2).uppercase(),
                color = if (isSos) MycoError else MycoGreen,
                fontSize = if (isSos) 20.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.contact.displayName,
                    color = if (isSos) MycoError else MycoOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                item.lastMessage?.let { msg ->
                    Text(
                        text = msg.timestamp.toFormattedTime(),
                        color = MycoSubtle,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = item.lastMessage?.content ?: "Sin mensajes",
                color = when {
                    isSos -> MycoError.copy(alpha = 0.9f)
                    item.unreadCount > 0 -> MycoOnSurface
                    else -> MycoSubtle
                },
                fontWeight = if (item.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (item.unreadCount > 0) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 20.dp, minHeight = 20.dp)
                        .clip(CircleShape)
                        .background(MycoGreen)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                        color = MycoBg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 82.dp),
        color = MycoSurface3,
        thickness = 0.5.dp
    )
}

@Composable
private fun MycoBottomBar(
    selectedTab: Int,
    totalUnreadCount: Int = 0,
    onSelectTab: (Int) -> Unit
) {
    NavigationBar(containerColor = MycoSurface) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onSelectTab(0) },
            icon = {
                if (totalUnreadCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MycoGreen,
                                contentColor = MycoBg
                            ) {
                                Text(if (totalUnreadCount > 99) "99+" else totalUnreadCount.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    ) {
                        Icon(Icons.Default.ChatBubble, null)
                    }
                } else {
                    Icon(Icons.Default.ChatBubble, null)
                }
            },
            label = { Text("Chats") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MycoGreen,
                selectedTextColor = MycoGreen,
                indicatorColor = MycoGreenDark.copy(alpha = 0.3f),
                unselectedIconColor = MycoSubtle,
                unselectedTextColor = MycoSubtle
            )
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onSelectTab(1) },
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text("Contactos") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MycoGreen,
                selectedTextColor = MycoGreen,
                indicatorColor = MycoGreenDark.copy(alpha = 0.3f),
                unselectedIconColor = MycoSubtle,
                unselectedTextColor = MycoSubtle
            )
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onSelectTab(2) },
            icon = { Icon(Icons.Default.Hub, null) },
            label = { Text("Red") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MycoGreen,
                selectedTextColor = MycoGreen,
                indicatorColor = MycoGreenDark.copy(alpha = 0.3f),
                unselectedIconColor = MycoSubtle,
                unselectedTextColor = MycoSubtle
            )
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onSelectTab(3) },
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Ajustes") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MycoGreen,
                selectedTextColor = MycoGreen,
                indicatorColor = MycoGreenDark.copy(alpha = 0.3f),
                unselectedIconColor = MycoSubtle,
                unselectedTextColor = MycoSubtle
            )
        )
    }
}
