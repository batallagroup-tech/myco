package com.batallagroup.myco.presentation.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batallagroup.myco.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onShowQr: () -> Unit
) {
    val relayEnabled by viewModel.relayEnabled.collectAsStateWithLifecycle()
    val userAlias by viewModel.userAlias.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var editAliasDialog by remember { mutableStateOf(false) }
    var backupDialog by remember { mutableStateOf(false) }
    var restoreDialog by remember { mutableStateOf(false) }
    var currentAliasInput by remember { mutableStateOf(userAlias) }

    // Diálogo para editar alias
    if (editAliasDialog) {
        AlertDialog(
            onDismissRequest = { editAliasDialog = false },
            title = { Text("Nombre visible", color = MycoGreen, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = currentAliasInput,
                    onValueChange = { currentAliasInput = it },
                    label = { Text("Tu alias o nombre", color = MycoSubtle) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MycoGreen,
                        unfocusedBorderColor = MycoSurface3,
                        focusedTextColor = MycoOnSurface,
                        unfocusedTextColor = MycoOnSurface
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUserAlias(currentAliasInput)
                        editAliasDialog = false
                        Toast.makeText(context, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MycoGreen)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { editAliasDialog = false }) {
                    Text("Cancelar", color = MycoSubtle)
                }
            },
            containerColor = MycoSurface
        )
    }

    // Diálogo de exportar respaldo de claves
    if (backupDialog) {
        val backupText = remember { viewModel.exportIdentityBackup() }
        AlertDialog(
            onDismissRequest = { backupDialog = false },
            title = { Text("Copia de seguridad de identidad", color = MycoGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Guarda este código seguro en un lugar protegido. Te permite restaurar tu ID y tus mensajes si cambias de celular.",
                        color = MycoOnSurface,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MycoSurface2,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = backupText,
                            color = MycoGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp),
                            maxLines = 6
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(backupText))
                        Toast.makeText(context, "Copia de seguridad copiada", Toast.LENGTH_SHORT).show()
                        backupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MycoGreen)
                ) {
                    Text("Copiar claves")
                }
            },
            dismissButton = {
                TextButton(onClick = { backupDialog = false }) {
                    Text("Cerrar", color = MycoSubtle)
                }
            },
            containerColor = MycoSurface
        )
    }

    // Diálogo de restaurar identidad
    if (restoreDialog) {
        var restoreJsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { restoreDialog = false },
            title = { Text("Restaurar identidad", color = MycoGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Pega el texto de tu copia de seguridad para restaurar tus claves:", color = MycoOnSurface, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        placeholder = { Text("{\"user_id\": ...}", color = MycoSubtle, fontSize = 12.sp) },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MycoGreen,
                            unfocusedBorderColor = MycoSurface3,
                            focusedTextColor = MycoOnSurface,
                            unfocusedTextColor = MycoOnSurface
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.restoreIdentityBackup(restoreJsonInput.trim())
                        if (success) {
                            Toast.makeText(context, "Identidad restaurada con éxito", Toast.LENGTH_LONG).show()
                            restoreDialog = false
                        } else {
                            Toast.makeText(context, "Error: Datos de respaldo inválidos", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MycoGreen)
                ) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreDialog = false }) {
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
                title = { Text("Ajustes", color = MycoOnSurface) }
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
            // Mi identidad
            SettingsSection("Mi identidad") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MycoSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Nombre / Alias visible", color = MycoSubtle, fontSize = 12.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    userAlias.ifEmpty { "Sin nombre configurado" },
                                    color = if (userAlias.isNotEmpty()) MycoOnSurface else MycoSubtle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            TextButton(onClick = {
                                currentAliasInput = userAlias
                                editAliasDialog = true
                            }) {
                                Text("Editar", color = MycoGreen)
                            }
                        }

                        HorizontalDivider(color = MycoSurface3, modifier = Modifier.padding(vertical = 12.dp))

                        Text("ID en la red Mesh", color = MycoSubtle, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                viewModel.myUserId,
                                color = MycoGreen,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Row {
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(viewModel.myUserId))
                                    Toast.makeText(context, "ID copiado", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, null, tint = MycoSubtle, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = onShowQr) {
                                    Icon(Icons.Default.QrCode, null, tint = MycoGreen, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Botón de Invitar y Compartir ID
                        Button(
                            onClick = {
                                val shareText = """
                                    🔒 Conéctate conmigo en Myco de forma privada y sin servidores.
                                    Mi ID de contacto: ${viewModel.myUserId}
                                    Nombre: ${userAlias.ifEmpty { "Usuario Myco" }}

                                    Descarga Myco en Google Play:
                                    https://play.google.com/store/apps/details?id=com.batallagroup.myco
                                """.trimIndent()
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Mi ID de Myco")
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Compartir mi ID de Myco"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MycoGreenDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = MycoGreen)
                            Spacer(Modifier.width(8.dp))
                            Text("Invitar y Compartir mi ID", color = MycoGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = MycoSurface3, modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { backupDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MycoGreen)
                            ) {
                                Icon(Icons.Default.Key, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Respaldar", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { restoreDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MycoGreen)
                            ) {
                                Icon(Icons.Default.Restore, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Restaurar", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Configuración de la red
            SettingsSection("Red y Retransmisión") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MycoSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Nodo repetidor (Relay)", color = MycoOnSurface, fontSize = 14.sp)
                                Text(
                                    "Ayuda a retrasmitir paquetes cifrados de otros nodos cercanos",
                                    color = MycoSubtle,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            Switch(
                                checked = relayEnabled,
                                onCheckedChange = viewModel::setRelayEnabled,
                                colors = SwitchDefaults.colors(checkedThumbColor = MycoBg, checkedTrackColor = MycoGreen)
                            )
                        }

                        HorizontalDivider(color = MycoSurface3, modifier = Modifier.padding(vertical = 12.dp))

                        TextButton(
                            onClick = {
                                viewModel.clearTransitMessages()
                                Toast.makeText(context, "Mensajes en tránsito eliminados", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MycoError)
                        ) {
                            Text("Limpiar mensajes en tránsito")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Difusión / Compartir
            SettingsSection("Difusión") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MycoSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "🍄 ¡Únete a Myco! La red de mensajería cifrada y descentralizada que funciona sin internet y a larga distancia. Mi ID de usuario es: ${viewModel.myUserId}\n\nDescárgala en Google Play: https://play.google.com/store/apps/details?id=com.batallagroup.myco"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Compartir Myco"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MycoGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Compartir Myco con amigos", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Información
            SettingsSection("Acerca de") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MycoSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow("Versión", com.batallagroup.myco.BuildConfig.VERSION_NAME)
                        InfoRow("Protocolo", "Myco v1.0 (Mesh + Nostr Relays)")
                        InfoRow("Cifrado", "ECDH P-256 + AES-256-GCM")
                        InfoRow("Firma", "ECDSA SHA-256")
                        InfoRow("Desarrollado por", "Batalla Group")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(title.uppercase(), color = MycoSubtle, fontSize = 11.sp, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
    content()
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MycoSubtle, fontSize = 14.sp)
        Text(value, color = MycoOnSurface, fontSize = 14.sp)
    }
}
