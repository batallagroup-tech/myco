package com.batallagroup.myco.presentation.splash

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.batallagroup.myco.presentation.theme.MycoGreen
import com.batallagroup.myco.presentation.theme.MycoGreenDark

/** Logo de Myco: red de micelio estilizada dibujada en Canvas. */
@Composable
fun MycoLogoCanvas(
    size: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.then(Modifier.run { this })) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2
        val cy = h / 2

        // Nodos del micelio
        val nodes = listOf(
            Offset(cx, cy),                   // centro
            Offset(cx - w * 0.32f, cy - h * 0.28f), // superior izquierda
            Offset(cx + w * 0.32f, cy - h * 0.28f), // superior derecha
            Offset(cx - w * 0.38f, cy + h * 0.08f), // izquierda
            Offset(cx + w * 0.38f, cy + h * 0.08f), // derecha
            Offset(cx - w * 0.18f, cy + h * 0.36f), // inferior izquierda
            Offset(cx + w * 0.18f, cy + h * 0.36f), // inferior derecha
            Offset(cx, cy - h * 0.42f),             // arriba
            Offset(cx + w * 0.20f, cy - h * 0.08f), // interno derecha
            Offset(cx - w * 0.20f, cy + h * 0.12f), // interno izquierda
        )

        // Conexiones (ramificaciones)
        val connections = listOf(
            0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6,
            1 to 7, 2 to 7, 1 to 3, 2 to 4,
            0 to 8, 0 to 9, 8 to 2, 9 to 5
        )

        val lineColor = MycoGreenDark.copy(alpha = 0.7f)
        val nodeColor = MycoGreen
        val dimColor = Color(0xFF2E7D32).copy(alpha = 0.4f)

        // Filamentos
        connections.forEachIndexed { i, (a, b) ->
            drawLine(
                color = if (i < 6) lineColor else dimColor,
                start = nodes[a],
                end = nodes[b],
                strokeWidth = if (i < 6) 2.5f else 1.5f,
                cap = StrokeCap.Round
            )
        }

        // Nodos
        nodes.forEachIndexed { i, node ->
            val radius = when (i) {
                0 -> w * 0.06f
                in 1..2 -> w * 0.04f
                else -> w * 0.028f
            }
            drawCircle(color = MycoGreenDark, radius = radius * 1.5f, center = node)
            drawCircle(color = nodeColor, radius = radius, center = node)
        }
    }
}
