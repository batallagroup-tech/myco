package com.batallagroup.myco.presentation.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.presentation.settings.SettingsViewModel
import com.batallagroup.myco.presentation.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrDisplayScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val qrPayload = remember { viewModel.getQrPayload() }
    val qrBitmap = remember(qrPayload) { generateQrBitmap(qrPayload) }

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
                title = { Text("Mi código QR", color = MycoOnSurface) }
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
            Text(
                "Comparte este código con\notros usuarios de Myco",
                color = MycoSubtle,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(280.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    qrBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: run {
                        CircularProgressIndicator(color = MycoGreen)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            val userAlias by viewModel.userAlias.collectAsState()
            if (userAlias.isNotBlank()) {
                Text(
                    userAlias,
                    color = MycoOnSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
            }

            Text("ID en la red Mesh", color = MycoSubtle, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                viewModel.myUserId,
                color = MycoGreen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 3.sp
            )
        }
    }
}

private fun generateQrBitmap(content: String): Bitmap? {
    return try {
        val size = Constants.QR_SIZE_PX
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        null
    }
}
