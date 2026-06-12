package com.batallagroup.myco.presentation.network

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batallagroup.myco.domain.model.MycoNode
import com.batallagroup.myco.presentation.theme.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val nodes by viewModel.nearbyNodes.collectAsStateWithLifecycle()
    val transitCount by viewModel.transitCount.collectAsStateWithLifecycle()

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
                title = { Text("Red Myco", color = MycoOnSurface) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Stats cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "Nodos cercanos",
                    value = nodes.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "En tránsito",
                    value = transitCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text("Topología de la red", color = MycoSubtle, fontSize = 13.sp)

            Spacer(Modifier.height(12.dp))

            // Grafo de la red
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                colors = CardDefaults.cardColors(containerColor = MycoSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                NetworkGraph(
                    nodes = nodes,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(24.dp))

            if (nodes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sin nodos detectados. Activa Bluetooth y acércate a otros usuarios de Myco.",
                        color = MycoSubtle,
                        fontSize = 13.sp
                    )
                }
            } else {
                Text("Nodos detectados", color = MycoSubtle, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                nodes.forEach { node ->
                    NodeRow(node = node)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MycoSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = MycoGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(label, color = MycoSubtle, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NetworkGraph(nodes: List<MycoNode>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = minOf(cx, cy) * 0.6f

        val myNodeColor = MycoGreen
        val nodeColor = MycoGreenDark
        val lineColor = MycoGreenDark.copy(alpha = 0.4f)

        // Nodo central (yo)
        drawCircle(color = myNodeColor.copy(alpha = 0.2f), radius = 24f, center = Offset(cx, cy))
        drawCircle(color = myNodeColor, radius = 12f, center = Offset(cx, cy))

        // Nodos externos
        nodes.forEachIndexed { i, _ ->
            val angle = (2 * Math.PI * i / nodes.size) - Math.PI / 2
            val x = cx + radius * cos(angle).toFloat()
            val y = cy + radius * sin(angle).toFloat()
            val pos = Offset(x, y)

            drawLine(color = lineColor, start = Offset(cx, cy), end = pos, strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawCircle(color = nodeColor, radius = 10f, center = pos)
            drawCircle(color = myNodeColor.copy(alpha = 0.7f), radius = 6f, center = pos)
        }
    }
}

@Composable
private fun NodeRow(node: MycoNode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(MycoGreen, RoundedCornerShape(5.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(node.nodeId, color = MycoOnSurface, fontSize = 13.sp)
        }
        Text("${node.rssi} dBm", color = MycoSubtle, fontSize = 12.sp)
    }
}
