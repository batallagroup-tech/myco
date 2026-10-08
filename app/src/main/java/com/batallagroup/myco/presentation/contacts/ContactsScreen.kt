package com.batallagroup.myco.presentation.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batallagroup.myco.domain.model.Contact
import com.batallagroup.myco.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onScanQr: () -> Unit,
    onShowQr: () -> Unit
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val addResult by viewModel.addResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(addResult) {
        addResult?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    if (showAddDialog) {
        var inputId by remember { mutableStateOf("") }
        var inputAlias by remember { mutableStateOf("") }
        var inputPhone by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Agregar contacto", color = MycoGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = inputId,
                        onValueChange = { inputId = it.trim().lowercase() },
                        label = { Text("ID del contacto (8 caracteres)", color = MycoSubtle) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MycoGreen,
                            unfocusedBorderColor = MycoSurface3,
                            focusedTextColor = MycoOnSurface,
                            unfocusedTextColor = MycoOnSurface
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputAlias,
                        onValueChange = { inputAlias = it },
                        label = { Text("Alias / Nombre (opcional)", color = MycoSubtle) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MycoGreen,
                            unfocusedBorderColor = MycoSurface3,
                            focusedTextColor = MycoOnSurface,
                            unfocusedTextColor = MycoOnSurface
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputPhone,
                        onValueChange = { inputPhone = it.trim() },
                        label = { Text("Teléfono celular (para SMS sin internet)", color = MycoSubtle) },
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
                        if (inputId.isNotBlank()) {
                            viewModel.addContactManual(inputId, inputAlias, inputPhone)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MycoGreen)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = MycoSubtle)
                }
            },
            containerColor = MycoSurface
        )
    }

    Scaffold(
        containerColor = MycoBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MycoSurface),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MycoGreen)
                    }
                },
                title = { Text("Contactos", color = MycoOnSurface) },
                actions = {
                    IconButton(onClick = onShowQr) {
                        Icon(Icons.Default.QrCode, contentDescription = "Mi QR", tint = MycoGreen)
                    }
                    IconButton(onClick = onScanQr) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear QR", tint = MycoGreen)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MycoGreen,
                contentColor = MycoBg,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Person, contentDescription = "Agregar contacto")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MycoSubtle,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Sin contactos", color = MycoSubtle, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Agrega un contacto con su ID o escaneando su código QR",
                            color = MycoSurface3,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MycoGreen)
                            ) {
                                Text("Ingresar ID")
                            }
                            OutlinedButton(
                                onClick = onScanQr,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MycoGreen)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Escanear QR")
                            }
                        }
                    }
                }
            } else {
                LazyColumn {
                    items(contacts) { contact ->
                        ContactRow(
                            contact = contact,
                            onClick = { onOpenChat(contact.userId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MycoGreenDark),
            contentAlignment = Alignment.Center
        ) {
            Text(
                contact.userId.take(2).uppercase(),
                color = MycoGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(Modifier.width(14.dp))

        Column {
            Text(
                contact.displayName,
                color = MycoOnSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (contact.customNickname.isNotBlank() && contact.alias.isNotBlank()) "Red: ${contact.alias} • ID: ${contact.userId}" else "ID: ${contact.userId}",
                    color = MycoSubtle,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (contact.phoneNumber.isNotBlank()) {
                    Text(
                        "📱 ${contact.phoneNumber}",
                        color = MycoGreen,
                        fontSize = 11.sp
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
