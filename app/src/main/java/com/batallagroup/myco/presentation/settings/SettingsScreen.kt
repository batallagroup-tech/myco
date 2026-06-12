package com.batallagroup.myco.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
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
    val clipboard = LocalClipboardManager.current

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
                        Text("Tu ID en la red", color = MycoSubtle, fontSize = 12.sp)
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
                                }) {
                                    Icon(Icons.Default.ContentCopy, null, tint = MycoSubtle, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = onShowQr) {
                                    Icon(Icons.Default.QrCode, null, tint = MycoGreen, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Configuración de la red
            SettingsSection("Red") {
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
                                Text("Retransmitir mensajes ajenos", color = MycoOnSurface, fontSize = 14.sp)
                                Text(
                                    "Ayuda a otros a enviar mensajes a través de tu nodo",
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
                            onClick = viewModel::clearTransitMessages,
                            colors = ButtonDefaults.textButtonColors(contentColor = MycoError)
                        ) {
                            Text("Limpiar mensajes en tránsito")
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
                        InfoRow("Versión", "1.0.0")
                        InfoRow("Protocolo", "Myco v1.0")
                        InfoRow("Desarrollado por", "Batalla Group")
                        InfoRow("Saltos máximos", "15")
                        InfoRow("TTL mensajes", "24 horas")
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
