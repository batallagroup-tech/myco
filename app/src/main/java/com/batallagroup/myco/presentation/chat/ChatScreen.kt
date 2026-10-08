package com.batallagroup.myco.presentation.chat

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.utils.toFormattedTime
import com.batallagroup.myco.domain.model.Message
import com.batallagroup.myco.domain.model.MessageStatus
import com.batallagroup.myco.presentation.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactId: String,
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val contact by viewModel.contact.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMessageIds.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showInfoMessage by remember { mutableStateOf<Message?>(null) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var phoneInputText by remember { mutableStateOf("") }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInputText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val isSelectionMode = selectedIds.isNotEmpty()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Diálogo de confirmación para vaciar chat
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Vaciar chat", color = MycoError, fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar todo el historial de mensajes con este contacto? Esta acción es irreversible.", color = MycoOnSurface) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChatHistory()
                        showClearConfirm = false
                        Toast.makeText(context, "Historial eliminado", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MycoError)
                ) {
                    Text("Eliminar todo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancelar", color = MycoSubtle)
                }
            },
            containerColor = MycoSurface
        )
    }

    // Diálogo de info criptográfica del mensaje
    showInfoMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { showInfoMessage = null },
            title = { Text(if (msg.isSos) "🚨 Alerta de Emergencia" else "Detalles del mensaje", color = if (msg.isSos) MycoError else MycoGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoItem("ID de Paquete", msg.id.take(16) + "...")
                    InfoItem("Vía de Transporte", when (msg.transportType) {
                        Constants.TRANSPORT_WIFI_DIRECT -> "📶 Wi-Fi Direct P2P (200 metros)"
                        Constants.TRANSPORT_BLE -> "🔵 Bluetooth Low Energy Mesh"
                        Constants.TRANSPORT_SMS -> "✉️ Mensaje de Respaldo SMS"
                        else -> "🌐 Relay Global Descentralizado"
                    })
                    InfoItem("Cifrado", if (msg.isSos) "🔓 Emisión SOS Pública de Rescate" else "ECDH P-256 + AES-256-GCM (E2EE)")
                    InfoItem("Firma Digital", "ECDSA SHA-256")
                    InfoItem("Hora", msg.timestamp.toFormattedTime())
                    InfoItem("Dirección", if (msg.isOutgoing) "Saliente (Tú)" else "Entrante")
                    InfoItem("Estado", when (msg.status) {
                        MessageStatus.SENDING -> "Enviando / En cola"
                        MessageStatus.IN_TRANSIT -> "En tránsito en la red Mesh"
                        MessageStatus.DELIVERED -> "Entregado con éxito"
                        MessageStatus.EXPIRED -> "Expirado (>24h)"
                        MessageStatus.FAILED -> "Fallo en entrega"
                    })
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoMessage = null }) {
                    Text("Cerrar", color = MycoGreen)
                }
            },
            containerColor = MycoSurface
        )
    }

    // Diálogo para asignar número celular de SMS Fallback
    if (showPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            title = { Text("Teléfono de Respaldo SMS", color = MycoGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Asigna el número celular de este contacto para enviar mensajes cifrados por la red celular cuando no haya internet ni repetidores cerca.",
                        color = MycoOnSurface,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = phoneInputText,
                        onValueChange = { phoneInputText = it.trim() },
                        placeholder = { Text("Ej. +521234567890", color = MycoSubtle) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MycoGreen,
                            unfocusedBorderColor = MycoSurface3,
                            focusedTextColor = MycoOnSurface,
                            unfocusedTextColor = MycoOnSurface
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (phoneInputText.isNotBlank()) {
                            viewModel.updateContactPhone(phoneInputText)
                            showPhoneDialog = false
                            Toast.makeText(context, "Teléfono guardado", Toast.LENGTH_SHORT).show()
                            if (inputText.isNotBlank()) {
                                val intent = viewModel.createSmsFallbackIntent(inputText)
                                if (intent != null) {
                                    context.startActivity(intent)
                                    inputText = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MycoGreen)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneDialog = false }) {
                    Text("Cancelar", color = MycoSubtle)
                }
            },
            containerColor = MycoSurface
        )
    }

    // Diálogo para editar nombre personalizado del contacto
    if (showEditNameDialog) {
        val networkAlias = contact?.alias?.ifEmpty { "Nodo ${contactId.take(4).uppercase()}" } ?: "Nodo"
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Editar nombre del contacto", color = MycoGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Asigna un nombre personalizado para este contacto. Este nombre se mantendrá en tu lista aunque el usuario cambie su alias en la red.",
                        color = MycoOnSurface,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = editNameInputText,
                        onValueChange = { editNameInputText = it },
                        placeholder = { Text("Ej. Mi amigo Carlos", color = MycoSubtle) },
                        label = { Text("Nombre personalizado", color = MycoSubtle) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MycoGreen,
                            unfocusedBorderColor = MycoSurface3,
                            focusedTextColor = MycoOnSurface,
                            unfocusedTextColor = MycoOnSurface
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Nombre emitido en la red: $networkAlias",
                        color = MycoSubtle,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCustomNickname(editNameInputText)
                        showEditNameDialog = false
                        Toast.makeText(context, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MycoGreen)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!contact?.customNickname.isNullOrBlank()) {
                        TextButton(onClick = {
                            viewModel.updateCustomNickname("")
                            showEditNameDialog = false
                            Toast.makeText(context, "Restablecido a nombre de red", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Restablecer", color = MycoError)
                        }
                    }
                    TextButton(onClick = { showEditNameDialog = false }) {
                        Text("Cancelar", color = MycoSubtle)
                    }
                }
            },
            containerColor = MycoSurface
        )
    }

    Scaffold(
        containerColor = MycoBg,
        topBar = {
            if (isSelectionMode) {
                // TopBar contextual de selección múltiple
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MycoSurface2),
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar selección", tint = MycoOnSurface)
                        }
                    },
                    title = {
                        Text(
                            "${selectedIds.size} seleccionado${if (selectedIds.size > 1) "s" else ""}",
                            color = MycoOnSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                        if (selectedIds.size == 1) {
                            IconButton(onClick = {
                                showInfoMessage = messages.find { it.id == selectedIds.first() }
                            }) {
                                Icon(Icons.Default.Info, contentDescription = "Info del mensaje", tint = MycoGreen)
                            }
                        }
                        IconButton(onClick = {
                            val text = viewModel.getSelectedMessagesText()
                            if (text.isNotBlank()) {
                                clipboard.setText(AnnotatedString(text))
                                Toast.makeText(context, "Mensaje copiado", Toast.LENGTH_SHORT).show()
                                viewModel.clearSelection()
                            }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar texto", tint = MycoGreen)
                        }
                        IconButton(onClick = {
                            viewModel.deleteSelectedMessages()
                            Toast.makeText(context, "Mensajes eliminados", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar mensajes", tint = MycoError)
                        }
                    }
                )
            } else {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MycoSurface),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MycoGreen)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MycoGreenDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    contact?.userId?.take(2)?.uppercase() ?: "??",
                                    color = MycoGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    contact?.displayName ?: contactId,
                                    color = MycoOnSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (!contact?.customNickname.isNullOrBlank() && !contact?.alias.isNullOrBlank()) {
                                        "Red: ${contact?.alias} • ID: ${contactId.take(8)}"
                                    } else if (!contact?.phoneNumber.isNullOrBlank()) {
                                        "📱 ${contact?.phoneNumber} • ID: ${contactId.take(8)}"
                                    } else {
                                        "ID: ${contactId.take(8)}"
                                    },
                                    color = MycoSubtle,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            phoneInputText = contact?.phoneNumber ?: ""
                            showPhoneDialog = true
                        }) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Configurar teléfono SMS",
                                tint = if (!contact?.phoneNumber.isNullOrBlank()) MycoGreen else MycoSubtle
                            )
                        }
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = MycoGreen)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MycoSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar nombre", color = MycoOnSurface) },
                                onClick = {
                                    showMenu = false
                                    editNameInputText = contact?.customNickname ?: ""
                                    showEditNameDialog = true
                                },
                                leadingIcon = {
                                    Text("✏️", fontSize = 16.sp)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Teléfono de respaldo", color = MycoOnSurface) },
                                onClick = {
                                    showMenu = false
                                    phoneInputText = contact?.phoneNumber ?: ""
                                    showPhoneDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = MycoGreen)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Vaciar chat", color = MycoError) },
                                onClick = {
                                    showMenu = false
                                    showClearConfirm = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MycoError)
                                }
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            Surface(
                color = MycoSurface,
                shadowElevation = 8.dp,
                modifier = Modifier.imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Mensaje cifrado…", color = MycoSubtle) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MycoSurface2,
                            unfocusedContainerColor = MycoSurface2,
                            focusedTextColor = MycoOnSurface,
                            unfocusedTextColor = MycoOnSurface,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    Spacer(Modifier.width(6.dp))
                    if (inputText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val phone = contact?.phoneNumber
                                if (phone.isNullOrBlank()) {
                                    phoneInputText = ""
                                    showPhoneDialog = true
                                } else {
                                    val intent = viewModel.createSmsFallbackIntent(inputText)
                                    if (intent != null) {
                                        context.startActivity(intent)
                                        inputText = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MycoSurface3)
                        ) {
                            Icon(
                                Icons.Default.Sms,
                                contentDescription = "Enviar por SMS Cifrado",
                                tint = MycoGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                                scope.launch {
                                    if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        enabled = inputText.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) MycoGreen else MycoSurface3)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = if (inputText.isNotBlank()) MycoBg else MycoSubtle
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍄", fontSize = 42.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Comienza a chatear", color = MycoSubtle, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Tus mensajes están protegidos de extremo a extremo", color = MycoSurface3, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val isSelected = selectedIds.contains(message.id)
                    MessageBubble(
                        message = message,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.toggleSelectMessage(message.id)
                            }
                        },
                        onLongClick = {
                            viewModel.toggleSelectMessage(message.id)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isOutgoing = message.isOutgoing
    val isSos = message.isSos

    val transportIcon = when (message.transportType) {
        Constants.TRANSPORT_WIFI_DIRECT -> "📶"
        Constants.TRANSPORT_BLE -> "🔵"
        Constants.TRANSPORT_SMS -> "✉️"
        else -> "🌐"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = when {
                isSelected -> MycoGreenDark.copy(alpha = 0.5f)
                isSos -> MycoError.copy(alpha = 0.25f)
                isOutgoing -> MycoBubbleOut
                else -> MycoBubbleIn
            },
            shape = RoundedCornerShape(
                topStart = if (isOutgoing) 18.dp else 4.dp,
                topEnd = if (isOutgoing) 4.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            modifier = Modifier
                .widthIn(max = 290.dp)
                .then(
                    when {
                        isSelected -> Modifier.border(1.5.dp, MycoGreen, RoundedCornerShape(18.dp))
                        isSos -> Modifier.border(1.5.dp, MycoError, RoundedCornerShape(18.dp))
                        else -> Modifier
                    }
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                if (isSos) {
                    Text(
                        text = "🚨 ALERTA SOS EMITIDA",
                        color = MycoError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = message.content,
                    color = MycoOnSurface,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${message.timestamp.toFormattedTime()}  $transportIcon",
                        color = MycoSubtle,
                        fontSize = 10.sp
                    )
                    if (isOutgoing) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = when (message.status) {
                                MessageStatus.SENDING -> "⏳"
                                MessageStatus.IN_TRANSIT -> "🔄"
                                MessageStatus.DELIVERED -> "✅"
                                MessageStatus.EXPIRED -> "⚠️"
                                MessageStatus.FAILED -> "❌"
                            },
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(label, color = MycoSubtle, fontSize = 11.sp)
        Text(value, color = MycoOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
