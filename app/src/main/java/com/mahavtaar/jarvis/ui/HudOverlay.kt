package com.mahavtaar.jarvis.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun HudOverlay() {
    val infiniteTransition = rememberInfiniteTransition()

    // Scanline animation moving top to bottom
    val scanlineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val cornerSize = 40.dp.toPx()
        val strokeWidth = 2.dp.toPx()

        // Top Left Corner
        val topLeftPath = Path().apply {
            moveTo(0f, cornerSize)
            lineTo(0f, 0f)
            lineTo(cornerSize, 0f)
        }
        drawPath(topLeftPath, color, style = Stroke(width = strokeWidth))

        // Top Right Corner
        val topRightPath = Path().apply {
            moveTo(width - cornerSize, 0f)
            lineTo(width, 0f)
            lineTo(width, cornerSize)
        }
        drawPath(topRightPath, color, style = Stroke(width = strokeWidth))

        // Bottom Left Corner
        val bottomLeftPath = Path().apply {
            moveTo(0f, height - cornerSize)
            lineTo(0f, height)
            lineTo(cornerSize, height)
        }
        drawPath(bottomLeftPath, color, style = Stroke(width = strokeWidth))

        // Bottom Right Corner
        val bottomRightPath = Path().apply {
            moveTo(width - cornerSize, height)
            lineTo(width, height)
            lineTo(width, height - cornerSize)
        }
        drawPath(bottomRightPath, color, style = Stroke(width = strokeWidth))

        // Target Reticle marks on edges
        val edgeLength = 10.dp.toPx()
        drawLine(color, Offset(width / 2, 0f), Offset(width / 2, edgeLength), strokeWidth)
        drawLine(color, Offset(width / 2, height - edgeLength), Offset(width / 2, height), strokeWidth)
        drawLine(color, Offset(0f, height / 2), Offset(edgeLength, height / 2), strokeWidth)
        drawLine(color, Offset(width - edgeLength, height / 2), Offset(width, height / 2), strokeWidth)

        // Scanline
        val currentScanY = height * scanlineY
        drawLine(
            color = color.copy(alpha = 0.3f),
            start = Offset(0f, currentScanY),
            end = Offset(width, currentScanY),
            strokeWidth = 4.dp.toPx()
        )
    }
}
