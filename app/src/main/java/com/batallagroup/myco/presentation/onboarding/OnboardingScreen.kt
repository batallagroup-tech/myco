package com.batallagroup.myco.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.batallagroup.myco.presentation.theme.*

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val description: String,
    val emoji: String
)

private val pages = listOf(
    OnboardingPage(
        title = "Bienvenido a Myco",
        subtitle = "La red que vive entre los teléfonos",
        description = "Myco es una red de mensajería descentralizada. Tus mensajes viajan de teléfono en teléfono como el micelio de los hongos, sin necesitar internet ni cobertura móvil.",
        emoji = "🍄"
    ),
    OnboardingPage(
        title = "Sin internet",
        subtitle = "Sin servidores. Sin rastreo.",
        description = "Myco usa Bluetooth LE para crear una red mesh local. Los mensajes saltan hasta 15 nodos para llegar a su destino. No hay servidores centrales que puedan ser hackeados o censurados.",
        emoji = "📡"
    ),
    OnboardingPage(
        title = "Tu identidad es tuya",
        subtitle = "Criptografía de extremo a extremo",
        description = "Myco genera un par de claves criptográficas únicas en tu dispositivo. Solo tú puedes leer tus mensajes. Ni siquiera los nodos que los retransmiten pueden verlos.",
        emoji = "🔐"
    ),
    OnboardingPage(
        title = "Aviso legal",
        subtitle = "Léelo antes de continuar",
        description = "",
        emoji = "⚠️"
    )
)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onFinish: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    var accepted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MycoBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Emoji / icono de página
            Text(
                text = pages[currentPage].emoji,
                fontSize = 64.sp
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = pages[currentPage].title,
                color = MycoGreen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = pages[currentPage].subtitle,
                color = MycoOnSurface,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            if (currentPage < 3) {
                Text(
                    text = pages[currentPage].description,
                    color = MycoSubtle,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else {
                // Pantalla de aviso legal
                LegalNoticeCard(
                    accepted = accepted,
                    onAcceptChanged = { accepted = it }
                )
            }

            Spacer(Modifier.weight(1f))

            // Indicadores de página
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                pages.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == currentPage) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (i == currentPage) MycoGreen else MycoSurface3)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (currentPage < 3) {
                        currentPage++
                    } else if (accepted) {
                        viewModel.completeOnboarding()
                        onFinish()
                    }
                },
                enabled = currentPage < 3 || accepted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MycoGreen)
            ) {
                Text(
                    text = if (currentPage < 3) "Continuar" else "Entiendo y acepto",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LegalNoticeCard(
    accepted: Boolean,
    onAcceptChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MycoSurface2),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val items = listOf(
                "Myco retransmitirá mensajes cifrados de otros usuarios por hasta 24 horas",
                "Los mensajes no entregados se eliminan automáticamente",
                "Myco funciona actualmente solo en Android",
                "Diseñado para uso en emergencias y zonas sin cobertura"
            )
            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = MycoGreen, modifier = Modifier.padding(end = 8.dp, top = 2.dp))
                    Text(item, color = MycoOnSurface, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = accepted,
                    onCheckedChange = onAcceptChanged,
                    colors = CheckboxDefaults.colors(checkedColor = MycoGreen)
                )
                Text(
                    text = "He leído y acepto las condiciones",
                    color = MycoOnSurface,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
