package com.batallagroup.myco.presentation.qr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.batallagroup.myco.presentation.contacts.ContactsViewModel
import com.batallagroup.myco.presentation.theme.*

/**
 * Pantalla de escaneo QR.
 * En producción integrar CameraX + ML Kit Barcode Scanning.
 * Esta versión muestra la UI base con campo de entrada manual como alternativa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var manualInput by remember { mutableStateOf("") }
    val addResult by viewModel.addResult.collectAsState(initial = null)

    LaunchedEffect(addResult) {
        if (addResult?.startsWith("Contacto agregado") == true) {
            onNavigateBack()
        }
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
                title = { Text("Escanear QR", color = MycoOnSurface) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("📷", fontSize = 64.sp)

            Spacer(Modifier.height(16.dp))

            Text(
                "Apunta la cámara al código QR\ndel contacto que quieres agregar",
                color = MycoSubtle,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            HorizontalDivider(color = MycoSurface3)

            Spacer(Modifier.height(16.dp))

            Text("O ingresa el código manualmente:", color = MycoSubtle, fontSize = 13.sp)

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = manualInput,
                onValueChange = { manualInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("myco://userId:encKey:signKey", color = MycoSubtle, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MycoOnSurface,
                    unfocusedTextColor = MycoOnSurface,
                    focusedBorderColor = MycoGreen,
                    unfocusedBorderColor = MycoSurface3
                ),
                maxLines = 3
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.addContactFromQr(manualInput) },
                enabled = manualInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MycoGreen)
            ) {
                Text("Agregar contacto")
            }

            addResult?.let { result ->
                Spacer(Modifier.height(12.dp))
                Text(
                    result,
                    color = if (result.startsWith("Error") || result == "QR inválido") MycoError else MycoSuccess,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
