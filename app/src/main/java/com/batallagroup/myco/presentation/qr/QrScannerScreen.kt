package com.batallagroup.myco.presentation.qr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.batallagroup.myco.presentation.contacts.ContactsViewModel
import com.batallagroup.myco.presentation.theme.*
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var manualInput by remember { mutableStateOf("") }
    val addResult by viewModel.addResult.collectAsState(initial = null)

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            viewModel.addContactFromQr(result.contents)
        }
    }

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
            Text("🍄", fontSize = 56.sp)

            Spacer(Modifier.height(12.dp))

            Text(
                "Escanear código QR de contacto",
                color = MycoGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Apunta la cámara al código QR\ndel contacto que deseas agregar",
                color = MycoSubtle,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val options = ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt("Apunta al código QR de Myco")
                        setCameraId(0)
                        setBeepEnabled(true)
                        setBarcodeImageEnabled(false)
                        setOrientationLocked(false)
                    }
                    scanLauncher.launch(options)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MycoGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Abrir Cámara / Escáner", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(28.dp))

            HorizontalDivider(color = MycoSurface3)

            Spacer(Modifier.height(20.dp))

            Text("O ingresa el código del QR manualmente:", color = MycoSubtle, fontSize = 13.sp)

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

            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = { viewModel.addContactFromQr(manualInput) },
                enabled = manualInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MycoGreen)
            ) {
                Text("Procesar texto QR")
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
