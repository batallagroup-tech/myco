package com.batallagroup.myco.presentation.identity

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batallagroup.myco.presentation.theme.*

@Composable
fun IdentityScreen(
    viewModel: IdentityViewModel = hiltViewModel(),
    onContinue: () -> Unit
) {
    val identity by viewModel.identity.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()

    val rotation = rememberInfiniteTransition(label = "spin")
    val angle by rotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MycoBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Generando identidad",
                color = MycoGreen,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Tus claves criptográficas son únicas y nunca salen de este dispositivo",
                color = MycoSubtle,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(40.dp))

            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp).rotate(angle),
                    color = MycoGreen,
                    strokeWidth = 3.dp
                )
            } else {
                identity?.let { id ->
                    // Tarjeta de identidad
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MycoGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MycoSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Tu ID en la red",
                                color = MycoSubtle,
                                fontSize = 12.sp
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = id.userId,
                                color = MycoGreen,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 3.sp
                            )

                            Spacer(Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                KeyIndicator("Cifrado", "ECDH P-256")
                                KeyIndicator("Firma", "ECDSA SHA-256")
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Text(
                        text = "✓ Claves generadas y almacenadas de forma segura",
                        color = MycoSuccess,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            if (!isGenerating && identity != null) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MycoGreen)
                ) {
                    Text(
                        "Entrar a Myco",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyIndicator(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MycoSubtle, fontSize = 11.sp)
        Text(value, color = MycoOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
